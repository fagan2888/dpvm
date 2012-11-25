package dsolve;

import ilog.concert.IloException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/18/12
 * Time: 1:23 PM
 */

public class GlobalSolver {

	private List<String> modelFileList;
	private String       objectiveFile;
    private double       distance2Objective;
    private NamedCoordList objective = null;

    public static Logger logger = Logger.getLogger( GlobalSolver.class.getName() );

	public GlobalSolver( List<String> modelFiles, String objectiveFile ) {
	 	this.modelFileList = modelFiles;
		this.objectiveFile = objectiveFile;
	}

    public double getSolutionInfeasibility(){return distance2Objective;}

    public NamedCoordList getGlobalSolution(){return objective;}

	private NamedCoordList readCurrentObjective() throws IloException, IOException {
		return LocalSolver.readObjectiveFromFile( objectiveFile );
	}

	private NamedCoordList adjustCurrentObjective( NamedCoordList currentObj, List<NamedCoordList> solutions ) {

		NamedCoordList newObj = new NamedCoordList( currentObj.size() );

		// compute coordinate level mean on every solution point
		for ( int i=0; i<currentObj.size(); i++ ) {
			NamedCoord coord = new NamedCoord( currentObj.get(i).name, 0.0 );

			double meanContrib = 0;
			for ( NamedCoordList sol : solutions ) {
				if ( sol.has( coord.name ) ) {
					coord.val += sol.get( coord.name ).val;
					meanContrib ++;
				}
			}
			if ( meanContrib > 0 ) {
				coord.val /= meanContrib;
			}

			newObj.add( coord );
		}

		return newObj.rebuild();
	}

	public boolean runSolver( int maxIterCount ) throws IloException, IOException {

		List<NamedCoordList> solutions = new ArrayList<NamedCoordList>( modelFileList.size() );
		List<LocalSolver> localSolvers = new ArrayList<LocalSolver>( modelFileList.size() );

		objective = readCurrentObjective();

        distance2Objective = Double.MAX_VALUE;

		logger.info( "Loading model files into local solvers" );
		for ( String model: modelFileList ) {
			logger.info( "loading model file: " + model );

			LocalSolver solver = new LocalSolver();
			solver.loadModelFromFile( model );

			localSolvers.add( solver );
		}

		boolean systemIsStillFeasible = true;
		int iterCount = 0;
		while ( (iterCount < maxIterCount) && (distance2Objective >= 10e-8) ) {

			if ( !systemIsStillFeasible ) {
				logger.warning( "global system in infeasible; aborting" );
				return false;
			}

			solutions.clear();

			for ( LocalSolver solver: localSolvers ) {

				solver.setTargetObjectivePoint( objective );
				systemIsStillFeasible =  solver.runSolver();

				if ( !systemIsStillFeasible ) { break; }

				solutions.add( solver.getSolution() );
			}

			distance2Objective = computeDistance( objective, solutions );
			objective = adjustCurrentObjective( objective, solutions );

			iterCount ++;
		}

		return true;
	}

	public double euclidianDist( NamedCoordList v1, NamedCoordList v2 ) {
		double distance = 0;
		v1.rebuild(); v2.rebuild();

		int i = 0, j=0;
		while( i < v1.size() && j < v2.size() ) {
			if ( v1.get(i).name.compareTo( v2.get(j).name ) < 0 ) { i++; continue; }
			if ( v1.get(i).name.compareTo( v2.get(j).name ) > 0 ) { j++; continue; }

			distance += Math.pow( v1.get(i).val - v2.get(j).val, 2 );

			i++; j++;
		}

		return distance;
	}

	private double computeDistance( NamedCoordList objective, List<NamedCoordList> solutions ) {
		double distance = 0;
		for ( NamedCoordList sol : solutions ) {
			distance += euclidianDist( objective, sol );
		}
		return distance;
	}
}
