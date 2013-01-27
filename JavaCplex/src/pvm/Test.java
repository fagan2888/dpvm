package pvm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/24/12
 * Time: 9:32 PM
 */
public class Test {

    public static void main ( String [] args ) {
	    List<PvmTrainParameters> params = new ArrayList<PvmTrainParameters>( 3 );

	    PvmTrainParameters p = new PvmTrainParameters(); p.accuracy = 97;params.add( p );
	    p = new PvmTrainParameters(); p.accuracy = 98;params.add( p );
	    p = new PvmTrainParameters(); p.accuracy = 99;params.add( p );
	    Collections.sort( params, p );

	    for ( PvmTrainParameters x : params ) {
		    System.out.println( x.accuracy );
	    }
    }
}
