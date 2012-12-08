package dsolve.lfs;

import com.sun.xml.internal.ws.policy.AssertionSet;
import dsolve.SolverHelper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/25/12
 * Time: 9:29 PM
 */

public class SolverHelperTest {
	@Test
	public void testDroppingNativeLibrary() throws IOException {
		SolverHelper.dropNativeCplex( "cplex124.dll" );
		Assert.assertTrue( System.getProperty( "java.library.path" ) != null );
	}
}
