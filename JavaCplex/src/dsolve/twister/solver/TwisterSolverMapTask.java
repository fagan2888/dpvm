package dsolve.twister.solver;

import cgl.imr.base.*;
import cgl.imr.base.impl.JobConf;
import cgl.imr.base.impl.MapperConf;
import cgl.imr.data.file.FileData;
import cgl.imr.types.StringKey;
import dsolve.LocalSolver;
import dsolve.NamedCoordList;
import dsolve.SolverHelper;
import dsolve.twister.util.TwisterLogger;
import ilog.concert.IloException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.SocketException;
import java.util.Date;

/**
 * Twister mapper class that takes a cplex model file (equations block)
 * It loads the equations file into a LocalSolver object and keeps it cached through multiple iterations
 */

public class TwisterSolverMapTask implements MapTask {

	private int mapCallCount = 0;
	Logger logger = null;

	private String modelFileName;

	private LocalSolver cplexLocalSolver = null;
	private StringKey outKey = null;
	private NamedCoordList emptySolution = new NamedCoordList();

	@Override
	public void close() throws TwisterException {
		logger.info( "close() from mapper" );
	}

	@Override
	public void configure( JobConf jobConf, MapperConf mapperConf ) throws TwisterException {
		try {
			logger = new TwisterLogger().fromMapperConfig( jobConf, mapperConf );
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		try {
			String appsDir = jobConf.getProperty( TwisterSolverDriver.TWSITER_APPS_DIR_CONFIG );
			SolverHelper.setLogger( logger );
			SolverHelper.dropNativeCplex( appsDir );
		} catch ( Throwable e ) {
			logger.error( "could not load native cplex", e );
			cplexLocalSolver = null;
			return;
		}

		// set the key used by this mapper to send back it's solutions
		outKey = new StringKey( getMapperIdentity() );
		logger.info( "configure() from mapper taskid: " + outKey.getString() );

		// load equations block into local solver
		modelFileName = ( (FileData) mapperConf.getDataPartition() ).getFileName();
		try {
			logger.info( "starting to load equations block from file: " + modelFileName );
			cplexLocalSolver = new LocalSolver();
			cplexLocalSolver.loadModelFromFile( modelFileName );
		} catch ( IloException e ) {
			e.printStackTrace();
		}

		logger.info( "finished loadeding the block" );
	}

	private String getMapperIdentity() {
		try {
			return  TwisterLogger.getHostIp() + "--" + (new Date().getTime()/1000);
		} catch ( SocketException e ) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void map( MapOutputCollector mapOutputCollector, Key key, Value value ) throws TwisterException {

		NamedCoordList objective, solution;
		boolean systemSolved = false;

		logger.info( "map() call no: " + mapCallCount );
		mapCallCount++;

		logger.info( "key: " + ( ( StringKey ) key ).getString() + " val: " + value.toString() );

		if ( cplexLocalSolver == null ) {
			logger.error( "we don't have a local solver loaded; returning null solution" );
			mapOutputCollector.collect( outKey, emptySolution );
			return;
		}

		// get current objective to optimize
		objective = new NamedCoordList();
		try {
			objective.fromBytes( value.getBytes() );
		} catch ( SerializationException e ) {
			logger.error( "could not transform objective from received bytes", e );
			mapOutputCollector.collect( outKey, emptySolution );
			return;
		}

		if ( objective.size() <= 0 ) {
			logger.error( "the objective size <= 0: " + objective.size(), new IllegalArgumentException() );
			mapOutputCollector.collect( outKey, emptySolution );
			return;
		}

		// run local solver iteration
		try {
			cplexLocalSolver.setTargetObjectivePoint( objective );

			systemSolved = cplexLocalSolver.runSolver();

			solution = cplexLocalSolver.getSolution();

			if ( systemSolved ) {
				logger.info( "blocked solved with sol: " + solution );
				mapOutputCollector.collect( outKey, solution );
			} else {
				logger.error( "local solver could not solve block" );
				mapOutputCollector.collect( outKey, emptySolution );
			}
		} catch ( IloException e ) {
			logger.error( "local solver was unable to solve model", e );
			mapOutputCollector.collect( outKey, emptySolution );
		}
	}
}
