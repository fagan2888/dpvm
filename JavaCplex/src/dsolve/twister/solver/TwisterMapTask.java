package dsolve.twister.solver;

import cgl.imr.base.*;
import cgl.imr.base.impl.JobConf;
import cgl.imr.base.impl.MapperConf;
import cgl.imr.data.file.FileData;
import cgl.imr.types.DoubleVectorData;
import cgl.imr.types.IntValue;
import cgl.imr.types.StringKey;
import dsolve.LocalSolver;
import dsolve.NamedCoordList;
import dsolve.twister.util.TwisterLogger;
import ilog.concert.IloException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.SocketException;

/**
 * Twister mapper class that takes a cplex model file (equations block)
 * It loads the equations file into a LocalSolver object and keeps it cached through multiple iterations
 */

public class TwisterMapTask implements MapTask {

	private int mapCallCount = 0;
	private int taskId = 0;
	Logger logger = null;

	private String modelFileName;

	private LocalSolver cplexLocalSolver = null;
	private NamedCoordList objective = new NamedCoordList();

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
		taskId = mapperConf.getMapTaskNo();
		logger.info( "configure() from mapper" );

		// load equations block into local solver
		modelFileName = ( (FileData) mapperConf.getDataPartition() ).getFileName();
		try {
			cplexLocalSolver.loadModelFromFile( modelFileName );
		} catch ( IloException e ) {
			e.printStackTrace();
		}
		logger.info( "local solver loaded equations from block: " + modelFileName );
	}

	@Override
	public void map( MapOutputCollector mapOutputCollector, Key key, Value value ) throws TwisterException {
		logger.info( "map() call no: " + mapCallCount );
		mapCallCount++;

		logger.info( "key: " + ( ( StringKey ) key ).getString() + " val: " + ( ( IntValue ) value ).getVal() );
		String hostIP = "127.0.0.1";

		// get current objective to optimize


		try {
			hostIP = TwisterLogger.getHostIp();
		} catch ( SocketException e ) {
			e.printStackTrace();
		}

		mapOutputCollector.collect( new StringKey( hostIP ), value );
	}
}
