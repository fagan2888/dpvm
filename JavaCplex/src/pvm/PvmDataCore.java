package pvm;

import pvm.KernelProducts.KernelProductManager;
import util.RandomUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/28/12
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class PvmDataCore {

    public ArrayList<PvmEntry> entries;
    public int [] xPos, xNeg;
    public double [][] gramMtx;
    public double [] kpos, kneg;
    public double [] alphas;
    public double [] sigmas;
    public double offsetB;
    public double ePos, eNeg, sigPos, sigNeg;
    public static double epsDouble = 1e-10;
    public static double epsFloat = 1e-4;


    public PvmDataCore(){
        entries = new ArrayList<PvmEntry>(0);
    }

    public PvmDataCore(int entriesCapacity){
        entries = new ArrayList<PvmEntry>(entriesCapacity);
    }

    public void ReadFile(String FileName) throws IOException {
        //String ss = "/work/cplex-pvm/dbs/mushroom/agaricus-lepiota.data.mean";
        entries = DatabaseLoader.loadEntries(FileName);
    }

    public void Init(){
        Init(true);
    }

    public void Init(boolean computeGramMtx){
        alphas = new double[entries.size()];
        sigmas = new double[entries.size()];
        offsetB = 0;

        SortNegativeAndPositiveEntries();

	    if ( computeGramMtx ) {
            ComputeGramMatrix();
	    }
    }

    public void SortNegativeAndPositiveEntries(){
        int i, ip, in;
        int posCount = 0;

        for (i = 0; i < entries.size(); i++)
            if (entries.get(i).label)
                posCount++;

        xPos = new int[posCount];
        xNeg = new int[entries.size() - posCount];

        for (i = 0, ip = 0, in = 0; i < entries.size(); i++)
        {
            if (entries.get(i).label)
            {
                xPos[ip] = i;
                ip++;
            }
            else
            {
                xNeg[in] = i;
                in++;
            }
        }
    }

    public double ComputeKerProd(int idx0, int idx1){
        int i, last_i;
        double res = 0;
        double [] x0 = entries.get(idx0).x;
        double [] x1 = entries.get(idx1).x;

        last_i = entries.get(idx0).x.length;
        assert(last_i == entries.get(idx1).x.length);

        for (i = 0; i < last_i; i++)
            res += x0[i] * x1[i];

        return res;
    }

    public void ComputeGramMatrix(){
        int i, j;


        gramMtx = new double[entries.size()][entries.size()];

        for (i = 0; i < entries.size(); i++)
            for (j = i; j < entries.size(); j++)
            {
                //gramMtx[i][j] = ComputeKerProd(i, j);
                gramMtx[i][j] = KernelProductManager.ComputeKerProd(entries.get(i), entries.get(j));
                gramMtx[j][i] = gramMtx[i][j];
            }


        ComputeKPrimes();
    }

    public void ComputeKPrimes(){
        int i, j, last_i, last_j;
        double ckpos, ckneg;

        kpos = new double[entries.size()];
        kneg = new double[entries.size()];

        last_i = entries.size();

        for (i = 0; i < last_i; i++)
        {
            ckpos = ckneg = 0;

            for (j = 0; j < xPos.length; j++)
                ckpos += gramMtx[i][xPos[j]];

            ckpos /= (double)xPos.length;

            for (j = 0; j < xNeg.length; j++)
                ckneg += gramMtx[i][xNeg[j]];

            ckneg /= (double)xNeg.length;

            kpos[i] = ckpos;
            kneg[i] = ckneg;
        }
    }

    public boolean CheckResult(double [] resT, boolean useFixedAverageThresh, boolean checkAverages){
        int i, j;
        double epos = offsetB, eneg = offsetB;
        double localDist, localSigma;
        double sigmaPos = 0, sigmaNeg = 0;


        //first compute the averages and see if they turned out okay
        for (i = 0; i < kpos.length; i++)
            epos += alphas[i] * kpos[i];

        for (i = 0; i < kneg.length; i++)
            eneg += alphas[i] * kneg[i];

        if (checkAverages){
            if (useFixedAverageThresh && (epos < 1.0 - epsDouble || -eneg < 1.0 - epsDouble))
                return false;

            if (!useFixedAverageThresh && (epos < epsDouble || -eneg < epsDouble))
                return false;
        }

        //then compute the deviation of the distance associated with each record and see if it checks out

        for (i = 0; i < entries.size(); i++)
        {
            localDist = offsetB;

            for (j = 0; j < entries.size(); j++)
                localDist += alphas[j] * gramMtx[i][j];

            if (entries.get(i).label)
                localSigma = localDist - epos;
            else
                localSigma = localDist - eneg;

            if (localSigma < 0)
                localSigma = -localSigma;

            if (localSigma > sigmas[i] + epsFloat)
                return false;
        }

        for (i = 0; i < xPos.length; i++)
            sigmaPos += sigmas[xPos[i]];

        assert(xPos.length > 1);
        sigmaPos /= (double)(xPos.length - 1);

        for (i = 0; i < xNeg.length; i++)
            sigmaNeg += sigmas[xNeg[i]];

        assert(xNeg.length > 1);
        sigmaNeg /= (double)(xNeg.length - 1);

        if (sigmaPos / epos > resT[0] + epsFloat)
            return false;

        if (- sigmaNeg / eneg > resT[0] + epsFloat)
            return false;

        resT[0] = sigmaPos / epos;
        if (resT[0] < - sigmaNeg / eneg)
            resT[0] = - sigmaNeg / eneg;

        return true;
    }

    public void recomputeHyperplaneBias(double [] resT){
        recomputeHyperplaneBias(resT, 1.0);
    }

    protected void computeCurrentEntriesUnbiasedScore(){
        assert entries.size() == alphas.length;

        for (PvmEntry entry : entries)
            entry.compareScore = getSignedDistance(entry);
    }

    protected int computeIndexSplitMaximizingAccuracy(PvmEntry tempEntries[]){
        int i, besti = -1;
        int falsePosCount, falseNegCount, bestFalseCount;

        Arrays.sort(tempEntries);
        falseNegCount = 0;
        falsePosCount = xNeg.length;

        bestFalseCount = falseNegCount + falsePosCount;

        for (i = 0; i < tempEntries.length; i++){
            if (tempEntries[i].label)
                falseNegCount++;
            else
                falsePosCount--;

            if (bestFalseCount > falsePosCount + falseNegCount){
                bestFalseCount = falsePosCount + falseNegCount;
                besti = i;
            }
        }

        return besti;

    }

    public boolean recomputeHyperplaneBiasOptimizingIQR(){
        PvmEntry tempEntries[] = new PvmEntry[entries.size()];

        int i;
        for (i = 0; i < tempEntries.length; i++)
            tempEntries[i] = entries.get(i);

        offsetB = 0.0;
        computeCurrentEntriesUnbiasedScore();

        Arrays.sort(tempEntries);

        int locCount;
        double medianNeg = 0, medianPos = 0, iqrNeg, iqrPos;
        double lowQ = 0, uppQ = 0;

        locCount = 0;

        for (i = 0; i < tempEntries.length; i++){
            if (tempEntries[i].label)
                continue;

            locCount++;

            if (locCount == xNeg.length / 4)
                lowQ = tempEntries[i].compareScore;
            else if (locCount == xNeg.length / 2)
                medianNeg = tempEntries[i].compareScore;
            else if (locCount == (xNeg.length * 3) / 4){
                uppQ = tempEntries[i].compareScore;
                break;
            }
        }

        iqrNeg = uppQ - lowQ;
        if (iqrNeg < epsDouble)
            iqrNeg = epsDouble;

        locCount = 0;
        for (i = 0; i < tempEntries.length; i++){
            if (!tempEntries[i].label)
                continue;

            locCount++;

            if (locCount == xPos.length / 4)
                lowQ = tempEntries[i].compareScore;
            else if (locCount == xPos.length / 2)
                medianPos = tempEntries[i].compareScore;
            else if (locCount == (xPos.length * 3) / 4){
                uppQ = tempEntries[i].compareScore;
                break;
            }

        }

        iqrPos = uppQ - lowQ;
        if (iqrPos < epsDouble)
            iqrPos = epsDouble;


        if (medianNeg > medianPos)
            return false;

        offsetB = -(iqrNeg * medianNeg + iqrPos * medianPos) / (iqrNeg + iqrPos);

        return true;
    }

    public boolean recomputeHyperplaneBiasOptimizingAccuracy(){
        PvmEntry tempEntries[] = new PvmEntry[entries.size()];

        int i;
        for (i = 0; i < tempEntries.length; i++)
            tempEntries[i] = entries.get(i);

        offsetB = 0.0;
        computeCurrentEntriesUnbiasedScore();
        int splitIdx = computeIndexSplitMaximizingAccuracy(tempEntries);

        if (splitIdx < 2 || splitIdx > tempEntries.length - 2)
            return false;


        int locPosCount = 0, locNegCount = 0, tempCount;
        double locMedpos = 0, locMedneg = 0, tempLowQ = 0, tempUppQ = 0;
        double iqrPos, iqrNeg;


        for (i = 0; i <= splitIdx; i++){
            if (!tempEntries[i].label)
                locNegCount++;
        }

        tempCount = locNegCount;
        locNegCount = 0;
        for (i = 0; i <= splitIdx; i++){
            if (!tempEntries[i].label)
                locNegCount++;

            if (locNegCount == tempCount / 4)
                tempLowQ = tempEntries[i].compareScore;
            else if (locNegCount == tempCount / 2)
                locMedneg = tempEntries[i].compareScore;
            else if (locNegCount == (tempCount * 3) / 4)
                tempUppQ = tempEntries[i].compareScore;
        }

        iqrNeg = tempUppQ - tempLowQ;
        if (iqrNeg < epsDouble)
            iqrNeg = epsDouble;


        for (i = splitIdx + 1; i < tempEntries.length; i++){
            if (tempEntries[i].label)
                locPosCount++;
        }

        tempCount = locPosCount;
        locPosCount = 0;
        for (i = splitIdx + 1; i < tempEntries.length; i++){
            if (tempEntries[i].label)
                locPosCount++;

            if (locPosCount == tempCount / 4)
                tempLowQ = tempEntries[i].compareScore;
            else if (locPosCount == tempCount / 2)
                locMedpos = tempEntries[i].compareScore;
            else if (locPosCount == (tempCount * 3) / 4)
                tempUppQ = tempEntries[i].compareScore;
        }

        iqrPos = tempUppQ - tempLowQ;
        if (iqrPos < epsDouble)
            iqrPos = epsDouble;

        double biasInterval = tempEntries[splitIdx + 1].compareScore - tempEntries[splitIdx].compareScore;

        biasInterval *= (locMedneg / iqrNeg) / ((locMedneg / iqrNeg) + (locMedpos / iqrPos));

        offsetB = - (tempEntries[splitIdx].compareScore + biasInterval);

        return true;
    }

    public void recomputeHyperplaneBias(double [] resT, double positiveTrainBias){
        int i;
        double ksumPos = 0, ksumNeg = 0;

        sigPos = 0;
        sigNeg = 0;

        offsetB = 0.0;

        assert(xPos.length > 1 && xNeg.length > 1);

        for (i = 0; i < xPos.length; i++)
            sigPos += sigmas[xPos[i]];

        sigPos /= (double)(xPos.length - 1);

        for (i = 0; i < xNeg.length; i++)
            sigNeg += sigmas[xNeg[i]];

        sigNeg /= (double)(xNeg.length - 1);

        for (i = 0; i < alphas.length; i++)
        {
            ksumPos += alphas[i] * kpos[i];
            ksumNeg += alphas[i] * kneg[i];
        }

        if (sigPos < epsDouble && sigNeg < epsDouble)
        {
            offsetB = -(ksumPos + ksumNeg) / (1 + positiveTrainBias);
            resT[0] = 0.0;
        }
        else
        {
            offsetB = - (positiveTrainBias * sigPos * ksumNeg + sigNeg * ksumPos) / (positiveTrainBias * sigPos + sigNeg);
            resT[0] = sigPos / (ksumPos + offsetB);
        }

        ePos = ksumPos + offsetB;
        eNeg = ksumNeg + offsetB;
    }

    private int [] getLabelCopiesPos(){
        int i;
        int ret[] = new int[xPos.length];

        for (i = 0; i < xPos.length; i++)
            ret[i] = xPos[i];

        return ret;
    }
    private int [] getLabelCopiesNeg(){
        int i;
        int ret[] = new int[xNeg.length];

        for (i = 0; i < xNeg.length; i++)
            ret[i] = xNeg[i];

        return ret;
    }

    public ArrayList<PvmDataCore> splitRandomIntoSlices(int sliceCount){

        int i;
        int cxPos[], cxNeg[];
        PvmDataCore cCore;
        ArrayList<PvmDataCore> resCores = new ArrayList<PvmDataCore>(sliceCount);

        resCores.clear();

        if (sliceCount < 1)
            return null;

        for (i = 0; i < sliceCount; i++)
            resCores.add(new PvmDataCore(1 + (entries.size() / sliceCount)));

        cxPos = getLabelCopiesPos();
        cxNeg = getLabelCopiesNeg();

        RandomUtils.doShuffle(cxPos);
        RandomUtils.doShuffle(cxNeg);

        for (i = 0; i < cxPos.length; i++){
            cCore = resCores.get(i % sliceCount);
            cCore.entries.add(entries.get(cxPos[i]));
        }

        for (i = 0; i < cxNeg.length; i++){
            cCore = resCores.get(i % sliceCount);
            cCore.entries.add(entries.get(cxNeg[i]));
        }

        return resCores;
    }

    public static PvmDataCore mergeCores(ArrayList<PvmDataCore> srcCores){
        //to do : merge all the cores into a single resulting core
        int i, entriesCount = 0;
        PvmDataCore resCore;

        for (PvmDataCore cCore : srcCores)
            entriesCount += cCore.entries.size();

        if (entriesCount == 0)
            return null;

        resCore = new PvmDataCore(entriesCount);

        for (PvmDataCore cCore : srcCores){
            resCore.entries.addAll(cCore.entries);
        }

        return resCore;
    }

    public double[] getNormalizedSignedDistancesPos(){
        double ret[] = new double[xPos.length];

        for (int i = 0; i < xPos.length; i++)
            ret[i] = getNormalizedSignedDistance(entries.get(xPos[i]));

        return ret;
    }

    public double[] getNormalizedSignedDistancesNeg(){
        double ret[] = new double[xNeg.length];

        for (int i = 0; i < xNeg.length; i++)
            ret[i] = getNormalizedSignedDistance(entries.get(xNeg[i]));

        return ret;
    }

    public double getNormalizedSignedDistance(PvmEntry src){
        if (src.label){
            if (sigPos < epsDouble)
                return 1e+30;

            return (getSignedDistance(src) - ePos) / sigPos;
        }
        if (sigNeg < epsDouble)
            return 1e+30;

        return (getSignedDistance(src) - eNeg) / sigNeg;
    }

    public double getSignedDistance(PvmEntry src){
        int i;
        double sum = offsetB;

        assert (alphas.length == entries.size());

        for (i = 0; i < alphas.length; i++)
        {
            if (alphas[i] == 0)
                continue;

            sum += alphas[i] * KernelProductManager.ComputeKerProd(src, entries.get(i));
        }

        return sum;
    }

    public boolean classify(PvmEntry src){

        return getSignedDistance(src) < 0 ? false : true;
    }

}
