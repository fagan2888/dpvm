package pvm;

import java.util.ArrayList;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/27/13
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class PvmClusterDataCoreTest {
    public void addEntriesToPvmClusterDataCore(PvmClusterDataCore clusterDataCore, int entriesCount, int entriesCountPositive){
        int i;
        clusterDataCore.entries = new ArrayList<PvmEntry>(entriesCount);
        for (i = 0; i < entriesCount; i++){
            PvmEntry entry = new PvmEntry();
            entry.resize(1);
            entry.label = false;
            clusterDataCore.entries.add(entry);
        }

        for (i = 0; i < entriesCountPositive; i++)
            clusterDataCore.entries.get(i).label = true;

        clusterDataCore.Init();
    }

    @Test
    public void test_splitSingleClusterAccordingToLabel_FailsOnNonDifferentiatedCluster(){
        PvmClusterDataCore clusterDataCore = new PvmClusterDataCore();
        addEntriesToPvmClusterDataCore(clusterDataCore, 2, 1);
        int cluster[] = {0};

        clusterDataCore.allocClusters();
        clusterDataCore.splitSingleClusterAccordingToLabel(cluster, 1);
        Assert.assertTrue(clusterDataCore.clustersPos.size() == 0 &&
                          clusterDataCore.clustersNeg.size() == 0);

    }

    @Test (expected = java.lang.NegativeArraySizeException.class)
    public void test_splitSingleClusterAccordingToLabel_FailsOnTooLargePositiveCount(){
        PvmClusterDataCore clusterDataCore = new PvmClusterDataCore();
        addEntriesToPvmClusterDataCore(clusterDataCore, 2, 1);
        int cluster[] = {0};

        clusterDataCore.allocClusters();
        clusterDataCore.splitSingleClusterAccordingToLabel(cluster, 2);
    }

    @Test
    public void test_splitSingleClusterAccordingToLabel_splitsDifferentiatedCluster(){
        PvmClusterDataCore clusterDataCore = new PvmClusterDataCore();
        addEntriesToPvmClusterDataCore(clusterDataCore, 2, 1);
        int cluster[] = {0, 1};

        clusterDataCore.allocClusters();
        clusterDataCore.splitSingleClusterAccordingToLabel(cluster, 1);
        Assert.assertTrue(clusterDataCore.clustersPos.size() == 1 &&
                clusterDataCore.clustersNeg.size() == 1);
        Assert.assertTrue(clusterDataCore.clustersPos.get(0).length == 1 &&
                clusterDataCore.clustersPos.get(0)[0] == 0);
        Assert.assertTrue(clusterDataCore.clustersNeg.get(0).length == 1 &&
                clusterDataCore.clustersNeg.get(0)[0] == 1);
    }

    @Test
    public void test_constructInheritedCluster_ReturnsNullWhenNoEntriesReplicate(){
        PvmClusterDataCore core = new PvmClusterDataCore(), tempCore = new PvmClusterDataCore();
        addEntriesToPvmClusterDataCore(core, 4, 2);

        tempCore.entries = new ArrayList<PvmEntry>(core.xNeg.length);
        for (int i : core.xNeg)
            tempCore.entries.add(core.entries.get(i));

        int resCluster[] = tempCore.constructInheritedCluster(core.entries, core.xPos);
        Assert.assertTrue(resCluster == null);
    }

    @Test
    public void test_constructInheritedCluster_ReturnsOnlyEntriesIdxsStillExistent(){
        PvmClusterDataCore core = new PvmClusterDataCore(), tempCore = new PvmClusterDataCore();
        addEntriesToPvmClusterDataCore(core, 4, 2);

        tempCore.entries = new ArrayList<PvmEntry>(core.xNeg.length);
        tempCore.entries.add(core.entries.get(core.xPos[0]));

        int []res = tempCore.constructInheritedCluster(core.entries, core.xPos);

        Assert.assertTrue(res != null);
        Assert.assertTrue(res.length == 1 && res[0] == 0);
    }
}
