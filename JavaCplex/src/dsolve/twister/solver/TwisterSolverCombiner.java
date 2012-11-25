package dsolve.twister.solver;

import cgl.imr.base.*;
import cgl.imr.base.impl.JobConf;
import cgl.imr.types.StringValue;
import dsolve.NamedCoordList;
import dsolve.twister.util.TwisterLogger;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TwisterSolverCombiner implements Combiner {

	private int combineCallCount = 0;
	Logger logger = null;

	List<NamedCoordList> solutions = null;

	@Override
	public void combine( Map<Key, Value> keyValueMap ) throws TwisterException {
		logger.info( "combine() call no: " + combineCallCount );
		combineCallCount++;

		for ( Map.Entry<Key, Value> entry : keyValueMap.entrySet() ) {
			try {

				NamedCoordList val = new NamedCoordList();
				val.fromBytes( entry.getValue().getBytes() );
				solutions.add( val );

			} catch ( SerializationException e ) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void configure( JobConf jobConf ) throws TwisterException {
		try {
			logger = new TwisterLogger().fromCombinerConfig( jobConf );
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		logger.info( "configure() from combiner" );
	}

	public List<NamedCoordList> getSolutions() {
		return solutions;
	}

}
