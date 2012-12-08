package dsolve;

import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.apache.commons.lang3.Validate;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 11/3/12
 * Time: 2:56 PM
 */

public class LocalSolver {

    IloCplex cplexSolver = null;
    IloNumVar [] cplexVars = null;
    NamedCoordList targetObjective = null;
    NamedCoordList solution = null;

    public class LocalSolverInputException extends Throwable{
        public String reason;

        LocalSolverInputException(String reason){this.reason = reason;}
    }

    public LocalSolver() throws IloException {
        cplexSolver = new IloCplex();
	    cplexSolver.clearModel();
        cplexSolver.setOut(null);
    }

    public void loadModelFromFile( String fileName ) throws IloException {

        cplexSolver.clearModel();
        cplexSolver.importModel( fileName );

        updateCplexVarsFromModel();
    }

    private void updateCplexVarsFromModel() throws IloException {
        cplexVars = ((IloLPMatrix) cplexSolver.LPMatrixIterator().next()).getNumVars();
        List<IloNumVar> ourVars = new ArrayList<IloNumVar>();

        for ( IloNumVar var: cplexVars ) {
            if ( var.getName().startsWith("v") ) {
                ourVars.add( var );
            }
        }
        cplexVars = ourVars.toArray( new IloNumVar[ 0 ] );
    }

    private void setObjective( IloLQNumExpr qobj, boolean minimize ) throws IloException {
	    IloObjective newObjective;

	    if ( minimize ) {
		    newObjective = cplexSolver.minimize( qobj );
	    } else {
		    newObjective = cplexSolver.maximize( qobj );
	    }

	    cplexSolver.remove( cplexSolver.getObjective() );
	    cplexSolver.add( newObjective );
    }

    private void setMinDistance2OriginObjective() throws IloException {

	    IloLQNumExpr objExpr = cplexSolver.lqNumExpr();

	    for ( IloNumVar var : cplexVars ) {
		    objExpr.addTerm( 1.0, var, var ); // xi ^ 2
		    objExpr.addTerm( -2.0 * targetObjective.get( var.getName() ).val, var ); // -2oi * xi
	    }

        setObjective( objExpr, true );
    }

	public void setObjectiveFromFile( String fileName ) throws IOException, IloException {
		NamedCoordList objective = SolverHelper.readObjectiveFromFile( fileName );
		setTargetObjectivePoint( objective );
	}

    public void setTargetObjectivePoint( NamedCoordList objective ) throws IloException {

        this.targetObjective = objective;
	    setMinDistance2OriginObjective();
    }

    public boolean runSolver() throws IloException {

	    if ( !cplexSolver.solve() ) {
		    return false;
	    }

	    double[] x = cplexSolver.getValues( cplexVars );
	    Validate.isTrue( x.length == cplexVars.length, "Cplex error, we must get the same number of vars back" );

	    solution = new NamedCoordList();

	    for ( int i = 0; i < cplexVars.length; i++ ) {
		    solution.add( new NamedCoord( cplexVars[ i ].getName(), x[ i ] ) );
	    }

	    solution.rebuild();

	    return true;
    }

	public NamedCoordList getSolution() {
		return solution;
	}

	public NamedCoordList getTargetObjective() {
		return targetObjective;
	}

	public void terminateSolver() throws IloException {
		cplexSolver.clearModel();
		cplexSolver.endModel();
		cplexSolver.end();
	}

    public void exportModel( String exportedModelFile ) throws IloException {
        cplexSolver.exportModel( exportedModelFile );
    }

    public void reInit() throws IloException {
        updateCplexVarsFromModel();
    }

    public void removeConstraint(String constraintName) throws IloException {
        Iterator it = cplexSolver.rangeIterator();
        IloRange range;

        while (it.hasNext())
        {
            range = (IloRange)it.next();

            if (range.getName() == constraintName)
                cplexSolver.end(range);
        }

    }

    public void replaceConstraint(String constraintName, String constraintNameNew, double lowerBound, double upperBound, NamedCoordList expr) throws IloException, LocalSolverInputException {

        Iterator it = cplexSolver.rangeIterator();
        IloRange range;
        IloNumVar cVar;
        int i;

        while (it.hasNext())
        {
            range = (IloRange)it.next();

            if (range.getName().compareTo(constraintName) == 0)
                cplexSolver.remove(range);
        }

        IloLinearNumExpr numExpr = cplexSolver.linearNumExpr();

        // add terms to build the body of the equation
        for ( i = 0; i < expr.size(); i++ ) {
            cVar = getNumVarByName(expr.get(i).name);

            if (cVar == null)
                throw new LocalSolverInputException("Undefined variable in local solver");

            numExpr.addTerm( expr.get(i).val, cVar);
        }

        // add the actual equation to the cplex object
        cplexSolver.addRange( lowerBound, numExpr, upperBound, constraintNameNew);
    }

    private IloNumVar getNumVarByName(String varName)
    {
        int i;

        for (i = 0; i < cplexVars.length; i++)
            if (cplexVars[i].getName().compareTo(varName) == 0)
                return cplexVars[i];

        return null;
    }
}
