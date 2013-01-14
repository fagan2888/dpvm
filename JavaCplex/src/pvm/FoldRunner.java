package pvm;

import dsolve.LocalSolver;
import dsolve.SolverHelper;
import ilog.concert.IloException;
import pvm.KernelProducts.KernelProductManager;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/14/13
 * Time: 10:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class FoldRunner {

    public static boolean checkMainArgs(String[] args){
        if (args.length != 6){

            System.out.println("Insuficient parameters. Usage: ");
            System.out.println("Arg 0 : FileName");
            System.out.print("Arg 1 : KernelType :");

            for (int i = 0; i < KernelProductManager.KerType.values().length; i++){
                System.out.print(String.format(" %d <-> %s |", i, KernelProductManager.getAvailableKernels()[i].getName()));
            }
            System.out.println();

            System.out.println("Arg 2 : TrainBias");
            System.out.println("Arg 3 : ParamInt");
            System.out.println("Arg 4 : ParamDouble");
            System.out.println("Arg 5 : NumberOfRuns");

            return false;
        }

        return true;
    }


    public static void main(String[] args ) throws IOException, IloException, LocalSolver.LocalSolverInputException, CloneNotSupportedException {

        if (!checkMainArgs(args))
            return;

        try { SolverHelper.dropNativeCplex(); } catch ( URISyntaxException ignored ) {}

        PvmSolver solver = new PvmSolver();

        int runCount = Integer.valueOf(args[5]);
        int paramInt = Integer.valueOf(args[3]);
        double paramDouble = Double.valueOf(args[4]);
        double trainBias = Double.valueOf(args[2]);
        KernelProductManager.KerType kerType = KernelProductManager.getAvailableKernels()[Integer.valueOf(args[1])].getKerType();

        int i;
        solver.core.ReadFile( args[0] );
        KernelProductManager.setKernelTypeGlobal(kerType);
        KernelProductManager.setParamInt(paramInt);
        KernelProductManager.setParamDouble(paramDouble);

        double runAccs[] = new double[runCount];
        double runSens[] = new double[runCount];
        double runSpec[] = new double[runCount];

        double fAcc = 0, fSens = 0, fSpec = 0;
        double tempAcc[] = new double[1];
        double tempSens[] = new double[1];
        double tempSpec[] = new double[1];

        for (i = 0; i < runCount; i++){
            solver.performCrossFoldValidationWithBias(10, trainBias, tempAcc, tempSens, tempSpec);
            runAccs[i] = tempAcc[0];
            runSens[i] = tempSens[0];
            runSpec[i] = tempSpec[0];

            fAcc += tempAcc[0];
            fSens += tempSens[0];
            fSpec += tempSpec[0];
        }

        fAcc /= (double)runCount;
        fSens /= (double)runCount;
        fSpec /= (double)runCount;

        double devAcc = 0, devSens = 0, devSpec = 0;
        double temp;

        for (i = 0; i < runCount; i++){
            temp = runAccs[i] - fAcc;
            devAcc += temp * temp;

            temp = runSens[i] - fSens;
            devSens += temp * temp;

            temp = runSpec[i] - fSpec;
            devSpec += temp * temp;
        }

        devAcc /= (double)runCount;
        devSens /= (double)runCount;
        devSpec /= (double)runCount;

        devAcc = Math.sqrt(devAcc);
        devSens = Math.sqrt(devSens);
        devSpec = Math.sqrt(devSpec);

        System.out.println(String.format("Acc:%f Sens:%f Spec:%f", fAcc, fSens, fSpec));
        System.out.println(String.format("StdDevAcc:%f StdDevSens:%f StdDevSpec:%f", devAcc, devSens, devSpec));
    }
}
