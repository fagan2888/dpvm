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

	    int splitCount = 10;

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

        double runAccs[] = new double[runCount * splitCount];
        double runSens[] = new double[runCount * splitCount];
        double runSpec[] = new double[runCount * splitCount];
	    boolean runSolvedFlags [] = new boolean[ runCount * splitCount ];

        double foldAcc[]  = new double[ splitCount ];
        double foldSens[] = new double[ splitCount ];
        double foldSpec[] = new double[ splitCount ];
	    boolean foldSolvedFlags [] = new boolean[ splitCount ];

	    // perform runs
        for (i = 0; i < runCount; i++){

            solver.performCrossFoldValidationWithBias( splitCount, trainBias, foldSolvedFlags, foldAcc, foldSens, foldSpec );

	        double foldMeanAcc = 0, foldMeanSens = 0, foldMeanSpec = 0;
	        double foldSolveCount = 0.0;
	        for ( int j=0; j<splitCount; j++ ) {
		        if ( !foldSolvedFlags[j] ) continue;
		        foldSolveCount ++;
		        foldMeanAcc += foldAcc[j];
	            foldMeanSens += foldSens[j];
	            foldMeanSpec += foldSpec[j];
	        }
	        foldMeanAcc /= foldSolveCount; foldMeanSens /= foldSolveCount; foldMeanSpec /= foldSolveCount;
	        runAccs[i] = foldMeanAcc;
	        runSens[i] = foldMeanSens;
	        runSpec[i] = foldMeanSpec;

	        System.out.printf(
		        "RUN:%03d->ACC:%.05f/SENS:%.05f/SPEC:%.05f\n",
		        i, runAccs[i], runSens[i], runSpec[i]
	        );
        }

	    // take mean of accs, sens and spec

	    double meanAcc = 0, meanSens = 0, meanSpec = 0;

	    for ( i=0; i < runCount; i++) {
		    meanAcc += runAccs[i]; meanSens += runSens[i]; meanSpec += runSpec[i];
	    }

	    meanAcc  /= runCount;
        meanSens /= runCount;
        meanSpec /= runCount;

        double devAcc = 0, devSens = 0, devSpec = 0;
        double temp;

	    // compute stddevs
        for (i = 0; i < runCount; i++){
            temp = runAccs[i] - meanAcc;
            devAcc += temp * temp;

            temp = runSens[i] - meanSens;
            devSens += temp * temp;

            temp = runSpec[i] - meanSpec;
            devSpec += temp * temp;
        }

        devAcc  /= runCount; devAcc  = Math.sqrt( devAcc  );
        devSens /= runCount; devSens = Math.sqrt( devSens );
        devSpec /= runCount; devSpec = Math.sqrt( devSpec );

	    System.out.printf(
		    "\nMEAN ->ACC:%.05f/SENS:%.05f/SPEC:%.05f\n",
		    meanAcc,meanSens,meanSpec
	    );

	    System.out.printf(
		    "STDEV->ACC:%.05f/SENS:%.05f/SPEC:%.05f\n",
		    devAcc,devSens,devSpec
	    );

    }
}
