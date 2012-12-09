package dsolve.lfs;

import dsolve.SolverHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/25/12
 * Time: 9:29 PM
 */

public class SolverHelperTest {
	@Test
	public void testDroppingNativeLibrary() throws IOException, URISyntaxException {
		SolverHelper.dropNativeCplex();
		String []path = System.getProperty( "java.library.path" ).split( ";" );
		String dropPath = path[path.length-1];
		System.out.println( "droppath : " + dropPath );

		File[] dropPathFiles = new File( dropPath ).listFiles();
		boolean foundDll = false;
		for ( File dll : dropPathFiles ) {
			if( dll.getAbsolutePath().contains( "cplex124" ) ) {
				foundDll = true;
				System.out.println( "found dll: " + dll.getAbsolutePath() );
				break;
			}
		}
		Assert.assertTrue( "cplex dll could not be found in classpath", foundDll );
	}

	public static void main( String[] args ) throws IOException, URISyntaxException {
		SolverHelper.listJarFiles( "cplex124.dll" );
		new SolverHelperTest().testDroppingNativeLibrary();
	}
}
