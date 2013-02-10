package pvm;

import dsolve.*;
import dsolve.lfs.LfsConstraint;
import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/25/12
 * Time: 8:57 PM
 */

public class PvmSystem {
    IloCplex cplex;//the cplex solver
    IloNumVar[] vars;//the variables
    IloRange [] rngConstraints;//the constraints returned by the solver

    /*the two constraints that express:
        sigma_+ <= t * E_+
        simga_- <= t * E_-
    */
    IloRange tConstraintPos = null, tConstraintNeg = null;
    IloObjective obj;

    PvmDataCore core;
    int baseCount;

    IterativeGlobalSolver globalSolver = null;

    public boolean BuildSystemFor(PvmDataCore pvms, double t, boolean saveSys, boolean nameAllConstraints) throws IloException {

        try {
            addCplexSolver( false );
        }
        catch (IloException e){
            return false;
        }

        core = pvms;
        baseCount = core.entries.size();

        rngConstraints = new IloRange[baseCount * 2 + 2];

        if (!CreateVariables())
            return false;

        if (!AddSigmaRegularConstraints())
            return false;

        if (!AddStrictlyPositiveAveragesConstraints(2 * baseCount))
            return false;

        if (!AddFinalTConstraints(t))
            return false;

        if (!SetEmptyObjective())
            return false;

        setVariableNames();

        if (nameAllConstraints)
            SetConstraintsNames();

        if (saveSys)
            SaveModel();

        return true;
    }

    private void setVariableNames(){
        int i;

        //todo : this is just a shortcut. GlobalModelBuilder should be modified to work with strings instead of indexes

        for (i = 0; i < vars.length; i++)
            vars[i].setName(GlobalModelBuilder.getVarNameByIndex(i));
    }

    private void SetConstraintsNames(){
        int i;

        for (i = 0; i < rngConstraints.length; i++)
            rngConstraints[i].setName("c" + String.valueOf(i));
    }

    public boolean SaveModel(){

        try
        {
            cplex.exportModel("current.lp");
            exportModelDPvmFormat("dpvm_problem.txt");
        }
        catch (ilog.concert.IloException e)
        {
            return false;
        } catch (IOException e) {
            return false;
        }


        return true;
    }

    protected boolean CreateVariables() throws IloException {
        return CreateVariables(true, false, Double.MAX_VALUE);
    }
    private boolean CreateVariables(boolean addHyperplaneOffsetVar, boolean constrainedAlphas, double constrainedAlphasLimit){
        int i;
        if (addHyperplaneOffsetVar)
            vars = new IloNumVar[baseCount * 2 + 1];
        else
            vars = new IloNumVar[baseCount * 2];

        try
        {
            if (constrainedAlphas)
            {
                if (constrainedAlphasLimit < 0)
                    constrainedAlphasLimit = -constrainedAlphasLimit;
                for (i = 0; i < baseCount; i++)
                    vars[i] = cplex.numVar(-constrainedAlphasLimit, constrainedAlphasLimit, IloNumVarType.Float);
            }
            else
                for (i = 0; i < baseCount; i++)
                    vars[i] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, IloNumVarType.Float);




            for (i = 0; i < baseCount; i++)
                vars[i + baseCount] = cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float);


            if (addHyperplaneOffsetVar)
                vars[2 * core.entries.size()] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, IloNumVarType.Float);
        }
        catch(ilog.concert.IloException e)
        {
            return false;
        }

        return true;

    }

    protected boolean AddSigmaRegularConstraints() throws IloException {
        int i, rngIdx = 0;

        for (i = 0; i < core.xPos.length; i++, rngIdx += 2)
            if (!AddConstraintsForIdxPos(core.xPos[i], rngIdx))
                return false;

        for (i = 0; i < core.xNeg.length; i++, rngIdx += 2)
            if (!AddConstraintsForIdxNeg(core.xNeg[i], rngIdx))
                return false;

        return true;

    }

    protected boolean AddConstraintsForIdxPos(int idx, int rngIdx) throws IloException {

        int i;
        IloLinearNumExpr lin = null;

        try
        {
            lin = cplex.linearNumExpr();

            for (i = 0; i < baseCount; i++)
                lin.addTerm(core.kpos[i] - core.gramMtx[idx][i], vars[i]);

            lin.addTerm( 1.0, vars[idx + baseCount] );

            rngConstraints[rngIdx] = cplex.addGe( lin, 0.0 );

            lin = cplex.linearNumExpr();
            for (i = 0; i < baseCount; i++)
                lin.addTerm(core.gramMtx[idx][i] - core.kpos[i], vars[i]);

            lin.addTerm( 1.0, vars[idx + baseCount] );

            rngConstraints[rngIdx + 1] = cplex.addGe(lin, 0.0);
        }
        catch (ilog.concert.IloException e)
        {
            return false;
        }

        return true;
    }

    protected boolean AddConstraintsForIdxNeg(int idx, int rngIdx) throws IloException {
        int i;
        IloLinearNumExpr lin = null;

        try
        {
            lin = cplex.linearNumExpr();

            for (i = 0; i < baseCount; i++)
                lin.addTerm(core.kneg[i] - core.gramMtx[idx][i], vars[i]);

            lin.addTerm((double)1.0, vars[idx + baseCount]);

            rngConstraints[rngIdx] = cplex.addGe(lin, 0.0);

            lin = cplex.linearNumExpr();
            for (i = 0; i < baseCount; i++)
                lin.addTerm(core.gramMtx[idx][i] - core.kneg[i], vars[i]);

            lin.addTerm((double)1.0, vars[idx + baseCount]);

            rngConstraints[rngIdx + 1] = cplex.addGe(lin, 0.0);
        }
        catch (ilog.concert.IloException e)
        {
            return false;
        }

        return true;
    }

    private boolean AddStrictlyPositiveAveragesConstraints(int rngIdx){
        int i;
        IloLinearNumExpr lin = null;

        try
        {
            lin = cplex.linearNumExpr();

            for (i = 0; i < baseCount; i++)
                lin.addTerm(core.kpos[i], vars[i]);

            lin.addTerm(1.0, vars[2 * baseCount]);

            rngConstraints[rngIdx] = cplex.addGe(lin, 1.0);



            lin = cplex.linearNumExpr();
            for (i = 0; i < baseCount; i++)
                lin.addTerm(-core.kneg[i], vars[i]);

            lin.addTerm(-1.0, vars[2 * baseCount]);


            rngConstraints[rngIdx + 1] = cplex.addGe(lin, 1.0);
        }
        catch (ilog.concert.IloException e)
        {
            return false;
        }

        return true;
    }

    public boolean AddFinalTConstraints(double t){
        int i, sigIdx;
        double temp;
        IloLinearNumExpr lin = null;

        assert(core.xPos.length > 1);
        assert(core.xNeg.length > 1);

        if (tConstraintNeg != null || tConstraintPos != null)
            if (!RemoveFinalTConstraints())
                return false;

        try
        {
            lin = cplex.linearNumExpr();



            temp = t * (core.xPos.length - 1);
            for (i = 0; i < baseCount; i++)
                lin.addTerm(temp * core.kpos[i], vars[i]);

            for (i = 0; i < core.xPos.length; i++)
            {
                sigIdx = baseCount + core.xPos[i];
                lin.addTerm(-1.0, vars[sigIdx]);
            }

            lin.addTerm(temp, vars[2 * baseCount]);

            tConstraintPos = cplex.addGe(lin, 0.0);
            tConstraintPos.setName("cTPositive");





            lin = cplex.linearNumExpr();
            temp = t * (core.xNeg.length - 1);
            for (i = 0; i < baseCount; i++)
                lin.addTerm(-temp * core.kneg[i], vars[i]);

            for (i = 0; i < core.xNeg.length; i++)
            {
                sigIdx = baseCount + core.xNeg[i];
                lin.addTerm(-1.0, vars[sigIdx]);
            }

            lin.addTerm(-temp, vars[2 * baseCount]);

            tConstraintNeg = cplex.addGe(lin, 0.0);
            tConstraintNeg.setName("cTNegative");
        }
        catch (ilog.concert.IloException e)
        {
            return false;
        }

        return true;
    }

    private NamedCoordList getNamedCoordListFromConstraint(IloRange constraint) throws IloException {

        IloNumVar cVar;
        IloLinearNumExprIterator numIt;
        NamedCoordList retList = new NamedCoordList();
        NamedCoord cCoord;

        numIt = ((IloLinearNumExpr)constraint.getExpr()).linearIterator();
        while (numIt.hasNext()){
            cVar = numIt.nextNumVar();

            cCoord = new NamedCoord(cVar.getName(), numIt.getValue());
            retList.add(cCoord);
        }

        return retList.rebuild();
    }

    public void replaceFinalTConstraintsDistributed(double t) throws IloException, LocalSolver.LocalSolverInputException {
        String tConstraintPosName = tConstraintPos.getName(), tConstraintNegName = tConstraintNeg.getName();

        AddFinalTConstraints(t);

        String tConstraintPosNameNew = tConstraintPos.getName(), tConstraintNegNameNew = tConstraintNeg.getName();

        globalSolver.replaceConstraint(tConstraintPosName, tConstraintPosNameNew, tConstraintPos.getLB(), tConstraintPos.getUB(), getNamedCoordListFromConstraint(tConstraintPos));
        globalSolver.replaceConstraint(tConstraintNegName, tConstraintPosNameNew, tConstraintNeg.getLB(), tConstraintNeg.getUB(), getNamedCoordListFromConstraint(tConstraintNeg));
    }

    public boolean RemoveFinalTConstraints(){

        try
        {
            if(tConstraintNeg != null)
            {
                cplex.remove(tConstraintNeg);
                tConstraintNeg = null;
            }

            if (tConstraintPos != null)
            {
                cplex.remove(tConstraintPos);
                tConstraintPos = null;
            }
        }
        catch (ilog.concert.IloException e)
        {
            return false;
        }

        return true;
    }

    public boolean SetEmptyObjective(){
        try
        {
            IloLinearNumExpr lin = cplex.linearNumExpr();

            obj = cplex.addMinimize(lin);
        }
        catch (ilog.concert.IloException e)
        {
            return false;
        }

        return true;
    }

    public boolean Solve(){

        int i, sigIdx;

        try
        {
            if (!cplex.solve())
                return false;

            double [] x = cplex.getValues(vars);

            for (i = 0; i < baseCount; i++)
                core.alphas[i] = x[i];

            for (i = 0; i < baseCount; i++)
                core.sigmas[i] = x[i + baseCount];

            core.offsetB = x[2 * baseCount];
        }
        catch (ilog.concert.IloException e)
        {
            return false;
        }

        return true;
    }

    public int getVariableIndex(IloNumVar var)
    {
        int i;

        for (i = 0; i < vars.length; i++)
            if (vars[i] == var)
                return i;

        return -1;
    }

    private LfsConstraint iloRangeToLfsConstraint(IloRange range) throws IloException {
        IloNumVar cVar;
        int varIdx;
        IloLinearNumExprIterator numIt;
        LfsConstraint constraint;
        int i;
        int[] cIdxs;
        double[] cTerms;

        List<Integer> tempIdxs = new ArrayList<Integer>();
        List<Double> tempTerms = new ArrayList<Double>();

        numIt = ((IloLinearNumExpr)range.getExpr()).linearIterator();

        while (numIt.hasNext())
        {
            cVar = numIt.nextNumVar();
            varIdx = getVariableIndex(cVar);

            if (varIdx < 0)
                return null;//careful, this could mean there are unaccounted for variables

            tempIdxs.add(varIdx);
            tempTerms.add(numIt.getValue());
        }

        constraint = new LfsConstraint(tempIdxs.size());

        constraint.setLowerBound(range.getLB());
        constraint.setUpperBound(range.getUB());

        constraint.setName(range.getName());

        cIdxs = constraint.getIndex();
        cTerms = constraint.getTerms();

        for (i = 0; i < constraint.getSize(); i++)
        {
            cIdxs[i] = tempIdxs.get(i).intValue();
            cTerms[i] = tempTerms.get(i).doubleValue();
        }

        return constraint;
    }

    public List<LfsConstraint> exportToLfsConstraintsList() throws IOException, IloException {
        Iterator it = cplex.rangeIterator();
        List<LfsConstraint> retList = new ArrayList<LfsConstraint>(rngConstraints.length + 2);
        LfsConstraint constraint;


        while (it.hasNext())
        {
            constraint = iloRangeToLfsConstraint((IloRange)it.next());
            if (constraint == null)
                return null;
            retList.add(constraint);
        }

        if (tConstraintPos != null)
            retList.add(iloRangeToLfsConstraint(tConstraintPos));

        if (tConstraintNeg != null)
            retList.add(iloRangeToLfsConstraint(tConstraintNeg));

        return retList;
    }

    void exportModelDPvmFormat(String fileName) throws IOException, IloException {

        File f = new File(fileName);

        if (!f.exists())
            f.createNewFile();

        FileWriter fstream = new FileWriter(f.getAbsoluteFile());
        PrintWriter out = new PrintWriter(fstream);
        Iterator it = cplex.rangeIterator();
        IloRange range;
        IloLinearNumExprIterator numIt;
        IloNumVar cVar;
        String line;
        int varIdx;


        while (it.hasNext())
        {
            range = (IloRange)it.next();

            line = "";

            numIt = ((IloLinearNumExpr)range.getExpr()).linearIterator();
            while (numIt.hasNext())
            {
                cVar = numIt.nextNumVar();
                varIdx = getVariableIndex(cVar);

                if (varIdx < 0)
                    continue;//careful, this could mean there are unaccounted for variables

                line += String.format(" %d:%8f", varIdx, numIt.getValue());
            }

            if (line == "")
                continue;//this could mean there are unaccounted for constraints

            out.write(String.valueOf(range.getLB()));
            out.write("|");
            out.write(String.valueOf(range.getUB()));
            out.write("|");

            out.println(line);
        }

        out.close();

    }

    public boolean solveDistributedFirstTime() throws IOException, IloException {
        int equationsPerBlock = 400;
        NamedCoordList solveResult;

        GlobalModelBuilder modelBuilder = new GlobalModelBuilder();
        modelBuilder.setConstraintsContainer(exportToLfsConstraintsList());

        List<String> blockModelFiles = modelBuilder.splitIntoBlockFiles( equationsPerBlock, true );
        String objectiveFile = modelBuilder.generateStartingPointFile(1.0);

        globalSolver = new IterativeGlobalSolver(blockModelFiles, objectiveFile);

        solveResult = globalSolver.runSolver(2000);
        if (solveResult == null)
            return false;

        if (!extractSolution(solveResult))
            return false;

        return true;
    }

    public boolean solveDistributedRecuringTime() throws IOException, IloException {
        NamedCoordList solveResult = globalSolver.reRunSolver(2000);

        if (solveResult == null)
            return false;

        if (!extractSolution(solveResult))
            return false;

        return true;
    }

    public boolean solveDistributed() throws IOException, IloException {

        if (globalSolver == null)
            return solveDistributedFirstTime();

        return solveDistributedRecuringTime();
    }

    private boolean extractSolution(NamedCoordList source)
    {
        int i, j;
        boolean ret = true;

        for (i = 0; i < baseCount; i++)
        {
            String cName = vars[i].getName();

            for (j = 0; j < source.size(); j++)
                if (source.get(j).name.compareTo(cName) == 0)
                    break;

            if (j < source.size())
                core.alphas[i] = source.get(j).val;
            else
                ret = false;
        }

        for (i = baseCount; i < 2 * baseCount; i++)
        {
            String cName = vars[i].getName();

            for (j = 0; j < source.size(); j++)
                if (source.get(j).name.compareTo(cName) == 0)
                    break;

            if (j < source.size())
                core.sigmas[i - baseCount] = source.get(j).val;
            else
                ret = false;
        }

        String cName = vars[2 * baseCount].getName();

        for (j = 0; j < source.size(); j++)
            if (source.get(j).name.compareTo(cName) == 0)
                break;

        if (j < source.size())
            core.offsetB = source.get(j).val;
        else
            ret = false;

        return ret;
    }

    protected void setDenominatorUnityEqualityConstraint(int rngIdx) throws IloException {
        int i;

        IloLinearNumExpr lin;

        lin = cplex.linearNumExpr();

        for (i = 0; i < baseCount; i++)
            lin.addTerm(core.kpos[i] - core.kneg[i], vars[i]);


        rngConstraints[rngIdx] = cplex.addEq(1.0, lin, "UnityEq");
    }

    protected void setSingleLPTypeObjectiveWithBias(double positiveBias) throws IloException {
        int i;
        IloLinearNumExpr lin;
        double posTerm = positiveBias * (core.xNeg.length - 1), negTerm = core.xPos.length - 1;

        assert(posTerm > 0 && negTerm > 0);

        lin = cplex.linearNumExpr();
        for (i = 0; i < core.xPos.length; i++)
            lin.addTerm(posTerm, vars[core.xPos[i] + baseCount]);

        for (i = 0; i < core.xNeg.length; i++)
            lin.addTerm(negTerm, vars[core.xNeg[i] + baseCount]);

        obj = cplex.addMinimize(lin);
    }

    private void setSingleLPTypeObjective() throws IloException {
        setSingleLPTypeObjectiveWithBias(1.0);
    }

    private void setDenominatorEqualityConstraintInverse(int rngIdx, boolean setToZero) throws IloException{
        int i;
        double posTerm = core.xNeg.length - 1, negTerm = core.xPos.length - 1;
        IloLinearNumExpr lin;

        lin = cplex.linearNumExpr();

        for (i = 0; i < core.xPos.length; i++)
            lin.addTerm(posTerm, vars[core.xPos[i] + baseCount]);

        for (i = 0; i < core.xNeg.length; i++)
            lin.addTerm(negTerm, vars[core.xNeg[i] + baseCount]);

        if (setToZero)
            rngConstraints[rngIdx] = cplex.addEq(0, lin, "UnityEq");
        else
            rngConstraints[rngIdx] = cplex.addEq(posTerm * negTerm, lin, "UnityEq");
    }

    private void setSingleLPTypeObjectiveInverse(double positiveTrainBias) throws IloException{
        int i;

        IloLinearNumExpr lin;

        lin = cplex.linearNumExpr();

        for (i = 0; i < baseCount; i++)
            lin.addTerm((core.kpos[i] / positiveTrainBias) - core.kneg[i], vars[i]);

        obj = cplex.addMaximize(lin);
    }

    public boolean buildSingleLPSystemWithBias(PvmDataCore pvms, double positiveBias) throws IloException {
        cleanCplex();
        addCplexSolver( false );

        core = pvms;
        baseCount = core.entries.size();

        rngConstraints = new IloRange[baseCount * 2 + 1];

        if (!CreateVariables(false, false, Double.MAX_VALUE))
            return false;

        if (!AddSigmaRegularConstraints())
            return false;

        setDenominatorUnityEqualityConstraint(baseCount * 2);
        setSingleLPTypeObjectiveWithBias(positiveBias);

        return true;
    }

    public boolean buildSingleLPSystem(PvmDataCore pvms, boolean saveSys, boolean nameAllVars, boolean nameAllConstraints) throws IloException {

        addCplexSolver( false );

        core = pvms;
        baseCount = core.entries.size();

        rngConstraints = new IloRange[baseCount * 2 + 1];

        if (!CreateVariables(false, false, Double.MAX_VALUE))
            return false;

        if (!AddSigmaRegularConstraints())
            return false;

        setDenominatorUnityEqualityConstraint(baseCount * 2);
        setSingleLPTypeObjective();

        //setDenominatorEqualityConstraintInverse(baseCount * 2);
        //setSingleLPTypeObjectiveInverse();

        if (nameAllVars)
            setVariableNames();

        if (nameAllConstraints)
            SetConstraintsNames();

        if (saveSys)
            SaveModel();

        return true;
    }

    public boolean solveSingleLPWithBias( double [] resT, double positiveTrainBias ) throws IloException {
        int i;

	    // the actual solving of the system
        if ( !cplex.solve() ) return false;

        assert ( resT.length > 0 );

        double [] x = cplex.getValues( vars );

        for (i = 0; i < baseCount; i++)
            core.alphas[i] = x[i];

        for (i = 0; i < baseCount; i++)
            core.sigmas[i] = x[i + baseCount];

        core.recomputeHyperplaneBias( resT, positiveTrainBias );
        //core.recomputeHyperplaneBiasOptimizingAccuracy();
        //if (!core.recomputeHyperplaneBiasOptimizingIQR())
          //  core.recomputeHyperplaneBias(resT, positiveTrainBias);

        return true;
    }

    public boolean solveSingleLP(double [] resT) throws IloException {
        return solveSingleLPWithBias(resT, 1.0);
    }

    public boolean solveSingleLPSecondary(double [] resT) throws IloException{
        return solveSingleLPSecondary(resT, 1.0);
    }

    public boolean solveSingleLPSecondary(double [] resT, double positiveTrainBias) throws IloException {
        int i, sigIdx;

        if (!cplex.solve())
            return false;

        assert (resT.length > 0);

        double [] x = cplex.getValues(vars);

        for (i = 0; i < baseCount; i++)
            core.alphas[i] = x[i];

        for (i = 0; i < baseCount; i++)
            core.sigmas[i] = 0;

        core.recomputeHyperplaneBias(resT, positiveTrainBias);

        return true;

    }

    private void CreateSecondaryConstrainedVariables(double constrainedAlphasLimit) throws IloException {

        int i;
        vars = new IloNumVar[baseCount];

        if (constrainedAlphasLimit < 0)
            constrainedAlphasLimit = -constrainedAlphasLimit;
        for (i = 0; i < baseCount; i++)
            vars[i] = cplex.numVar(-constrainedAlphasLimit, constrainedAlphasLimit, IloNumVarType.Float);
    }

    private void AddSigmaVoidConstraints() throws IloException {
        int i, rngIdx = 0;

        for (i = 0; i < core.xPos.length; i++, rngIdx++)
            AddSigmaVoidConstraintPos(core.xPos[i], rngIdx);


        for (i = 0; i < core.xNeg.length; i++, rngIdx++)
            AddSigmaVoidConstraintNeg(core.xNeg[i], rngIdx);
    }

    private void AddSigmaVoidConstraintPos(int idx, int rngIdx) throws IloException {
        int i;
        IloLinearNumExpr lin = null;

        lin = cplex.linearNumExpr();

        for (i = 0; i < baseCount; i++)
            lin.addTerm(core.kpos[i] - core.gramMtx[idx][i], vars[i]);

        rngConstraints[rngIdx] = cplex.addEq(lin, 0.0);
    }

    private void AddSigmaVoidConstraintNeg(int idx, int rngIdx) throws IloException {
        int i;
        IloLinearNumExpr lin = null;

        lin = cplex.linearNumExpr();

        for (i = 0; i < baseCount; i++)
            lin.addTerm(core.kneg[i] - core.gramMtx[idx][i], vars[i]);

        rngConstraints[rngIdx] = cplex.addEq(lin, 0.0);
    }

    public boolean buildSecondaryLpSystem (PvmDataCore pvms) throws IloException {
        return buildSecondaryLpSystem(pvms, 1.0);
    }

    public boolean buildSecondaryLpSystem (PvmDataCore pvms, double positiveTrainBias) throws IloException {

        addCplexSolver( false );

        core = pvms;
        baseCount = core.entries.size();

        rngConstraints = new IloRange[baseCount];

        CreateSecondaryConstrainedVariables(1.0);
        AddSigmaVoidConstraints();
        addUnitSphereConstraint();
        setSingleLPTypeObjectiveInverse(positiveTrainBias);

        return true;
    }

    protected void addUnitSphereConstraint() throws IloException {

        IloLQNumExpr sphereConstraint = cplex.lqNumExpr();

        for (int i = 0; i < baseCount; i++)
            sphereConstraint.addTerm(1.0, vars[i], vars[i]);

        cplex.addLe(sphereConstraint, 1.0);
    }

    protected void cleanCplex() throws IloException {
        if (cplex != null)
        {
            cplex.clearModel();
            cplex.endModel();
            cplex.end();
        }
        cplex = null;
    }

    @Override
    protected void finalize() throws Throwable {
        cleanCplex();
	    super.finalize();
    }

    protected void addCplexSolver( boolean verbalize ) throws IloException {
        cleanCplex();
        cplex = new IloCplex();

        if ( !verbalize ) { cplex.setOut(null); }

	    cplex.setParam( IloCplex.BooleanParam.NumericalEmphasis, true );
	    cplex.setParam( IloCplex.IntParam.Threads, 2 );
	    //cplex.setParam( IloCplex.IntParam.ParallelMode, IloCplex.ParallelMode.Deterministic );

	    //cplex.setParam( IloCplex.IntParam.RootAlg, IloCplex.Algorithm.Dual );
    }
}
