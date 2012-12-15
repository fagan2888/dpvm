package pvm;

import dsolve.LocalSolver;
import ilog.concert.IloException;
import sun.awt.geom.Crossings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/25/12
 * Time: 8:31 PM
 */

public class PvmSolver {
    public PvmDataCore core;
    public PvmSystem pvmSys;

    public PvmSolver(){
        core = new PvmDataCore();
        pvmSys = new PvmSystem();
    }
    public boolean Train(){
        double t_l = 0.0, t_r = 1.0, t_c;
        core.Init();

        pvmSys.BuildSystemFor(core, 1.0, false, false);

        while (!pvmSys.Solve() && t_r < 1e+10)
        {
            t_l = t_r;
            t_r = 2 * t_r + 1;

            pvmSys.AddFinalTConstraints(t_r);
        };

        if (t_r >= 1e+10)
            return false;

        while (t_r - t_l > 1e-8)
        {
            t_c = (t_l + t_r) / 2.0;

            pvmSys.AddFinalTConstraints(t_c);
            if (pvmSys.Solve())
                t_r = t_c;
            else
                t_l = t_c;
        }

        double [] resT = new double[1];
        resT[0] = t_r;
        if (!core.CheckResult(resT, true))
            return false;

        return true;
    }

    public boolean TrainDistributed() throws IOException, IloException, LocalSolver.LocalSolverInputException {
        double t_l = 0.0, t_r = 1.0, t_c;
        core.Init();

        pvmSys.BuildSystemFor(core, 1.0, false, false);

        while (!pvmSys.solveDistributed() && t_r < 1e+10)
        {
            t_l = t_r;
            t_r = 2 * t_r + 1;

            pvmSys.replaceFinalTConstraintsDistributed( t_r );
        };

        if (t_r >= 1e+10)
            return false;

        while (t_r - t_l > 1e-8)
        {
            t_c = (t_l + t_r) / 2.0;

            pvmSys.replaceFinalTConstraintsDistributed( t_c );
            if (pvmSys.solveDistributed())
                t_r = t_c;
            else
                t_l = t_c;
        }

        double [] resT = new double[1];
        resT[0] = t_r;
        if (!core.CheckResult(resT, true))
            return false;

        return true;
    }

    public boolean TrainSingleLP()throws IloException {
        boolean ret;
        double [] resT = new double[1];

        core.Init();
        pvmSys.buildSingleLPSystem(core, false, false, false);
        ret = pvmSys.solveSingleLP(resT);

        if (ret && resT[0] == 0.0)
        {
            pvmSys.buildSecondaryLpSystem(core);
            ret = pvmSys.solveSingleLPSecondary(resT);
            //ret = pvmSys.solveSingleLP(resT);
        }

        assert(core.CheckResult(resT, false));
        return ret;
    }

    public boolean TrainSingleLPWithBias(double positiveBias) throws IloException {
        boolean ret;
        double [] resT = new double[1];

        core.Init();
        pvmSys.buildSingleLPSystemWithBias(core, positiveBias);
        ret = pvmSys.solveSingleLPWithBias(resT, positiveBias);

        if (ret && resT[0] == 0.0)
        {
            pvmSys.buildSecondaryLpSystem(core);
            ret = pvmSys.solveSingleLPSecondary(resT, positiveBias);
        }

        //assert(core.CheckResult(resT, false));
        return ret;
    }

    public boolean classify(PvmEntry src){
        return core.classify(src);
    }

    public boolean [] classify(ArrayList<PvmEntry> entries){
        int i;
        boolean retLabels[] = new boolean[entries.size()];


        for (i = 0; i < retLabels.length; i++){
            retLabels[i] = classify(entries.get(i));
        }

        return retLabels;
    }

    public static void computeAccuracy(boolean labels[], ArrayList<PvmEntry> entries, double [] accuracy, double [] sensitivity, double [] specificity){
        int i;
        int tp = 0, tn = 0, fp = 0, fn = 0;

        assert (labels.length == entries.size());

        for (i = 0; i < labels.length; i++)
        {
            if (entries.get(i).label)
            {
                if (labels[i])
                    tp++;
                else
                    fn++;
            }
            else
            {
                if (labels[i])
                    fp++;
                else
                    tn++;
            }
        }

        assert (specificity.length > 0 && sensitivity.length > 0 && accuracy.length > 0);

        if (fn + tp > 0)
            sensitivity[0] = (double)tp / (double)(fn + tp);

        if (fp + tn > 0)
            specificity[0] = (double)tn / (double)(fp + tn);

        accuracy[0] = (double)(tp + tn) / (double)(entries.size());
    }

    public void performCrossFoldValidationWithPositiveTrainBias(int splitCount, double positiveTrainBias, double accuracy[], double sensitivity[], double specificity[]) throws IloException{
        int i, solvesCount = 0;
        core.Init(false);


        ArrayList<PvmDataCore> splitCores = core.splitRandomIntoSlices(splitCount);
        ArrayList<PvmDataCore> tempCores = new ArrayList<PvmDataCore>(splitCores.size());
        PvmDataCore cTestCore;
        PvmSolver localSlv = new PvmSolver();
        double tempAccuracy[] = new double[1];
        double tempSensitivity[] = new double[1];
        double tempSpecificity[] = new double[1];


        assert (splitCount == splitCores.size());
        assert(accuracy.length > 0 && sensitivity.length > 0 && specificity.length > 0);

        accuracy[0] = sensitivity[0] = specificity[0] = 0.0;

        for (i = 0; i < splitCount; i++)
        {
            tempCores.clear();
            tempCores.addAll(splitCores);

            cTestCore = tempCores.get(i);

            tempCores.remove(i);
            localSlv.core = PvmDataCore.mergeCores(tempCores);

            if (!localSlv.TrainSingleLPWithBias(positiveTrainBias))
                continue;

            solvesCount++;

            boolean localLabels[] = localSlv.classify(cTestCore.entries);
            localSlv.computeAccuracy(localLabels, cTestCore.entries, tempAccuracy, tempSensitivity, tempSpecificity);

            accuracy[0] += tempAccuracy[0];
            sensitivity[0] += tempSensitivity[0];
            specificity[0] += tempSpecificity[0];
        }

        if (solvesCount > 0)
        {
            accuracy[0] /= (double)solvesCount;
            sensitivity[0] /= (double)solvesCount;
            specificity[0] /= (double)solvesCount;
        }

        return;
    }

    public void performCrossFoldValidation(int splitCount, double accuracy[], double sensitivity[], double specificity[]) throws IloException {
        performCrossFoldValidationWithPositiveTrainBias(splitCount, 1.0, accuracy, sensitivity, specificity);
    }

    public void searchPositiveTrainBias(int splitCount) throws IloException{
        double bestTrainBias = 0.1, tempTrainBias;
        double bestAcc = 0.0;

        double tempAccuracy[] = new double[1];
        double tempSensitivity[] = new double[1];
        double tempSpecificity[] = new double[1];

        double cpow, first_pow, last_pow;

        first_pow = -2;
        last_pow = 2;
        for (cpow = first_pow; cpow < last_pow; cpow += 0.2){
            tempTrainBias = Math.exp(cpow);

            performCrossFoldValidationWithPositiveTrainBias(splitCount, tempTrainBias,tempAccuracy, tempSensitivity, tempSpecificity);

            if (bestAcc < tempAccuracy[0]){
                bestAcc = tempAccuracy[0];
                bestTrainBias = tempTrainBias;
            }
        }

        first_pow = bestTrainBias - 0.4;
        last_pow = bestTrainBias + 0.4;

        for (cpow = first_pow; cpow < last_pow; cpow += 0.05){
            tempTrainBias = Math.exp(cpow);

            performCrossFoldValidationWithPositiveTrainBias(splitCount, tempTrainBias,tempAccuracy, tempSensitivity, tempSpecificity);

            if (bestAcc < tempAccuracy[0]){
                bestAcc = tempAccuracy[0];
                bestTrainBias = tempTrainBias;
            }
        }


        return;
    }

    public void searchKernel(int splitCount) throws IloException {

        int i, last_i;
        KerProduct.KerType bestKerType = KerProduct.KerType.KERSCALAR;
        double bestParamD = 0.0;
        int bestParamI = 0;

        double tempParamD;
        int tempParamI;

        double bestAcc = 0.0;

        double tempAccuracy[] = new double[1];
        double tempSensitivity[] = new double[1];
        double tempSpecificity[] = new double[1];


        for (KerProduct.KerType kerType : KerProduct.KerType.values())
        {
        //    KerProduct.KerType kerType = KerProduct.KerType.KERRBF;
            KerProduct.kerType = kerType;


            last_i = KerProduct.getParamDMaxStepsCount();

            for (i = 0; i < last_i; i++){
                tempParamD = KerProduct.getParamDValue(i , last_i);
                //tempParamD = Math.pow(2, -17);
                KerProduct.paramD = tempParamD;

                for (tempParamI = KerProduct.getMinParamI(); tempParamI <= KerProduct.getMaxParamI(); tempParamI++)
                {
                    System.gc();
                    KerProduct.paramI = tempParamI;

                    performCrossFoldValidation(splitCount, tempAccuracy, tempSensitivity, tempSpecificity);

                    if (tempAccuracy[0] > bestAcc)
                    {
                        bestAcc = tempAccuracy[0];
                        bestKerType = kerType;
                        bestParamD = tempParamD;
                        bestParamI = tempParamI;
                    }
                }
            }
        }

        KerProduct.kerType = bestKerType;
        KerProduct.paramD = bestParamD;
        KerProduct.paramI = bestParamI;
    }

    public void writeNormalizedDistancesToFiles(String fileNamePos, String fileNameNeg) throws IloException, IOException {
        int i;
        if (!TrainSingleLP())
            return;

        double distancesPos[] = core.getNormalizedSignedDistancesPos();
        FileWriter fw = new FileWriter(new File(fileNamePos));

        for (i = 0; i < distancesPos.length; i++)
            fw.write(String.valueOf(distancesPos[i]) + "\n");

        fw.close();


        double distancesNeg[] = core.getNormalizedSignedDistancesNeg();
        fw = new FileWriter(new File(fileNameNeg));

        for (i = 0; i < distancesNeg.length; i++)
            fw.write(String.valueOf(distancesNeg[i]) + "\n");

        fw.close();
    }

    public static void main(String[] args ) throws IOException, IloException, LocalSolver.LocalSolverInputException {

        if (args.length < 1)
            return;

        PvmSolver solver = new PvmSolver();

        KerProduct.kerType = KerProduct.KerType.KERSCALAR;
        KerProduct.paramD = 1.0/32.0;
        KerProduct.paramI = 1;


        solver.core.ReadFile(args[0]);
        //solver.Train();
        //solver.TrainDistributed();

        double tempAccuracy[] = new double[1];
        double tempSensitivity[] = new double[1];
        double tempSpecificity[] = new double[1];

          /*
        solver.TrainSingleLP();
        boolean labels[] = solver.classify(solver.core.entries);
        solver.computeAccuracy(labels, solver.core.entries, tempAccuracy, tempSensitivity, tempSpecificity);
        */
        /*
        solver.writeNormalizedDistancesToFiles(args[0] + ".dstP", args[0] + ".dstN");

        solver.TrainSingleLP();
        boolean retLabels[] = solver.classify(solver.core.entries);
        PvmSolver.computeAccuracy(retLabels, solver.core.entries, tempAccuracy, tempSensitivity, tempSpecificity);
         */
/*        KerProduct.kerType = KerProduct.KerType.KERRBF;

        int last_i = KerProduct.getParamDMaxStepsCount();
        for (int i = 0; i < last_i; i++){
            KerProduct.paramD = KerProduct.getParamDValue(i, last_i);
            solver.searchPositiveTrainBias(5);
        }
  */
        KerProduct.kerType = KerProduct.KerType.KERSCALAR;
        solver.searchPositiveTrainBias(5);

        //solver.searchKernel(10);
        //solver.performCrossFoldValidation(5, tempAccuracy, tempSensitivity, tempSpecificity);

        //System.out.print("CrossFold Val Results: acc" + String.valueOf(tempAccuracy[0]) + " | ");
    }
}
