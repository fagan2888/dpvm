package dsolve.twister.solver;

import cgl.imr.base.*;
import cgl.imr.base.impl.JobConf;
import cgl.imr.types.StringValue;
import dsolve.twister.util.TwisterLogger;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class TwisterCombiner implements Combiner {

    private int combineCallCount = 0;
    private String outputString = "";
    Logger logger = null;

    @Override
    public void combine(Map<Key, Value> keyValueMap) throws TwisterException {
        logger.info("combine() call no: " + combineCallCount);
        combineCallCount ++;

        for ( Map.Entry<Key, Value> entry : keyValueMap.entrySet() ) {
            StringValue val = null;
            try {
                val = new StringValue();
                val.fromBytes( entry.getValue().getBytes() );
            } catch (SerializationException e) {
                e.printStackTrace();
            }
            outputString += val.toString();
        }
    }

    @Override
    public void configure(JobConf jobConf) throws TwisterException {
        try {
            logger = new TwisterLogger().fromCombinerConfig(jobConf);
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info( "configure() from combiner" );
    }

    public String getResults() {
        return outputString;
    }

}
