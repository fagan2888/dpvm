package pvm;

import dsolve.LocalSolver;
import ilog.concert.IloException;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/25/12
 * Time: 8:31 PM
 */

public class PvmSolver {
    public PvmDataCore core;
    public PvmSystem pvmSys;

    public PvmSolver(){
        core = new PvmDataCore();
        pvmSys = new PvmSystem();
    }
    public boolean Train(){
        double t_l = 0.0, t_r = 1.0, t_c;
        core.Init();

        pvmSys.BuildSystemFor(core, 1.0, false, false);

        while (!pvmSys.Solve() && t_r < 1e+10)
        {
            t_l = t_r;
            t_r = 2 * t_r + 1;

            pvmSys.AddFinalTConstraints(t_r);
        };

        if (t_r >= 1e+10)
            return false;

        while (t_r - t_l > 1e-8)
        {
            t_c = (t_l + t_r) / 2.0;

            pvmSys.AddFinalTConstraints(t_c);
            if (pvmSys.Solve())
                t_r = t_c;
            else
                t_l = t_c;
        }

        double [] resT = new double[1];
        resT[0] = t_r;
        if (!core.CheckResult(resT))
            return false;

        return true;
    }

    public boolean TrainDistributed() throws IOException, IloException, LocalSolver.LocalSolverInputException {
        double t_l = 0.0, t_r = 1.0, t_c;
        core.Init();

        pvmSys.BuildSystemFor(core, 1.0, false, false);

        while (!pvmSys.solveDistributed() && t_r < 1e+10)
        {
            t_l = t_r;
            t_r = 2 * t_r + 1;

            pvmSys.replaceFinalTConstraintsDistributed( t_r );
        };

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
        if (!core.CheckResult(resT))
            return false;

        return true;
    }

    public static void main(String[] args ) throws IOException, IloException, LocalSolver.LocalSolverInputException {

        if (args.length < 1)
            return;

        PvmSolver solver = new PvmSolver();

        /*KerProduct.kerType = KerProduct.KerType.KERRBF;
        KerProduct.paramD = 1.0/32.0;
        KerProduct.paramI = 0;
          */
        solver.core.ReadFile(args[0]);
        //solver.Train();
        solver.TrainDistributed();
    }
}
