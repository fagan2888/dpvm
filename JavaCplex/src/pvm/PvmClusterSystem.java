package pvm;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.IloRange;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/27/13
 * Time: 4:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class PvmClusterSystem extends PvmSystem{
    PvmClusterDataCore core;

    protected boolean CreateVariables() throws IloException {
        vars = new IloNumVar[core.clustersCount * 2];
        for (int i = 0; i < vars.length; i++)
            vars[i] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, IloNumVarType.Float);

        return true;
    }

    @Override
    protected boolean AddSigmaRegularConstraints(){

        return true;
    }

    public boolean buildSingleLPSystemWithBias(PvmClusterDataCore pvmClusterDataCore, double bias) throws IloException{
        super.cleanCplex();
        super.addCplexSolver(false);

        core = pvmClusterDataCore;
        baseCount = core.entries.size();

        rngConstraints = new IloRange[core.clustersCount * 2 + 1];
        CreateVariables();
        AddSigmaRegularConstraints();

        /*
        rngConstraints = new IloRange[baseCount * 2 + 1];

        if (!CreateVariables(false, false, Double.MAX_VALUE))
            return false;

        if (!AddSigmaRegularConstraints())
            return false;

        setDenominatorUnityEqualityConstraint(baseCount * 2);
        setSingleLPTypeObjectiveWithBias(positiveBias);

        return true;
        * */

        return false;
    }
}
