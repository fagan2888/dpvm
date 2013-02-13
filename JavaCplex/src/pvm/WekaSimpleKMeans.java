package pvm;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 2/13/13
 * Time: 9:19 PM
 * To change this template use File | Settings | File Templates.
 */

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

import java.util.ArrayList;

public class WekaSimpleKMeans {
    public static ArrayList<ArrayList<PvmEntry>> clusterElements(ArrayList<PvmEntry> entries, int clustersCount) throws Exception {
        SimpleKMeans kmeans = new SimpleKMeans();
        kmeans.setSeed(10);

        kmeans.setPreserveInstancesOrder(true);
        kmeans.setNumClusters(clustersCount);
        kmeans.buildClusterer(getWekaInstancesFromEntries(entries));

        int[] assignments = kmeans.getAssignments();

        return getClustersFromIndexes(entries, assignments);
    }

    protected static Instances getWekaInstancesFromEntries(ArrayList<PvmEntry> entries){
        int i, last_i = entries.get(0).x.length;
        if (last_i == 0)
            return null;

        ArrayList<Attribute> attributes = new ArrayList<Attribute>(last_i);
        for (i = 0; i < last_i; i++)
            attributes.add(new Attribute(Integer.toString(i)));

        Instances ret = new Instances("ClusterInstances", attributes, entries.size());

        for (i = 0; i < entries.size(); i++)
            ret.add(new DenseInstance(1.0, entries.get(i).x));

        return ret;
    }

    protected static ArrayList<ArrayList<PvmEntry>> getClustersFromIndexes(ArrayList<PvmEntry> entries, int[] assignments){
        int i, clustersCount = 0;

        if (assignments.length != entries.size())
            return null;

        for (int idx : assignments)
            if (clustersCount < idx)
                clustersCount = idx;

        clustersCount++;

        ArrayList<ArrayList<PvmEntry>> clusters = new ArrayList<ArrayList<PvmEntry>>(clustersCount);

        for (i = 0; i < clustersCount; i++)
            clusters.add(new ArrayList<PvmEntry>(1));

        for (i = 0; i < assignments.length; i++){
            clusters.get(assignments[i]).add(entries.get(i));
        }

        return clusters;
    }
}
