package dsolve.lfs;

import dsolve.GlobalModelBuilder;
import dsolve.SolverHelper;
import dsolve.IterativeGlobalSolver;
import ilog.concert.IloException;
import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/19/12
 * Time: 3:22 PM
 */

public class IterativeGlobalSolverTest {

	@Test
	public void testGlobalSolverWithRandomFeasibleSystem () throws IOException, IloException {

		int records = 1000;
		int dimensions = 10;
		int blocks = 2;

		GlobalModelBuilder modelBuilder = new GlobalModelBuilder();
		String generatedFileName = LfsTestUtils.generateRandomInput( records, dimensions, null );

		modelBuilder.readModelFromFile( generatedFileName );
		List<String> blockModelFiles = modelBuilder.splitIntoBlockFiles( records/blocks, true );

		String objectiveFile = SolverHelper.generateObjectivePoint( dimensions, null );

		IterativeGlobalSolver globalSolver = new IterativeGlobalSolver( blockModelFiles, objectiveFile );

		Assert.assertTrue( "Localsolver didn't manage to solve the most simple system ever", globalSolver.runSolver( 200 ) != null );
	}

}
