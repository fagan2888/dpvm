package dsolve.lfs;

import dsolve.*;
import ilog.concert.IloException;
import junit.framework.Assert;
import org.junit.Test;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/17/12
 * Time: 6:22 PM
 */

public class LocalSolverTest {

	private void printSolution( NamedCoordList solution ) {
		String line = "";
		for ( NamedCoord coord : solution.getList() ) {
			line += String.format( "%5s:%.5f, ", coord.name, coord.val );
		}
		System.out.println( line );
	}

	@Test
	public void testLocalSolverWithRandomFeasibleSystem () throws IOException, IloException {

        int records = 10;
        int dimensions = 5;
		int blocks = 1;

		GlobalModelBuilder modelBuilder = new GlobalModelBuilder();
		String generatedFileName = LfsTestUtils.generateRandomInput( records, dimensions, null );

		modelBuilder.readModelFromFile( generatedFileName );
		List<String> blockModelFiles = modelBuilder.splitIntoBlockFiles( records/blocks, true );

		for ( String modelFile : blockModelFiles ) {
			// build local solver
			LocalSolver localSolver = new LocalSolver();

			localSolver.loadModelFromFile( modelFile );
			localSolver.setObjectiveFromFile( SolverHelper.generateObjectivePoint( dimensions, 0.0 ) );
            localSolver.exportModel( "local-model-exported.lp" );

			Assert.assertTrue( "Localsolver didn't manage to solve the most simple system ever", localSolver.runSolver() );
			printSolution( localSolver.getSolution() );
		}
	}
}
