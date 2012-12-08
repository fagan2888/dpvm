package dsolve;

import ilog.concert.IloException;
import org.apache.commons.lang3.Validate;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/18/12
 * Time: 1:23 PM
 */

public class SolverHelper {

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

	/**
	 * @param dimCount
	 * @param defaultValue if this is null, random will be used
	 * @return the path to the file where the objective point was generated
	 * @throws java.io.IOException
	 */

	public static String generateObjectivePoint( int dimCount, Double defaultValue ) throws IOException {

		File temp = File.createTempFile( "temp-objective-point-", ".tmp" );
		PrintWriter out = new PrintWriter( new FileWriter( temp.getAbsolutePath() ) );
		Random random = new Random();

		for ( int i = 0; i < dimCount; i++ ) {
			double val;
			if ( defaultValue != null ) {
				val = defaultValue;
			} else {
				val = getRandomDouble( random, null );
			}

			String line = String.format( "%s|%.5f",
					GlobalModelBuilder.getVarNameByIndex( i ),
					val
			);
			out.println( line );
		}
		out.close();
		return temp.getAbsolutePath();
	}

	public static double getRandomDouble( Random random, Boolean positive ) {
		double val = random.nextInt() % 100 + random.nextDouble();

		if ( positive == null ) {
			return val;
		}
		if ( positive && val < 0 ) {
			return -val;
		}
		if ( !positive && val > 0 ) {
			return -val;
		}

		return val;
	}

	public static NamedCoordList readObjectiveFromFile( String fileName ) throws IOException, IloException {

		NamedCoordList objective = new NamedCoordList();

		FileInputStream fstream;
		DataInputStream in;
		BufferedReader br;
		String strLine;

		fstream = new FileInputStream( fileName );
		in = new DataInputStream( fstream );
		br = new BufferedReader( new InputStreamReader( in ) );

		while ( ( strLine = br.readLine() ) != null ) {

			String[] pipeSplit = strLine.split( "\\|" );
			Validate.isTrue( pipeSplit.length == 2, "Illegal format for line: " + strLine );

			objective.add( new NamedCoord( pipeSplit[ 0 ], Double.parseDouble( pipeSplit[ 1 ] ) ) );
		}
		Validate.isTrue( objective.size() > 0, "The objective point needs to have at least one coordinate" );

		return objective.rebuild();
	}

	private static void copyStream( InputStream input, OutputStream output ) throws IOException {
		byte[] buffer = new byte[ 1024 ]; // Adjust if you want
		int bytesRead;
		while ( ( bytesRead = input.read( buffer ) ) != -1 ) {
			output.write( buffer, 0, bytesRead );
		}
	}

	public static void dropNativeCplex( String libName ) throws IOException {
		// generate temporary directory as library path
		String tempdir = System.getProperty( "java.io.tmpdir" ) + new Date().getTime();
		String libraryInJar = "CPLEXLib" + File.separator + libName;

		InputStream inputStream = ClassLoader.getSystemResourceAsStream( libraryInJar );
		if ( inputStream == null ) {
			System.out.println( "cplex native library not in this jar; trying to load from disk" );
			inputStream = new FileInputStream( libraryInJar );
			return;
		}

		File libraryPath = new File( tempdir );
		libraryPath.mkdirs();

		OutputStream outputStream =  new FileOutputStream( libraryPath + File.separator + libName );
		copyStream( inputStream, outputStream );

		System.setProperty( "java.library.path", libraryPath.getAbsolutePath() );
	}
}
