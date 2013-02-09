package pvm;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/27/13
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class PvmClusterSolver extends PvmSolver{
    PvmClusterDataCore clusterCore;
    PvmClusterSystem pvmClusterSys;
    public static double relativeObjectiveThresh = 0.97;
    public static double relativeAberrationInitialThresh = 0.95;
    public static double relativeAberrationThreshDecayRate = 0.05;

    public PvmClusterSolver(){
        core = clusterCore = new PvmClusterDataCore();
        pvmSys = pvmClusterSys = new PvmClusterSystem();
    }

    @Override
    public boolean TrainSingleLPWithBias(double positiveBias) throws IloException {
        double splitAberrationThresh = relativeAberrationInitialThresh;
        double relativeObjective;
        double clusterObjective, nonClusteredObjective;

        if (core.getClass() == PvmClusterDataCore.class)
            clusterCore = (PvmClusterDataCore)core;

        clusterCore.Init();

        do{
            double [] resT = new double[1];
            pvmClusterSys.buildSingleLPSystemWithBias(clusterCore, positiveBias);
            if (!pvmClusterSys.solveSingleLPWithBias(resT, positiveBias))
                return false;

            nonClusteredObjective = clusterCore.computeNonClusteredObjectiveValue(positiveBias);
            clusterObjective = resT[0];

            relativeObjective = 1.0;
            if (nonClusteredObjective > 0)
                relativeObjective = clusterObjective / nonClusteredObjective;

            if (relativeObjective < relativeObjectiveThresh && splitAberrationThresh > 1e-10){
                while (!clusterCore.splitClustersWithAberrationOverThreshold(splitAberrationThresh))
                    splitAberrationThresh *= relativeAberrationThreshDecayRate;
            }
        } while (relativeObjective < relativeObjectiveThresh);

        return true;
    }

    @Override
    protected PvmSolver instantiateLocalSolver(){
        return new PvmClusterSolver();
    }
}
