package dsolve;

import ilog.concert.IloException;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import util.SolverLogger;

import java.io.*;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
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

	private static Logger logger = SolverLogger.getLogger( SolverHelper.class.getName() );

	public static void setLogger( Logger logger ) {
		SolverHelper.logger = logger;
	}

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

	private static void addDir2JavaPath( String s ) throws IOException {
		try {
			Field field = ClassLoader.class.getDeclaredField( "usr_paths" );
			field.setAccessible( true );
			String[] paths = ( String[] ) field.get( null );
			for ( String path : paths ) {
				if ( s.equals( path ) ) {
					return;
				}
			}
			String[] tmp = new String[ paths.length + 1 ];
			System.arraycopy( paths, 0, tmp, 0, paths.length );
			tmp[ paths.length ] = s;
			field.set( null, tmp );
			System.setProperty( "java.library.path", System.getProperty( "java.library.path" ) + File.pathSeparator + s );
		} catch ( IllegalAccessException e ) {
			throw new IOException( "Failed to get permissions to set library path" );
		} catch ( NoSuchFieldException e ) {
			throw new IOException( "Failed to get field handle to set library path" );
		}
	}

	private static boolean isWindows() {
		String osName = System.getProperty( "os.name" ).toLowerCase();
		return osName.contains( "win" );
	}

	private static boolean isUnix() {
		String osName = System.getProperty( "os.name" ).toLowerCase();
		return osName.contains( "nix" ) || osName.contains( "nux" ) || osName.contains( "aix" );
	}

	public static void listJarFiles( String dirPath ) throws URISyntaxException, IOException {
		logger.info( "Listing resources like: " + dirPath );
		URL url = ClassLoader.getSystemResource( dirPath );
		if ( url != null ) {
			logger.info( "file: " + url.toURI() );
		}

		String classPath = System.getProperty( "java.library.path" );
		for ( String pathDir : classPath.split( ":" ) ) {
			logger.info( "pathDir: " + pathDir );
		}
	}

	public static void dropNativeCplex( String pathFolder ) throws IOException, URISyntaxException {

		logger.info( "adding dir to java path: " + pathFolder );
		addDir2JavaPath( pathFolder );

		// generate temporary directory as library path
		String tempdir = System.getProperty( "java.io.tmpdir" ) + File.separator + new Date().getTime();

		String libName = "libcplex124.so";
		if ( isWindows() ) {
			libName = "cplex124.dll";
		}

		listJarFiles( libName );

		URL url = ClassLoader.getSystemResource( libName );
		InputStream inputStream = null;
		if ( url != null ) {
			logger.info( "found in jar as: " + url.toURI() );
			JarURLConnection conn = ( JarURLConnection ) url.openConnection();
			inputStream = conn.getInputStream();
		}

		if ( inputStream == null ) {
			logger.info( "cplex native library not in this jar; trying to load from disk" );
			File libFile = new File( pathFolder+ File.separator + libName );
			if ( !libFile.exists() ) {
				logger.error(
						"cplex native library does not exists at location: " + libFile.getAbsolutePath(),
						new FileNotFoundException( libFile.getAbsolutePath() )
				);
				return;
			}
			inputStream = new FileInputStream( libFile );
			if ( inputStream == null ) {
				logger.error( "could not read from libfile: " + libFile.getAbsolutePath() );
				return;
			}
		}

		File libraryPath = new File( tempdir );
		libraryPath.mkdirs();

		String finalLibPath = libraryPath + File.separator + libName;
		OutputStream outputStream = new FileOutputStream( finalLibPath );
		copyStream( inputStream, outputStream );
		logger.info( "library: " + finalLibPath + " dropped to disk" );

		// close file streams
		outputStream.close();

		addDir2JavaPath( libraryPath.getAbsolutePath() );
		logger.info( String.format( "library: %s added to java library path env", libraryPath ) );
	}
}
