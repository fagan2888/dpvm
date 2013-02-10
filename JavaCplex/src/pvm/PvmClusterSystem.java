package pvm;

import ilog.concert.*;

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
    protected boolean AddSigmaRegularConstraints() throws IloException {
        int i, idx = 0;

        for (i = 0; i < core.clustersPos.size(); i++, idx+=2)
            AddConstraintsForIdxPos(i, idx);

        for (i = 0; i < core.clustersNeg.size(); i++, idx+=2)
            AddConstraintsForIdxNeg(i, idx);
        return true;
    }

    protected void setLinearAbsoluteValueInequalityTerms(IloLinearNumExpr lin, int cluster[], boolean positiveCluster) throws IloException {
        int clusterAux[], alphaIdx;
        double kprime[];
        double cTerm;

        if (positiveCluster)
            kprime = core.kpos;
        else
            kprime = core.kneg;

        for (alphaIdx = 0; alphaIdx < core.clustersTotal.size(); alphaIdx++){
            clusterAux = core.clustersTotal.get(alphaIdx);
            cTerm = 0;

            for (int i : clusterAux)
                for (int j : cluster)
                    cTerm += core.gramMtx[i][j] - kprime[i];

            lin.addTerm(cTerm, vars[alphaIdx]);
        }
    }

    @Override
    protected boolean AddConstraintsForIdxPos(int idx, int rngIdx) throws IloException {
        int cluster[] = core.clustersPos.get(idx);
        IloLinearNumExpr lin;

        lin = cplex.linearNumExpr();
        setLinearAbsoluteValueInequalityTerms(lin, cluster, true);
        lin.addTerm(-1.0, vars[core.clustersCount + idx]);
        rngConstraints[rngIdx] = cplex.addLe(lin, 0.0);

        lin = cplex.linearNumExpr();
        setLinearAbsoluteValueInequalityTerms(lin, cluster, true);
        lin.addTerm(1.0, vars[core.clustersCount + idx]);
        rngConstraints[rngIdx + 1] = cplex.addGe(lin, 0.0);

        return true;
    }

    @Override
    protected boolean AddConstraintsForIdxNeg(int idx, int rngIdx) throws IloException {
        int cluster[] = core.clustersNeg.get(idx);
        IloLinearNumExpr lin;

        lin = cplex.linearNumExpr();
        setLinearAbsoluteValueInequalityTerms(lin, cluster, false);
        lin.addTerm(-1.0, vars[core.clustersCount + core.clustersPos.size() + idx]);
        rngConstraints[rngIdx] = cplex.addLe(lin, 0.0);

        lin = cplex.linearNumExpr();
        setLinearAbsoluteValueInequalityTerms(lin, cluster, false);
        lin.addTerm(1.0, vars[core.clustersCount + core.clustersPos.size() + idx]);
        rngConstraints[rngIdx + 1] = cplex.addGe(lin, 0.0);

        return true;
    }

    @Override
    protected void setDenominatorUnityEqualityConstraint(int rngIdx) throws IloException{
        int alphaIdx;
        int cluster[];
        double cTerm;
        IloLinearNumExpr lin = cplex.linearNumExpr();


        for (alphaIdx = 0; alphaIdx < core.clustersCount; alphaIdx++){
            cluster = core.clustersTotal.get(alphaIdx);
            cTerm = 0;
            for (int i : cluster)
                cTerm += core.kpos[i] - core.kneg[i];

            lin.addTerm(cTerm, vars[alphaIdx]);
        }
        rngConstraints[rngIdx] = cplex.addEq(lin, 1.0, "UnityEq");
    }

    @Override
    protected void setSingleLPTypeObjectiveWithBias(double positiveBias) throws IloException {
        int i, varIdx;
        IloLinearNumExpr lin = cplex.linearNumExpr();
        double posTerm = positiveBias * (core.xNeg.length - 1), negTerm = core.xPos.length - 1;
        assert(posTerm > 0 && negTerm > 0);

        varIdx = core.clustersCount;
        for (i = 0; i < core.clustersPos.size(); i++, varIdx++)
            lin.addTerm(posTerm * core.clustersPos.get(i).length, vars[varIdx]);

        varIdx = core.clustersCount + core.clustersPos.size();
        for (i = 0; i < core.clustersNeg.size(); i++, varIdx++)
            lin.addTerm(negTerm * core.clustersNeg.get(i).length, vars[varIdx]);

        obj = cplex.addMinimize(lin);
    }

    @Override
    public boolean buildSingleLPSystemWithBias(PvmDataCore pvmClusterDataCore, double bias) throws IloException{

        if (pvmClusterDataCore.getClass() == PvmDataCore.class)//should never happen here!!
            return super.buildSingleLPSystemWithBias(pvmClusterDataCore, bias);

        super.cleanCplex();
        super.addCplexSolver(false);

        core = (PvmClusterDataCore)pvmClusterDataCore;
        baseCount = core.entries.size();

        rngConstraints = new IloRange[core.clustersCount * 2 + 1];
        CreateVariables();
        AddSigmaRegularConstraints();
        setDenominatorUnityEqualityConstraint(core.clustersCount * 2);
        setSingleLPTypeObjectiveWithBias(bias);

        return true;
    }

    @Override
    public boolean solveSingleLPWithBias( double [] resT, double positiveTrainBias ) throws IloException {
        int i;

        // the actual solving of the system
        if ( !cplex.solve() ) return false;

        assert ( resT.length > 0 );

        double [] x = cplex.getValues( vars );

        for (i = 0; i < core.clustersCount; i++){
            for (int alphaIdx : core.clustersTotal.get(i))
                core.alphas[alphaIdx] = x[i];
        }

        for (i = 0; i < core.clustersPos.size(); i++)
            core.clusterSigmasPos[i] = x[core.clustersCount + i];

        for (i = 0; i < core.clustersNeg.size(); i++)
            core.clusterSigmasNeg[i] = x[core.clustersCount + core.clustersPos.size() + i];

        core.recomputeHyperplaneBias( resT, positiveTrainBias );

        return true;
    }
}
