package dsolve.twister.solver;

import cgl.imr.base.TwisterException;
import cgl.imr.base.impl.JobConf;
import cgl.imr.client.TwisterDriver;
import dsolve.NamedCoordList;
import dsolve.SolverHelper;
import dsolve.twister.util.TwisterLogger;
import ilog.concert.IloException;
import org.apache.log4j.Logger;
import org.safehaus.uuid.UUIDGenerator;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

public class TwisterSolverDriver {

	Logger logger = null;
	private static final String TWISTER_APPS_DIR = "/work/twister/apps";
	private static final String TWISTER_LOGS_DIR = "/work/twister/job-log/";

	public static final String TWSITER_APPS_DIR_CONFIG = "dpvm.twister.job.appsfolder";

	public static void main( String[] args ) throws TwisterException, IOException {
		if ( args.length != 3 ) {
			System.out.println( "Usage:[paritionFile] [num maps] [num reducers]" );
			System.exit( -1 );
		}

		String partitionFile = args[0];
		int mapperCount = Integer.parseInt( args[ 1 ] );
		int reducerCount = Integer.parseInt( args[ 2 ] );

		TwisterSolverDriver twisterDebugDriver = new TwisterSolverDriver();

		double beginTime = System.currentTimeMillis();
		twisterDebugDriver.runTwisterJob( partitionFile, mapperCount, reducerCount );
		double endTime = System.currentTimeMillis();

		System.out.println( "------------------------------------------------------" );
		System.out.println( "Twister Debug " + ( endTime - beginTime ) / 1000 + " seconds." );
		System.out.println( "------------------------------------------------------" );
		System.exit( 0 );
	}

	private UUIDGenerator uuidGen = UUIDGenerator.getInstance();

	public void runTwisterJob( String partitionFile, int mapperCount, int reducerCount ) throws TwisterException, IOException {
		JobConf jobConf = new JobConf( "imcu-twister--" + uuidGen.generateTimeBasedUUID() );

		// define shared properties
		Hashtable<String, String> properties = new Hashtable<String, String>();
		properties.put( TwisterLogger.JOB_LOGGER_FOLDER_PROP, TWISTER_LOGS_DIR + jobConf.getJobId() );
		properties.put( TWSITER_APPS_DIR_CONFIG, TWISTER_APPS_DIR );
		jobConf.setProperties( properties );

		logger = new TwisterLogger().fromDriverConfig( jobConf );
		logger.info( "created the twister logger from driver" );

		jobConf.setMapperClass( TwisterSolverMapTask.class );
		jobConf.setReducerClass( TwisterSolverReduceTask.class );
		jobConf.setCombinerClass( TwisterSolverCombiner.class );

		jobConf.setNumMapTasks( mapperCount );
		jobConf.setNumReduceTasks( reducerCount );

		TwisterDriver driver = new TwisterDriver( jobConf );

		//driver.configureMaps( "/work/twister-data", "global-model-block" );
		driver.configureMaps( partitionFile );

		int maxIterCount = 1000;
		int currentIter = 0;
		double threashold = 10e-8;

		List<NamedCoordList> solutions = null;
		NamedCoordList objective = null;
		double distance2Objective = Double.MAX_VALUE;

		try {
			objective = SolverHelper.readObjectiveFromFile( SolverHelper.generateObjectivePoint( 10, null ) );
		} catch ( IloException e ) {
			e.printStackTrace();
		}
		logger.info( "objective: " + objective );

		while ( true ) {

			// run the engine
			driver.runMapReduceBCast( objective ).monitorTillCompletion();
			logger.info( "iteration: " + currentIter + " finished; combining results");

			// get the results
			solutions = ( (TwisterSolverCombiner) driver.getCurrentCombiner() ).getSolutions();

			for ( NamedCoordList sol : solutions ) {
				if ( sol.isEmpty() ) {
					logger.error( "we received an empty solution; aborting everthing" );
					break;
				}
			}

			objective = SolverHelper.adjustCurrentObjective( objective, solutions );
			distance2Objective = SolverHelper.computeDistance( objective, solutions );

			logger.info( String.format( "iter: %04d - dist: %10f", currentIter, distance2Objective ) );

			if ( distance2Objective <= threashold ) {
				logger.info( "we've reached a solution" );
				logger.info( "solution: " + objective.toString() );
				break;
			}

			if ( currentIter >= maxIterCount ) {
				logger.info( "we could not reach a valid solution" );
				break;
			}
			currentIter++;
		}

		driver.close();
	}
}

