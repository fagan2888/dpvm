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
public class PvmClusterDataCore extends PvmDataCore {
    ArrayList<int[]> clustersPos = null, clustersNeg = null;
    ArrayList<int[]> clustersTotal = null;
    int clustersCount = 0;

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
}
