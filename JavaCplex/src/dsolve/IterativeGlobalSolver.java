package dsolve;

import ilog.concert.IloException;
import org.apache.log4j.Logger;
import util.SolverLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/25/12
 * Time: 7:45 PM
 */

public class IterativeGlobalSolver {
	private List<String> modelFileList;
	private String objectiveFile;
	private List<NamedCoordList> solutions = null;
	private List<LocalSolver> localSolvers = null;
	private NamedCoordList objective = null;


	public static Logger logger = SolverLogger.getLogger( GlobalSolver.class.getName() );

	public IterativeGlobalSolver( List<String> modelFiles, String objectiveFile ) {
		this.modelFileList = modelFiles;
		this.objectiveFile = objectiveFile;
	}

	private NamedCoordList readCurrentObjective() throws IloException, IOException {
		return LocalSolver.readObjectiveFromFile( objectiveFile );
	}

	public NamedCoordList runSolver( int maxIterCount ) throws IloException, IOException {
		return runSolver( maxIterCount, 1e-8 );
	}

	private void initCurrentSolvers() throws IOException, IloException {
		solutions = new ArrayList<NamedCoordList>( modelFileList.size() );
		localSolvers = new ArrayList<LocalSolver>( modelFileList.size() );
		objective = readCurrentObjective();

		logger.info( "Loading model files into local solvers" );
		for ( String model : modelFileList ) {
			logger.info( "loading model file: " + model );

			LocalSolver solver = new LocalSolver();
			solver.loadModelFromFile( model );

			localSolvers.add( solver );
		}
	}

	private void reInitCurrentSolvers() throws IloException {
		int i;

		for ( LocalSolver lSolver : localSolvers )
			lSolver.reInit();
	}

	private boolean iterateSolver( int maxIterCount, double feasibilityThreshold ) throws IloException {

		double distance2Objective = Double.MAX_VALUE;
		boolean systemIsStillFeasible = true;
		int iterCount = 0;

		while ( ( iterCount < maxIterCount ) && ( distance2Objective >= feasibilityThreshold ) ) {

			if ( !systemIsStillFeasible ) {
				logger.warn( "global system in infeasible; aborting" );
				return false;
			}

			solutions.clear();

			for ( LocalSolver solver : localSolvers ) {

				solver.setTargetObjectivePoint( objective );
				systemIsStillFeasible = solver.runSolver();

				if ( !systemIsStillFeasible ) {
					break;
				}

				solutions.add( solver.getSolution() );
			}

			objective = GlobalSolver.adjustCurrentObjective( objective, solutions );
			distance2Objective = GlobalSolver.computeDistance( objective, solutions );

			iterCount++;
		}

		logger.info( String.format( "Infeasibility bound: %f", distance2Objective ) );

		if ( !systemIsStillFeasible || distance2Objective >= feasibilityThreshold )
			return false;

		return true;
	}

	public NamedCoordList runSolver( int maxIterCount, double feasibilityThreshold ) throws IloException, IOException {
		initCurrentSolvers();

		if ( iterateSolver( maxIterCount, feasibilityThreshold ) )
			return objective;

		return null;
	}

	public NamedCoordList reRunSolver( int maxIterCount ) throws IloException, IOException {
		return reRunSolver( maxIterCount, 1e-8 );
	}

	public NamedCoordList reRunSolver( int maxIterCount, double feasibilityThreshold ) throws IloException, IOException {
		//reInitCurrentSolvers();

		if ( iterateSolver( maxIterCount, feasibilityThreshold ) )
			return objective;

		return null;
	}


	public void replaceConstraint( String constraintName, String constraintNameNew, double lowerBound, double upperBound, NamedCoordList expr ) throws IloException, LocalSolver.LocalSolverInputException {
		for ( LocalSolver localSolver : localSolvers )
			localSolver.replaceConstraint( constraintName, constraintNameNew, lowerBound, upperBound, expr );
	}

}
