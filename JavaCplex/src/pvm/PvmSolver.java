package pvm;

import dsolve.LocalSolver;
import dsolve.SolverHelper;
import ilog.concert.IloException;
import org.apache.commons.lang3.mutable.MutableDouble;
import pvm.KernelProducts.KernelProductManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/25/12
 * Time: 8:31 PM
 */

public class PvmSolver {
    public PvmDataCore core;
    public PvmSystem pvmSys;
    public static int MAX_POS_TRAIN_BIAS = 5;
    public static int MAX_BEST_POSITIONS = 5;
	public static double NOISE_DIST_INSTDEV_FROM_HP = 1.0;

    public PvmSolver(){
	    core = new PvmDataCore();
        pvmSys = new PvmSystem();
    }
    public boolean Train() {
        double t_l = 0.0, t_r = 1.0, t_c;
        core.Init();

        pvmSys.BuildSystemFor(core, 1.0, false, false);

        while (!pvmSys.Solve() && t_r < 1e+10) {
            t_l = t_r;
            t_r = 2 * t_r + 1;

            pvmSys.AddFinalTConstraints(t_r);
        }

        if (t_r >= 1e+10)
            return false;

        while (t_r - t_l > 1e-8) {
            t_c = (t_l + t_r) / 2.0;

            pvmSys.AddFinalTConstraints(t_c);
            if (pvmSys.Solve())
                t_r = t_c;
            else
                t_l = t_c;
        }

        double [] resT = new double[1];
        resT[0] = t_r;
        if (!core.CheckResult(resT, true, true))
            return false;

        return true;
    }

    public boolean TrainDistributed() throws IOException, IloException, LocalSolver.LocalSolverInputException {
        double t_l = 0.0, t_r = 1.0, t_c;
        core.Init();

        pvmSys.BuildSystemFor(core, 1.0, false, false);

        while (!pvmSys.solveDistributed() && t_r < 1e+10) {
            t_l = t_r;
            t_r = 2 * t_r + 1;

            pvmSys.replaceFinalTConstraintsDistributed( t_r );
        }

        if (t_r >= 1e+10)
            return false;

        while (t_r - t_l > 1e-8)
        {
            t_c = (t_l + t_r) / 2.0;

            pvmSys.replaceFinalTConstraintsDistributed( t_c );
            if (pvmSys.solveDistributed())
                t_r = t_c;
            else
                t_l = t_c;
        }

        double [] resT = new double[1];
        resT[0] = t_r;
        if (!core.CheckResult(resT, true, true))
            return false;

        return true;
    }

    public boolean TrainSingleLP()throws IloException {
        boolean ret;
        double [] resT = new double[1];

        core.Init();
        pvmSys.buildSingleLPSystem(core, false, false, false);
        ret = pvmSys.solveSingleLP(resT);

        if (ret && resT[0] == 0.0)
        {
            pvmSys.buildSecondaryLpSystem(core);
            ret = pvmSys.solveSingleLPSecondary(resT);
            //ret = pvmSys.solveSingleLP(resT);
        }

        //assert(core.CheckResult(resT, false, true));
        return ret;
    }

    public boolean TrainSingleLPWithBias(double positiveBias) throws IloException {
        boolean ret;
        double [] resT = new double[1];

        core.Init();
        pvmSys.buildSingleLPSystemWithBias( core, positiveBias );

        ret = pvmSys.solveSingleLPWithBias( resT, positiveBias );

	    // the case where stdev is 0
        if (ret && resT[0] == 0.0)
        {
            pvmSys.buildSecondaryLpSystem(core);
            ret = pvmSys.solveSingleLPSecondary(resT, positiveBias);
        }

        //assert(core.CheckResult(resT, false, false));
        return ret;
    }

	public boolean TrainSingleLPWithBiasAndNoiseElimination( double positiveBias ) throws IloException {
		boolean retVal;
		double [] resT = new double[1];

		core.Init();
		pvmSys.buildSingleLPSystemWithBias( core, positiveBias );
		retVal = pvmSys.solveSingleLPWithBias( resT, positiveBias );

		// the case where stdev is 0
		if ( retVal && resT[0] == 0.0 ) {
			pvmSys.buildSecondaryLpSystem( core );
			retVal = pvmSys.solveSingleLPSecondary( resT, positiveBias );
			return retVal;
		}

		// the case where we try to eliminate noisy records

		List<Map.Entry<Double, Integer>> recDist2Hp = core.getNormalizedSignedDistancesWithIndexPos();
		recDist2Hp.addAll( core.getNormalizedSignedDistancesWithIndexNeg() );

		ArrayList<PvmEntry> toRemove = new ArrayList<PvmEntry>();
		for ( Map.Entry<Double, Integer> entry : recDist2Hp ) {
			double   distance = entry.getKey();
			int      recordIndex = entry.getValue();

			PvmEntry subjectRecord = core.entries.get( recordIndex );
			if ( ( Math.abs( distance ) > NOISE_DIST_INSTDEV_FROM_HP ) &&
				 ( (subjectRecord.label && distance < 0) || (!subjectRecord.label && distance > 0) ) ) {

				System.out.printf( "th: %d -> removing record with dist: %.04f / index: %d / pos: %s\n",
					Thread.currentThread().getId(),
					distance, recordIndex, subjectRecord.label );

				toRemove.add( core.entries.get( recordIndex ) );
			}
		}

		if ( toRemove.size() == 0 ) { return retVal; }

		// remove the unwanted records/entries
		core.entries.removeAll( toRemove );

		// rebuild gram matrix.. etc
		core.Init();
		pvmSys.buildSingleLPSystemWithBias( core, positiveBias );
		retVal = pvmSys.solveSingleLPWithBias( resT, positiveBias );

		// the case where stdev is 0
		if ( retVal && resT[0] == 0.0 ) {
			pvmSys.buildSecondaryLpSystem( core );
			retVal = pvmSys.solveSingleLPSecondary( resT, positiveBias );
			return retVal;
		}

		return retVal;
	}


	public boolean classify(PvmEntry src){
        return core.classify(src);
    }

    public boolean [] classify(ArrayList<PvmEntry> entries){
        int i;
        boolean retLabels[] = new boolean[entries.size()];


        for (i = 0; i < retLabels.length; i++){
            retLabels[i] = classify(entries.get(i));
        }

        return retLabels;
    }

    public static void computeAccuracy( boolean labels[], ArrayList<PvmEntry> entries, double [] accuracy, double [] sensitivity, double [] specificity, int split ){
        int i;
        int tp = 0, tn = 0, fp = 0, fn = 0;

        assert (labels.length == entries.size());

        for (i = 0; i < labels.length; i++)
        {
            if (entries.get(i).label)
            {
                if (labels[i])
                    tp++;
                else
                    fn++;
            }
            else
            {
                if (labels[i])
                    fp++;
                else
                    tn++;
            }
        }

        assert (specificity.length > 0 && sensitivity.length > 0 && accuracy.length > 0);

        if (fn + tp > 0)
            sensitivity[split] = (double)tp / (double)(fn + tp);

        if (fp + tn > 0)
            specificity[split] = (double)tn / (double)(fp + tn);

        accuracy[split] = (double)(tp + tn) / (double)(entries.size());
    }

    public void performCrossFoldValidationWithBias(int splitCount, double positiveBias, boolean solvedFlags[], double accuracy[], double sensitivity[], double specificity[]) throws IloException {

	    core.Init( false );
        ArrayList<PvmDataCore> splitCores = core.splitRandomIntoSlices( splitCount );

        assert( splitCount == splitCores.size() );
        assert( accuracy.length > 0 && sensitivity.length > 0 && specificity.length > 0 );

	    ExecutorService executorService = Executors.newFixedThreadPool( splitCount );

	    // spawn a thread pool and add each fold as a task
        for ( int i = 0; i<splitCount; i++ ) {

	        accuracy[i]    = 0.0;
	        sensitivity[i] = 0.0;
	        specificity[i] = 0.0;
	        solvedFlags[i] = false;

	        SolverFoldRunnable foldRunnable = new SolverFoldRunnable( splitCores, i, positiveBias );
	        foldRunnable.setResultVectors( accuracy, sensitivity, specificity, solvedFlags );

	        executorService.submit( foldRunnable );
        }

	    try {
		    synchronized ( executorService ) {
			    executorService.shutdown();
		        executorService.awaitTermination( 24, TimeUnit.HOURS );
		    }
	    } catch ( InterruptedException e ) {
		    e.printStackTrace();
	    }
    }

    public void performCrossFoldValidation(int splitCount, boolean solvedFlags[], double accuracy[], double sensitivity[], double specificity[]) throws IloException {
        performCrossFoldValidationWithBias(splitCount, 1.0, solvedFlags, accuracy, sensitivity, specificity);
    }

    public void writeNormalizedDistancesToFiles(String fileNamePos, String fileNameNeg) throws IloException, IOException {
        int i;
        if (!TrainSingleLP())
            return;

        double distancesPos[] = core.getNormalizedSignedDistancesPos();
        FileWriter fw = new FileWriter(new File(fileNamePos));

        for (i = 0; i < distancesPos.length; i++)
            fw.write(String.valueOf(distancesPos[i]) + "\n");

        fw.close();


        double distancesNeg[] = core.getNormalizedSignedDistancesNeg();
        fw = new FileWriter(new File(fileNameNeg));

        for (i = 0; i < distancesNeg.length; i++)
            fw.write(String.valueOf(distancesNeg[i]) + "\n");

        fw.close();
    }

    public double searchPositiveTrainBias( int splitCount, MutableDouble accuracy, MutableDouble sensitivity, MutableDouble specificity ) throws IloException {
        int i;
        double cPow, maxCPow = 0, bestTrainBias = 1.0, trainBias;
        double bestAcc = 0.0;

        for (i = -MAX_POS_TRAIN_BIAS; i <= MAX_POS_TRAIN_BIAS; i++){

	        double meanAcc = 0, meanSens = 0, meanSpec = 0;
            trainBias = Math.pow(2, (double)i);

	        System.out.printf( "\t\tBIASPOW:2^%2d", i ); long start = System.currentTimeMillis();

	        double solvesCount = 0;
	        for ( int k=0; k<5; k++ ) {

		        double foldAcc[] = new double[splitCount], foldSens[] = new double[splitCount], foldSpec[] = new double[splitCount];
		        boolean foldSolvedFlags[] = new boolean[splitCount];

	            performCrossFoldValidationWithBias( splitCount, trainBias, foldSolvedFlags, foldAcc, foldSens, foldSpec );
		        for ( int j=0; j<splitCount; j++ ) {
			        if ( foldSolvedFlags[j] ) {
				        solvesCount++;
				        meanAcc  += foldAcc[j];
				        meanSens += foldSens[j];
				        meanSpec += foldSpec[j];
			        }
		        }
	        }

	        if ( solvesCount > 0 ) {
		        meanAcc  /= solvesCount;
		        meanSens /= solvesCount;
		        meanSpec /= solvesCount;
	        }

	        System.out.printf(
		        "/ACC:%.05f/SENS:%.05f/SPEC:%.05f/TIME:%d\n",
				meanAcc,meanSens,meanSpec, (System.currentTimeMillis()-start)/1000
	        );

            if ( bestAcc < meanAcc ) {
                bestAcc = meanAcc;
                bestTrainBias = trainBias;
                maxCPow = i;

                accuracy.setValue( meanAcc );
                sensitivity.setValue( meanSens );
                specificity.setValue( meanSpec );
            }
        }

        maxCPow += 1;
        for ( cPow = maxCPow - 2; cPow < maxCPow; cPow += 0.2 ) {

	        double meanAcc = 0, meanSens = 0, meanSpec = 0;

            trainBias = Math.pow( 2, cPow );

            System.out.print( String.format( "\t\tBIASPOW:2^%.2f", cPow ) ); long start = System.currentTimeMillis();

	        double solvesCount = 0;
	        for ( int k=0; k<3; k++ ) {
		        boolean foldSolvedFlags[] = new boolean[splitCount];
		        double foldAcc[] = new double[splitCount], foldSens[] = new double[splitCount], foldSpec[] = new double[splitCount];

		        performCrossFoldValidationWithBias( splitCount, trainBias, foldSolvedFlags, foldAcc, foldSens, foldSpec );

		        for ( int j=0; j<splitCount; j++ ) {

			        if ( foldSolvedFlags[j] ) {
				        solvesCount++;
				        meanAcc  += foldAcc[j];
				        meanSens += foldSens[j];
				        meanSpec += foldSpec[j];
			        }
		        }
	        }

	        if ( solvesCount > 0 ) {
		        meanAcc  /= solvesCount;
		        meanSens /= solvesCount;
		        meanSpec /= solvesCount;
	        }

	        System.out.printf(
		        "/ACC:%.05f/SENS:%.05f/SPEC:%.05f/TIME:%d\n",
		        meanAcc, meanSens, meanSpec, ( System.currentTimeMillis() - start ) / 1000
	        );

	        if ( bestAcc < meanAcc ) {
		        bestAcc = meanAcc;
		        bestTrainBias = trainBias;

		        accuracy.setValue( meanAcc );
		        sensitivity.setValue( meanSens );
		        specificity.setValue( meanSpec );
	        }
        }

        return bestTrainBias;
    }

    private void addLocalTrainParameters(
		    int splitCount,
		    int refinementLvl,
		    PvmTrainParameters tempParams,
		    ArrayList<PvmTrainParameters> bestTrainParams)
		    throws IloException, CloneNotSupportedException {

        MutableDouble tempAccuracy = new MutableDouble(0), tempSensitivity = new MutableDouble(0), tempSpecificity = new MutableDouble(0);
        double boundDLow, boundDHigh;
        int boundILow, boundIHigh;
        int paramIntItMax, paramDoubleItMax;
        int itParamInt, itParamDouble;

        KernelProductManager.setKernelTypeGlobal(tempParams.kerType);
        KernelProductManager.setRefinementLevel(refinementLvl);

        boundILow = KernelProductManager.getMinParamI(tempParams.paramInt);
        boundIHigh = KernelProductManager.getMaxParamI(tempParams.paramInt);
        paramIntItMax = KernelProductManager.getParamIntMaxStepsCount();

        boundDLow = KernelProductManager.getMinParamD();
        boundDHigh = KernelProductManager.getMaxParamD();
        paramDoubleItMax = KernelProductManager.getParamDoubleMaxStepsCount();

        for (itParamInt = 0; itParamInt < paramIntItMax; itParamInt++){
            tempParams.paramInt = KernelProductManager.getParamIValue(boundILow, boundIHigh, itParamInt, paramIntItMax);
            KernelProductManager.setParamInt(tempParams.paramInt);

            for (itParamDouble = 0; itParamDouble < paramDoubleItMax; itParamDouble++){

                tempParams.paramDouble = KernelProductManager.getParamDValue(boundDLow, boundDHigh, itParamDouble, paramDoubleItMax);
                KernelProductManager.setParamDouble(tempParams.paramDouble);

	            System.out.println( "\tEVALUATING->" + tempParams );
	            long start = System.currentTimeMillis();

	            // do the actual search
                tempParams.trainBias = searchPositiveTrainBias(splitCount, tempAccuracy, tempSensitivity, tempSpecificity);

	            System.out.printf(
		            "\tEVALUATED ->%s/ACC:%.05f/SENS:%.05f/SPEC:%.05f/TIME:%d\n",
		            tempParams,tempAccuracy.doubleValue(),tempSensitivity.doubleValue(),tempSpecificity.doubleValue(),(System.currentTimeMillis()-start)/1000
	            );

                tempParams.accuracy = tempAccuracy.doubleValue();
                tempParams.sensitivity = tempSensitivity.doubleValue();
                tempParams.specificity = tempSpecificity.doubleValue();

                bestTrainParams.add((PvmTrainParameters)tempParams.clone());
            }
        }
    }

    public void searchTrainParameters(int splitCount, KernelProductManager.KerType[] kernelTypes, PvmTrainParameters trainParameters, MutableDouble accuracy, MutableDouble sensitivity, MutableDouble specificity) throws IloException, CloneNotSupportedException {
        PvmTrainParameters tempParams = new PvmTrainParameters();
        int refinementLvl = 1;
        ArrayList<PvmTrainParameters> bestTrainParams = new ArrayList<PvmTrainParameters>();
		long start;

        for (KernelProductManager.KerType kerType : kernelTypes){
            //tempParams.kerType = KernelProductManager.KerType.KERSCALAR;
            tempParams.kerType = kerType;
            KernelProductManager.setKernelTypeGlobal(tempParams.kerType);
            tempParams.paramInt = KernelProductManager.getPreferedCenterInt();
            tempParams.paramDouble = KernelProductManager.getPreferedCenterDouble();

	        System.out.println( "EVALKERNEL: " + tempParams.kerType );
	        start = System.currentTimeMillis();

	        addLocalTrainParameters(splitCount, refinementLvl, tempParams, bestTrainParams);

	        System.out.println( "EVALKERNEL: " + tempParams.kerType + "/TIME:" + String.valueOf( (System.currentTimeMillis()-start)/1000 ) );
        }

        Collections.sort(bestTrainParams, tempParams);
        while (bestTrainParams.size() > MAX_BEST_POSITIONS )
            bestTrainParams.remove(bestTrainParams.size() - 1);

	    System.out.println( "Found best train params: " );
	    for ( PvmTrainParameters params : bestTrainParams ) {
		    System.out.println( "\t" + params.toCompleteString() );
	    }
	    System.out.println( "making refinements now...." );

        for (refinementLvl = 2; refinementLvl < 4; refinementLvl++){
            for (int i = bestTrainParams.size() - 1; i > 0; i--) {
	            System.out.print( "EVAL/" + tempParams );
	            start = System.currentTimeMillis();
                addLocalTrainParameters(splitCount, refinementLvl, bestTrainParams.get(i), bestTrainParams);
	            System.out.println( "/TIME:" + (System.currentTimeMillis()-start)*1000 );
            }

            Collections.sort(bestTrainParams, tempParams);
            while (bestTrainParams.size() > MAX_BEST_POSITIONS )
                bestTrainParams.remove(bestTrainParams.size() - 1);
        }

	    System.out.println( "Found final best train params: " );
	    for ( PvmTrainParameters params : bestTrainParams ) {
		    System.out.println( "\t" + params.toCompleteString() );
	    }

	    // copy the values rather than assign a new value to the original pointer
        trainParameters.copyFrom( bestTrainParams.get( 0 ) );

        KernelProductManager.setKernelTypeGlobal(trainParameters.kerType);
        KernelProductManager.setRefinementLevel( 1 );
        KernelProductManager.setParamInt(trainParameters.paramInt);
        KernelProductManager.setParamDouble(trainParameters.paramDouble);
    }

	private static class SolverFoldRunnable implements Runnable {

		ArrayList<PvmDataCore> splitCores;

		ArrayList<PvmDataCore> trainCores;
		PvmDataCore testCore;

		double accuracy[];
		double sensitivity[];
		double specificity[];
		boolean solvedFlags[];

		double positiveBias = 0;
		int    split = -1;

		public SolverFoldRunnable( ArrayList<PvmDataCore> splitCores, int split, double positiveBias ) throws IloException {

			this.splitCores = splitCores;
			this.positiveBias = positiveBias;
			this.split = split;
		}

		public void setResultVectors( double accuracy[], double sensitivity[], double specificity[], boolean solvedFlags[] ) {
			this.accuracy    = accuracy;
			this.sensitivity = sensitivity;
			this.specificity = specificity;
			this.solvedFlags = solvedFlags;
		}

		@Override
		public void run() {

			// add data to local storage ( just pointers, no data )
			trainCores = new ArrayList<PvmDataCore>( splitCores.size() );
			trainCores.addAll( splitCores );

			testCore = trainCores.get( split );
			trainCores.remove( split );

			PvmSolver localSlv = new PvmSolver();
			localSlv.core = PvmDataCore.mergeCores( trainCores );

			try {
				solvedFlags[split] = true;

				if ( !localSlv.TrainSingleLPWithBias( positiveBias ) ) {
				//if ( !localSlv.TrainSingleLPWithBiasAndNoiseElimination( positiveBias ) ) {
					solvedFlags[split] = false;
					return;
				}
				boolean localLabels[] = localSlv.classify( testCore.entries );
				PvmSolver.computeAccuracy( localLabels, testCore.entries, accuracy, sensitivity, specificity, split );

			} catch ( IloException e ) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args ) throws IOException, IloException, LocalSolver.LocalSolverInputException, CloneNotSupportedException {

        if (args.length < 1) return;

	    try { SolverHelper.dropNativeCplex(); } catch ( URISyntaxException ignored ) {}

	    PvmSolver solver = new PvmSolver();
        PvmTrainParameters bestTrainParams = new PvmTrainParameters();
        MutableDouble acc = new MutableDouble(0), sens = new MutableDouble(0), spec = new MutableDouble(0);

        solver.core.ReadFile( args[0] );

		KernelProductManager.KerType[] kernelTypes = new KernelProductManager.KerType[2];
		kernelTypes[0] = KernelProductManager.KerType.KERSCALAR;
		kernelTypes[1] = KernelProductManager.KerType.KERRBF;

        solver.searchTrainParameters( 10, kernelTypes, bestTrainParams, acc, sens, spec );
    }
}
