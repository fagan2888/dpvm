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

	public static void main( String[] args ) throws TwisterException, IOException {
		if ( args.length != 3 ) {
			System.out.println( "Usage:[partition File][num maps][num reducers]" );
			System.exit( -1 );
		}

		String partitionFile = args[ 0 ];
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
		properties.put( TwisterLogger.JOB_LOGGER_FOLDER_PROP, "/work/twister/job-log/" + jobConf.getJobId() );
		jobConf.setProperties( properties );

		logger = new TwisterLogger().fromDriverConfig( jobConf );
		logger.info( "created the twister logger from driver" );

		jobConf.setMapperClass( TwisterSolverMapTask.class );
		jobConf.setReducerClass( TwisterSolverReduceTask.class );
		jobConf.setCombinerClass( TwisterSolverCombiner.class );

		jobConf.setNumMapTasks( mapperCount );
		jobConf.setNumReduceTasks( reducerCount );

		TwisterDriver driver = new TwisterDriver( jobConf );

		driver.configureMaps( "/work/twister-data", "solver" );
		//driver.configureMaps( partitionFile );

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

		while ( true ) {

			// run the engine
			driver.runMapReduceBCast( objective ).monitorTillCompletion();

			// get the results
			solutions = ( (TwisterSolverCombiner) driver.getCurrentCombiner() ).getSolutions();

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

