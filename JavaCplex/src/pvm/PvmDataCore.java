package pvm;

import util.RandomUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

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
    public static double doubleEps = 1e-10;


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
        ComputeGramMatrix();
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
                gramMtx[i][j] = KerProduct.ComputeKerProd(entries.get(i), entries.get(j));
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

    public boolean CheckResult(double [] resT, boolean useFixedAverageThresh){
        int i, j;
        double epos = offsetB, eneg = offsetB;
        double localDist, localSigma;
        double sigmaPos = 0, sigmaNeg = 0;


        //first compute the averages and see if they turned out okay
        for (i = 0; i < kpos.length; i++)
            epos += alphas[i] * kpos[i];

        for (i = 0; i < kneg.length; i++)
            eneg += alphas[i] * kneg[i];

        if (useFixedAverageThresh && (epos < 1.0 - doubleEps || -eneg < 1.0 - doubleEps))
            return false;

        if (!useFixedAverageThresh && (epos < doubleEps || -eneg < doubleEps))
            return false;

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

            if (localSigma > sigmas[i] + doubleEps)
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

        if (sigmaPos / epos > resT[0] + doubleEps)
            return false;

        if (- sigmaNeg / eneg > resT[0] + doubleEps)
            return false;

        resT[0] = sigmaPos / epos;
        if (resT[0] < - sigmaNeg / eneg)
            resT[0] = - sigmaNeg / eneg;

        return true;
    }

    public void recomputeHyperplaneBias(double [] resT){
        int i;
        double sigmaPos = 0, sigmaNeg = 0, ksumPos = 0, ksumNeg = 0;

        offsetB = 0.0;

        assert(xPos.length > 1 && xNeg.length > 1);

        for (i = 0; i < xPos.length; i++)
            sigmaPos += sigmas[xPos[i]];

        sigmaPos /= (double)(xPos.length - 1);

        for (i = 0; i < xNeg.length; i++)
            sigmaNeg += sigmas[xNeg[i]];

        sigmaNeg /= (double)(xNeg.length - 1);

        for (i = 0; i < alphas.length; i++)
        {
            ksumPos += alphas[i] * kpos[i];
            ksumNeg += alphas[i] * kneg[i];
        }

        if (sigmaPos < doubleEps && sigmaNeg < doubleEps)
            return;

        offsetB = - (sigmaPos * ksumNeg + sigmaNeg * ksumPos) / (sigmaPos + sigmaNeg);

        resT[0] = sigmaPos / (ksumPos + offsetB);
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
        //todo : do sliceCount data fold
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

    public boolean classify(PvmEntry src){
        int i;
        double sum = offsetB;

        assert (alphas.length == entries.size());

        for (i = 0; i < alphas.length; i++)
        {
            if (alphas[i] == 0)
                continue;

            sum += alphas[i] * KerProduct.ComputeKerProd(src, entries.get(i));
        }

        return sum < 0 ? false : true;
    }

}
