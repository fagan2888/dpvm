package dsolve.twister.solver;


import cgl.imr.base.*;
import cgl.imr.base.impl.JobConf;
import cgl.imr.base.impl.ReducerConf;
import cgl.imr.types.IntValue;
import cgl.imr.types.StringKey;
import cgl.imr.types.StringValue;
import dsolve.twister.util.TwisterLogger;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;

public class TwisterReduceTask implements ReduceTask{

    private int reduceCallCount = 0;
    Logger logger = null;

    @Override
    public void close() throws TwisterException {
        logger.info("close() from reducer");
    }

    @Override
    public void configure(JobConf jobConf, ReducerConf reducerConf) throws TwisterException {
        try {
            logger = new TwisterLogger().fromReducerConfig(jobConf, reducerConf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info("configure() from reducer");
    }

    @Override
    public void reduce(ReduceOutputCollector reduceOutputCollector, Key key, List<Value> values) throws TwisterException {

        logger.info("reduce() call no: " + reduceCallCount);
        reduceCallCount ++;

        String outVal = "";
        for( Value val : values) {
            outVal += ((IntValue) val).getVal() + " ";
        }
        logger.info( "reducer -> key: " + ((StringKey)key).getString() + " val: " + outVal );
        reduceOutputCollector.collect(key, new StringValue(outVal));
    }
}
