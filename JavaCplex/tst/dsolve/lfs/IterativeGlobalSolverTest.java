package dsolve.lfs;

import dsolve.IterativeGlobalSolver;
import dsolve.SolverHelper;
import ilog.concert.IloException;
import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/19/12
 * Time: 3:22 PM
 */

public class IterativeGlobalSolverTest {

	@Test
	public void testGlobalSolverWithRandomFeasibleSystem () throws IOException, IloException, URISyntaxException {

		int records = 1000;
		int dimensions = 10;
		int blocks = 2;

		SolverHelper.dropNativeCplex( "." );

		List<String> blockModelFiles = LfsTestUtils.generateRandomGlobalSystem( records, dimensions, blocks );

		String objectiveFile = SolverHelper.generateObjectivePoint( dimensions, null );
		IterativeGlobalSolver globalSolver = new IterativeGlobalSolver( blockModelFiles, objectiveFile );

		Assert.assertTrue( "Localsolver didn't manage to solve the most simple system ever", globalSolver.runSolver( 200 ) != null );
	}

	public static void main( String[] args ) throws IOException, URISyntaxException, IloException {
		int records = 100;
		int dimensions = 10;
		int blocks = 2;

		SolverHelper.dropNativeCplex( "." );

		List<String> blockModelFiles = LfsTestUtils.generateRandomGlobalSystem( records, dimensions, blocks );
		for ( String block : blockModelFiles ) {
			System.out.println( "block file: " + block );
		}
	}

}
