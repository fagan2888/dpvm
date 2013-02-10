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
        clustersCount = clustersTotal.size();
        clusterSigmasPos = new double[clustersPos.size()];
        clusterSigmasNeg = new double[clustersNeg.size()];
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

        clustersTotal.clear();
        clustersTotal.addAll(clustersPos);
        clustersTotal.addAll(clustersNeg);

        clustersCount = clustersTotal.size();

        clusterSigmasPos = new double[1];
        clusterSigmasNeg = new double[1];
    }

    protected void buildClustersTotal(){
        clustersTotal.addAll(clustersPos);
        clustersTotal.addAll(clustersNeg);
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
            resT[0] = positiveTrainBias * sigPos / (ksumPos + offsetB);
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
            resT = -eNeg / eNeg;

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
        return new PvmClusterDataCore(temp);
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

    public double computeClusterRelativeInducedAberration(int clusterIdx, boolean positiveLabel){
        int cluster[];
        double sigmaSum = 0, sigmaCluster;

        if (positiveLabel){
            cluster = clustersPos.get(clusterIdx);
            sigmaCluster = clusterSigmasPos[clusterIdx];
        }
        else{
            cluster = clustersNeg.get(clusterIdx);
            sigmaCluster = clusterSigmasNeg[clusterIdx];
        }

        for (int i : cluster)
            sigmaSum += sigmas[i];

        if (sigmaSum < epsDouble)
            return 0;

        return 1.0 - (sigmaCluster / sigmaSum);
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

        clustersTotal.clear();
        clustersTotal.addAll(clustersPos);
        clustersTotal.addAll(clustersNeg);
        clustersCount = clustersTotal.size();

        if (originalClusterCount != clustersTotal.size()){
            clusterSigmasPos = new double[clustersPos.size()];
            clusterSigmasNeg = new double[clustersNeg.size()];
        }


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
