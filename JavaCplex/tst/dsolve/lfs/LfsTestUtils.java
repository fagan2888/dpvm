package dsolve.lfs;

import dsolve.GlobalModelBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/19/12
 * Time: 2:30 PM
 */

public class LfsTestUtils {
	public static  double getRandomDouble( Random random, Boolean positive ) {
		double val = random.nextInt()%100 + random.nextDouble();

		if ( positive == null )     { return  val; }
		if ( positive && val < 0 )  { return -val; }
		if ( !positive && val > 0 ) { return -val; }

		return val;
	}

	/**
	 * Generates a random set of numbers in range [0, maxValue)
	 * @param maxValue
	 * @param valueCount
	 * @return
	 */
	public static List<Integer> generateRandomSortedVars( int maxValue, int valueCount ) {
		Set<Integer> set = new HashSet<Integer>( valueCount );
		Random random = new Random();

		for ( int i=0; i<valueCount; i++ ) {
			while ( true ) {
				int rand = random.nextInt( maxValue );
				if ( !set.contains( rand ) ) {
					set.add( rand );
					break;
				}
			}
		}
		List<Integer> arrayList = new ArrayList<Integer>( set );
		Collections.sort( arrayList );

		return arrayList;
	}

	/**
	 * Method to generate a random input feasibility system that will contain 0,0,..,0 as a feasible point
	 * @param eqCount
	 * @param maxVarCount
	 * @return the temporary filePath where the system is located
	 * @throws java.io.IOException
	 */

	public static String generateRandomInput( int eqCount, int maxVarCount, Integer varCount ) throws IOException {

		File temp = File.createTempFile( "temp-input-system-", ".tmp" );
		PrintWriter out = new PrintWriter( new FileWriter( temp.getAbsolutePath() ) );
		Random random = new Random();

		for ( int i=0; i<eqCount; i++ ) {
			if ( varCount == null ) {
				varCount = maxVarCount/3 + random.nextInt( (2*maxVarCount)/3 );
			}

			List<Integer> randomVars = generateRandomSortedVars( maxVarCount, varCount );

			double lowerBound = getRandomDouble( random, false );
			double upperBound = getRandomDouble( random, true );

			String line = "";
			for ( Integer varIndex : randomVars ) {
				double val = getRandomDouble( random, null );
				line += String.format( "%d:%.5f,", varIndex, val );
			}

			line = String.format( "%.5f|%.5f|%s", lowerBound, upperBound, line.substring( 0, line.length()-1 ) );
			out.println( line );
		}
		out.close();
		return temp.getAbsolutePath();
	}

	/**
	 * @param dimCount
	 * @param defaultValue if this is null, random will be used
	 * @return the path to the file where the objective point was generated
	 * @throws IOException
	 */

	public static String generateObjectivePoint( int dimCount, Double defaultValue ) throws IOException {

		File temp = File.createTempFile( "temp-objective-point-", ".tmp" );
		PrintWriter out = new PrintWriter( new FileWriter( temp.getAbsolutePath() ) );
		Random random = new Random();

		for ( int i=0; i<dimCount; i++ ) {
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
}
