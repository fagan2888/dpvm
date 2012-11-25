package dsolve;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/18/12
 * Time: 1:23 PM
 */

public class GlobalSolver {

	public static NamedCoordList adjustCurrentObjective( NamedCoordList currentObj, List<NamedCoordList> solutions ) {
		NamedCoordList newObj = new NamedCoordList( currentObj.size() );

		// compute coordinate level mean on every solution point
		for ( int i = 0; i < currentObj.size(); i++ ) {
			NamedCoord coord = new NamedCoord( currentObj.get( i ).name, 0.0 );

			double meanContrib = 0;
			for ( NamedCoordList sol : solutions ) {
				if ( sol.has( coord.name ) ) {
					coord.val += sol.get( coord.name ).val;
					meanContrib++;
				}
			}
			if ( meanContrib > 0 ) {
				coord.val /= meanContrib;
			}

			newObj.add( coord );
		}

		return newObj.rebuild();
	}


	public static double euclidianDist( NamedCoordList v1, NamedCoordList v2 ) {
		double distance = 0;
		v1.rebuild();
		v2.rebuild();

		int i = 0, j = 0;
		while ( i < v1.size() && j < v2.size() ) {
			if ( v1.get( i ).name.compareTo( v2.get( j ).name ) < 0 ) {
				i++;
				continue;
			}
			if ( v1.get( i ).name.compareTo( v2.get( j ).name ) > 0 ) {
				j++;
				continue;
			}

			distance += Math.pow( v1.get( i ).val - v2.get( j ).val, 2 );

			i++;
			j++;
		}

		return distance;
	}

	public static double computeDistance( NamedCoordList objective, List<NamedCoordList> solutions ) {
		double distance = 0;
		for ( NamedCoordList sol : solutions ) {
			distance += euclidianDist( objective, sol );
		}
		return distance;
	}
}
