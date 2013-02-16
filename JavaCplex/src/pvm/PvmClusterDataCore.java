package pvm;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/27/13
 * Time: 2:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class PvmClusterDataCore extends PvmDataCore implements Cloneable{
    ArrayList<int[]> clustersPos = null, clustersNeg = null;
    ArrayList<int[]> clustersTotal = null;
    int clustersCount = 0;
    double clusterSigmasPos[], clusterSigmasNeg[];
    double clusterSigPos, clusterSigNeg;

    protected void allocClusters(){
        clustersPos = new ArrayList<int[]>(1);
        clustersNeg = new ArrayList<int[]>(1);
        clustersTotal = new ArrayList<int[]>(2);
    }

    public void ReadFile(String fileName, String clusterFileName) throws IOException {
        allocClusters();
        super.ReadFile(fileName);
        ArrayList<int[]> clusters = DatabaseLoader.loadClusters(clusterFileName);
        splitClustersAccordingToLabel(clusters);
        buildClustersTotal();
    }

    public void ReadFile(String fileName) throws IOException{
        allocClusters();
        super.ReadFile(fileName);
        Init(false);
        buildDefaultClusters();
    }

    protected void buildDefaultClusters(){
        clustersPos.clear();
        clustersNeg.clear();

        clustersPos.add(xPos.clone());
        clustersNeg.add(xNeg.clone());

        buildClustersTotal();
    }

    protected void buildIndividualClustersForEachElement(){
        int i;

        clustersPos.clear();
        clustersNeg.clear();

        for (int idx : xPos){
            int clst[] = new int[1];
            clst[0] = idx;
            clustersPos.add(clst);
        }

        for (int idx : xNeg){
            int clst[] = new int[1];
            clst[0] = idx;
            clustersNeg.add(clst);
        }

        buildClustersTotal();
    }

    protected void buildKMeansClusters(double clustersRatio) throws Exception {
        ArrayList<PvmEntry> tempEntries = new ArrayList<PvmEntry>(xPos.length);
        int cCount;

        cCount = getEntriesListAndClustersCount(tempEntries, xPos, clustersRatio);
        clustersPos = new ArrayList<int[]>(cCount);
        buildKMeansClustersForEntries(tempEntries, cCount, clustersPos);

        cCount = getEntriesListAndClustersCount(tempEntries, xNeg, clustersRatio);
        clustersNeg = new ArrayList<int[]>(cCount);
        buildKMeansClustersForEntries(tempEntries, cCount, clustersNeg);

        buildClustersTotal();
    }

    private int getEntriesListAndClustersCount(ArrayList<PvmEntry> destEntries, int [] srcIdxs, double clustersRatio){
        destEntries.clear();
        for (int idx : srcIdxs)
            destEntries.add(entries.get(idx));

        int cCount = (int)(xPos.length * clustersRatio + 0.5);

        if (cCount < 2)
            cCount = 2;

        return cCount;
    }

    private void buildKMeansClustersForEntries(ArrayList<PvmEntry> entriesSrc, int cCount, ArrayList<int[]> dest) throws Exception {
        ArrayList<ArrayList<PvmEntry>> retClusters = WekaSimpleKMeans.clusterElements(entriesSrc, cCount);
        int i, j;

        dest.clear();
        for (i = 0; i < retClusters.size(); i++){
            int idxs[] = new int[retClusters.get(i).size()];

            for (j = 0; j < idxs.length; j++)
                idxs[j] = entries.indexOf(retClusters.get(i).get(j));

            if (idxs.length > 0)
                dest.add(idxs);
        }
    }

    protected void buildClustersTotal(){
        if (clustersTotal == null)
            clustersTotal = new ArrayList<int[]>(clustersPos.size() + clustersNeg.size());

        clustersTotal.clear();
        clustersTotal.addAll(clustersPos);
        clustersTotal.addAll(clustersNeg);
        clustersCount = clustersTotal.size();

        if (clusterSigmasPos == null || clusterSigmasPos.length != clustersPos.size())
            clusterSigmasPos = new double[clustersPos.size()];
        if (clusterSigmasNeg == null || clusterSigmasNeg.length != clustersNeg.size())
            clusterSigmasNeg = new double[clustersNeg.size()];
    }

    protected int getSingleClusterPositiveLabelCount(int cluster[]){
        int localPosCount = 0, i;

        for (i = 0; i < cluster.length; i++)
            if (entries.get(cluster[i]).label)
                localPosCount++;

        return localPosCount;
    }

    protected void splitSingleClusterAccordingToLabel(int cluster[], int localPosCount){
        int i;
        int localNegCount = cluster.length - localPosCount;
        int clusterPos[] = new int[localPosCount];
        int clusterNeg[] = new int[localNegCount];

        if (localPosCount * localNegCount == 0)
            return;

        localPosCount = localNegCount = 0;

        for (i = 0; i < cluster.length; i++)
            if (entries.get(cluster[i]).label){
                clusterPos[localPosCount] =  cluster[i];
                localPosCount++;
            }
            else{
                clusterNeg[localNegCount] =  cluster[i];
                localNegCount++;
            }

        clustersPos.add(clusterPos);
        clustersNeg.add(clusterNeg);
    }

    protected void splitClustersAccordingToLabel(ArrayList<int[]> srcClusters){
        int i;
        int localPosCount;

        for (i = 0; i < srcClusters.size(); i++){
            localPosCount = getSingleClusterPositiveLabelCount(srcClusters.get(i));

            if (localPosCount == srcClusters.get(i).length)
                clustersPos.add(srcClusters.get(i));
            else if (localPosCount == 0)
                clustersNeg.add(srcClusters.get(i));
            else
                splitSingleClusterAccordingToLabel(srcClusters.get(i), localPosCount);
        }
    }

    @Override
    public void recomputeHyperplaneBias(double [] resT, double positiveTrainBias){
        int i;
        double ksumPos = 0, ksumNeg = 0;

        assert(xPos.length > 1 && xNeg.length > 1);
        clusterSigPos = clusterSigNeg = 0;

        for (i = 0; i < clustersPos.size(); i++)
            clusterSigPos += clusterSigmasPos[i];

        clusterSigPos /= (double)(xPos.length - 1);

        for (i = 0; i < clustersNeg.size(); i++)
            clusterSigNeg += clusterSigmasNeg[i];

        clusterSigNeg /= (double)(xNeg.length - 1);

        for (i = 0; i < alphas.length; i++){
            ksumPos += alphas[i] * kpos[i];
            ksumNeg += alphas[i] * kneg[i];
        }

        if (clusterSigPos < epsDouble && clusterSigNeg < epsDouble){
            offsetB = -(ksumPos + ksumNeg) / (1 + positiveTrainBias);
            resT[0] = 0.0;
        }
        else{
            offsetB = - (positiveTrainBias * clusterSigPos * ksumNeg + clusterSigNeg * ksumPos) / (positiveTrainBias * clusterSigPos + clusterSigNeg);
            resT[0] = positiveTrainBias * clusterSigPos / (ksumPos + offsetB);
        }

        ePos = ksumPos + offsetB;
        eNeg = ksumNeg + offsetB;

        recomputeSigmas();
    }

    public double computeNonClusteredObjectiveValue(double positiveTrainBias){
        int i;
        double resT = 0;

        sigPos = sigNeg = 0.0;
        for (i = 0; i < xPos.length; i++)
            sigPos += sigmas[xPos[i]];

        sigPos /= (double)(xPos.length - 1);


        for (i = 0; i < xNeg.length; i++)
            sigNeg += sigmas[xNeg[i]];

        sigNeg /= (double)(xNeg.length - 1);

        if (ePos > 0)
            resT = positiveTrainBias * sigPos / ePos;
        else if (eNeg < 0)
            resT = - sigNeg / eNeg;

        return resT;
    }

    public double computeClusteredObjectiveValue(double positiveTrainBias){
        int i;
        double clusterSigDist, resT;

        clusterSigPos = 0;
        for (i = 0; i < clustersPos.size(); i++){
            clusterSigDist = 0;
            for (int eIdx : clustersPos.get(i))
                clusterSigDist += computeSignedDistanceForIndexedEntry(eIdx);

            clusterSigDist -= ePos * clustersPos.get(i).length;

            if (clusterSigDist < 0)
                clusterSigDist = -clusterSigDist;

            clusterSigPos += clusterSigDist;
        }
        clusterSigPos /= (double)(xPos.length - 1);

        clusterSigNeg = 0;
        for (i = 0; i < clustersNeg.size(); i++){
            clusterSigDist = 0;
            for (int eIdx : clustersNeg.get(i))
                clusterSigDist += computeSignedDistanceForIndexedEntry(eIdx);

            clusterSigDist -= eNeg * clustersNeg.get(i).length;

            if (clusterSigDist < 0)
                clusterSigDist = -clusterSigDist;

            clusterSigNeg += clusterSigDist;
        }
        clusterSigNeg /= (double)(xNeg.length - 1);

        if (ePos < 1e-10 && eNeg > -1e-10)
            return 0;

        resT = 0.0;

        if (ePos >= 1e-10)
            resT = positiveTrainBias * clusterSigPos / ePos;

        if (eNeg <= -1e-10){
            if (resT < - clusterSigNeg / eNeg)
                resT = - clusterSigNeg / eNeg;
        }

        return resT;
    }

    public PvmClusterDataCore(PvmClusterDataCore other){
        from(other);

        clustersPos = new ArrayList<int[]>(other.clustersPos.size());
        for (int []cluster : other.clustersPos)
            clustersPos.add(cluster.clone());

        clustersNeg = new ArrayList<int[]>(other.clustersNeg.size());
        for (int []cluster : other.clustersNeg)
            clustersNeg.add(cluster.clone());

        clustersCount = clustersPos.size() + clustersNeg.size();
        clustersTotal = new ArrayList<int[]>(clustersCount);
        clustersTotal.addAll(clustersPos);
        clustersTotal.addAll(clustersNeg);

        clusterSigmasPos = other.clusterSigmasPos.clone();
        clusterSigmasNeg = other.clusterSigmasNeg.clone();
        clusterSigPos = other.clusterSigPos;
        clusterSigNeg = other.clusterSigNeg;
    }

    public PvmClusterDataCore(PvmDataCore other){
        from(other);
    }

    public PvmClusterDataCore(){

    }

    @Override
    public PvmDataCore mergeCores(ArrayList<PvmDataCore> srcCores){
        PvmDataCore temp = super.mergeCores(srcCores);
        PvmClusterDataCore ret = new PvmClusterDataCore(temp);
        ret.Init();
        ret.inheritClusteringFrom(this);
        return ret;
    }

    protected void inheritClusteringFrom(PvmClusterDataCore src){
        int[] cCluster;

        if (clustersPos == null || clustersNeg == null || clustersTotal == null)
            allocClusters();

        clustersPos.clear();
        clustersNeg.clear();

        for (int [] cluster : src.clustersPos){
            cCluster = constructInheritedCluster(src.entries, cluster);
            if (cCluster == null)
                continue;
            clustersPos.add(cCluster);
        }

        for (int [] cluster : src.clustersNeg){
            cCluster = constructInheritedCluster(src.entries, cluster);
            if (cCluster == null)
                continue;
            clustersNeg.add(cCluster);
        }

        buildClustersTotal();
    }

    protected int [] constructInheritedCluster(ArrayList<PvmEntry> srcEntries, int srcCluster[]){
        int i, idx;
        ArrayList<Integer> tempCluster = new ArrayList<Integer>(1);

        tempCluster.clear();
        for (i = 0; i < srcCluster.length; i++){
            idx = entries.indexOf(srcEntries.get(srcCluster[i]));
            if (idx < 0)
                continue;

            tempCluster.add(idx);
        }

        if (tempCluster.size() == 0)
            return null;

        int [] ncluster = new int[tempCluster.size()];
        for (i = 0; i < tempCluster.size(); i++)
            ncluster[i] = tempCluster.get(i);

        return ncluster;
    }

    public void from(PvmDataCore other){
        entries = other.entries;
        xPos = other.xPos;
        xNeg = other.xNeg;
        gramMtx = other.gramMtx;
        kpos = other.kpos;
        kneg = other.kneg;
        alphas = other.alphas;
        sigmas = other.sigmas;
        offsetB = other.offsetB;
        ePos = other.ePos;
        eNeg = other.eNeg;
        sigPos = other.sigPos;
        sigNeg = other.sigNeg;
    }

    public Object clone() throws CloneNotSupportedException{
        return new PvmClusterDataCore(this);
    }

    protected double computeClusterSigmaSum(int clusterIdx, boolean positiveLabel){
        double sigmaSum = 0;
        int cluster[];

        if (positiveLabel)
            cluster = clustersPos.get(clusterIdx);
        else
            cluster = clustersNeg.get(clusterIdx);

        for (int i : cluster)
            sigmaSum += sigmas[i];

        return sigmaSum;
    }

    protected double getClusterSigma(int clusterIdx, boolean positiveLabel){
        if (positiveLabel)
            return clusterSigmasPos[clusterIdx];
        return clusterSigmasNeg[clusterIdx];
    }

    public double computeClusterRelativeInducedAberration(int clusterIdx, boolean positiveLabel){
        double sigmaSum = computeClusterSigmaSum(clusterIdx, positiveLabel);
        double sigmaCluster = getClusterSigma(clusterIdx, positiveLabel);

        if (sigmaSum > 1e-12)
            return 1.0 - (sigmaCluster / sigmaSum);
        else
            return 0.0;
    }

    public double computeClusterAbsoluteInducedAberration(int clusterIdx, boolean positiveLabel){
        double sigmaSum = computeClusterSigmaSum(clusterIdx, positiveLabel);
        double sigmaCluster = getClusterSigma(clusterIdx, positiveLabel);

        return sigmaSum - sigmaCluster;
    }

    public boolean splitClustersDescendingAccordingToAberration(double percentOfMaximaSplitThresh){
        boolean splitLeastOne = false;
        int i, originalClusterCount = clustersTotal.size();
        double aberrationsPos[], aberrationsNeg[], thresh = 0;

        aberrationsPos = new double[clustersPos.size()];
        aberrationsNeg = new double[clustersNeg.size()];

        for (i = 0; i < aberrationsPos.length; i++){
            aberrationsPos[i] = computeClusterAbsoluteInducedAberration(i, true);
            if (thresh < aberrationsPos[i])
                thresh = aberrationsPos[i];
        }

        for (i = 0; i < aberrationsNeg.length; i++){
            aberrationsNeg[i] = computeClusterAbsoluteInducedAberration(i, false);
            if (thresh < aberrationsNeg[i])
                thresh = aberrationsNeg[i];
        }

        thresh *= percentOfMaximaSplitThresh;
        if (thresh < 1e-10)
            thresh = 1e-10;

        for (i = 0; i < aberrationsPos.length; i++)
            if (aberrationsPos[i] >= thresh){
                splitLeastOne = true;
                splitCluster(i, true);
            }

        for (i = 0; i < aberrationsNeg.length; i++)
            if (aberrationsNeg[i] >= thresh){
                splitLeastOne = true;
                splitCluster(i, false);
            }

        buildClustersTotal();

        return splitLeastOne;
    }


    public boolean splitClustersWithAberrationOverThreshold(double thresh){
        int i, originalClusterCount = clustersTotal.size();
        double aberration;


        for (i = clustersPos.size() - 1; i >= 0; i--){
            aberration = computeClusterRelativeInducedAberration(i, true);
            if (aberration > thresh)
                splitCluster(i, true);
        }

        for (i = clustersNeg.size() - 1; i >= 0; i--){
            aberration = computeClusterRelativeInducedAberration(i, false);
            if (aberration > thresh)
                splitCluster(i, false);
        }

        buildClustersTotal();

        return originalClusterCount != clustersTotal.size();
    }

    private void splitCluster(int clusterIdx, boolean positiveLabel){
        int i, movedCount = 0;
        int cluster[];
        boolean moved[];
        double avgSide, usedAvg;
        ArrayList<int[]> clustersSrc;

        if (positiveLabel){
            clustersSrc = clustersPos;
            usedAvg = ePos;
        }
        else{
            clustersSrc = clustersNeg;
            usedAvg = eNeg;
        }

        cluster = clustersSrc.get(clusterIdx);
        moved = new boolean[cluster.length];

        for (i = 0; i < cluster.length; i++){
            avgSide = computeSignedDistanceForIndexedEntry(cluster[i]);
            avgSide -= usedAvg;

            if (avgSide >= 0)
                moved[i] = false;
            else{
                movedCount++;
                moved[i] = true;
            }
        }

        if (movedCount == 0 || movedCount == cluster.length)
            return;

        int clusterPos[] = new int[cluster.length - movedCount], posIdx = 0;
        int clusterNeg[] = new int[movedCount], negIdx = 0;

        for (i = 0; i < cluster.length; i++){
            if (moved[i]){
                clusterNeg[negIdx] = cluster[i];
                negIdx++;
            }
            else {
                clusterPos[posIdx] = cluster[i];
                posIdx++;
            }
        }

        clustersSrc.set(clusterIdx, clusterPos);
        clustersSrc.add(clusterNeg);
    }

    @Override
    public ArrayList<PvmDataCore> splitRandomIntoSlices(int sliceCount){
        ArrayList<PvmDataCore> temp = super.splitRandomIntoSlices(sliceCount);
        ArrayList<PvmDataCore> ret = new ArrayList<PvmDataCore>(temp.size());

        for (PvmDataCore simpleCore : temp)
            ret.add(new PvmClusterDataCore(simpleCore));

        return ret;
    }

    @Override
    public void Init(){
        super.Init();
        allocClusters();
        buildDefaultClusters();
    }
}
