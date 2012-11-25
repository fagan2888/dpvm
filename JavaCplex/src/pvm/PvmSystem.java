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
    IloNumVar [] vars;//the variables
    IloRange [] rngConstraints;//the constraints returned by the solver

    /*the two constraints that express:
        sigma_+ <= t * E_+
        simga_- <= t * E_-
    */
    IloRange tConstraintPos = null, tConstraintNeg = null;
    IloObjective obj;

    PvmDataCore core;
    int baseCount;

    GlobalSolver globalSolver = null;

    public boolean BuildSystemFor(PvmDataCore pvms, double t, boolean saveSys, boolean nameAllConstraints){
        cplex = null;
        try
        {
            cplex = new IloCplex();
        }
        catch (ilog.concert.IloException e)
        {
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

    public boolean CreateVariables(){
        int i;
        vars = new IloNumVar[baseCount * 2 + 1];

        try
        {
            for (i = 0; i < baseCount; i++)
                vars[i] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, IloNumVarType.Float);

            for (i = 0; i < baseCount; i++)
                vars[i + baseCount] = cplex.numVar(0, Double.MAX_VALUE, IloNumVarType.Float);


            vars[2 * core.entries.size()] = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, IloNumVarType.Float);
        }
        catch(ilog.concert.IloException e)
        {
            return false;
        }

        return true;

    }

    public boolean AddSigmaRegularConstraints(){
        int i, rngIdx = 0;

        for (i = 0; i < core.xPos.length; i++, rngIdx += 2)
            if (!AddConstraintsForIdxPos(core.xPos[i], rngIdx))
                return false;

        for (i = 0; i < core.xNeg.length; i++, rngIdx += 2)
            if (!AddConstraintsForIdxNeg(core.xNeg[i], rngIdx))
                return false;

        return true;

    }

    public boolean AddConstraintsForIdxPos(int idx, int rngIdx){

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

    public boolean AddConstraintsForIdxNeg(int idx, int rngIdx){
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

    public boolean AddStrictlyPositiveAveragesConstraints(int rngIdx){
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

        globalSolver = new GlobalSolver(blockModelFiles, objectiveFile);

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


}
