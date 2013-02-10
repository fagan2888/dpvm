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
    public static double relativeObjectiveThresh = 0.9999;
    public static double objectiveMinimalDifference = 1.5e-15;
    public static double relativeAberrationInitialThresh = 0.95;
    public static double relativeAberrationThreshDecayRate = 0.85;
    public static double relativeAberrationMinimalThresh = 0.01;


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
            clusterObjective = clusterCore.computeClusteredObjectiveValue(positiveBias);

            //objectiveMinimalDifference
            if (clusterObjective + objectiveMinimalDifference < nonClusteredObjective * relativeObjectiveThresh){
                while (!clusterCore.splitClustersWithAberrationOverThreshold(splitAberrationThresh) &&
                        splitAberrationThresh > relativeAberrationMinimalThresh)
                    splitAberrationThresh *= relativeAberrationThreshDecayRate;

                if (splitAberrationThresh <= relativeAberrationMinimalThresh)
                    splitAberrationThresh *= relativeAberrationThreshDecayRate;
            }
            else
                break;
        } while (true);

        return true;
    }

    @Override
    protected PvmSolver instantiateLocalSolver(){
        return new PvmClusterSolver();
    }
}
