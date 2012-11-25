package dsolve.twister.solver;

import cgl.imr.base.TwisterException;
import cgl.imr.base.TwisterMonitor;
import cgl.imr.base.impl.JobConf;
import dsolve.twister.util.TwisterLogger;
import org.apache.log4j.Logger;
import org.safehaus.uuid.UUIDGenerator;

import java.io.IOException;
import java.util.Hashtable;

public class TwisterDriver {

    Logger logger = null;

    public static void main(String[] args) throws TwisterException, IOException {
        if (args.length != 3) {
            System.out.println("Usage:[partition File][num maps][num reducers]");
            System.exit(-1);
        }

        String partitionFile = args[0];
        int mapperCount = Integer.parseInt( args[1] );
        int reducerCount = Integer.parseInt( args[2] );


        TwisterDriver twisterDebugDriver = new TwisterDriver();

        double beginTime = System.currentTimeMillis();
        twisterDebugDriver.runTwisterJob( partitionFile, mapperCount, reducerCount );
        double endTime = System.currentTimeMillis();

        System.out.println("------------------------------------------------------");
        System.out.println("Twister Debug " + (endTime - beginTime) / 1000 + " seconds.");
        System.out.println("------------------------------------------------------");
        System.exit(0);
    }

    private UUIDGenerator uuidGen = UUIDGenerator.getInstance();

    public void runTwisterJob( String partitionFile, int mapperCount, int reducerCount ) throws TwisterException, IOException {
        JobConf jobConf = new JobConf( "imcu-twister--"+ uuidGen.generateTimeBasedUUID() );

        // define shared properties
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put( TwisterLogger.JOB_LOGGER_FOLDER_PROP, "/work/twister/job-log/" + jobConf.getJobId() );
        jobConf.setProperties( properties );

        logger = new TwisterLogger().fromDriverConfig( jobConf );
        logger.info("created the twister logger from driver");

        jobConf.setMapperClass(TwisterMapTask.class);
        jobConf.setReducerClass( TwisterReduceTask.class );
        jobConf.setCombinerClass( TwisterCombiner.class );

        jobConf.setNumMapTasks(mapperCount);
        jobConf.setNumReduceTasks(reducerCount);

        cgl.imr.client.TwisterDriver driver = new cgl.imr.client.TwisterDriver(jobConf);
        driver.configureMaps(partitionFile);

        TwisterMonitor monitor = driver.runMapReduce();
        monitor.monitorTillCompletion();

        String output = ((TwisterCombiner ) driver.getCurrentCombiner()).getResults();
        System.out.println( "------------------------------------" );
        System.out.println( output );
        System.out.println( "------------------------------------" );

        driver.close();
    }
}

