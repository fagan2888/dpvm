package pvm.KernelProducts;

import org.apache.commons.lang3.Validate;
import pvm.PvmEntry;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/4/13
 * Time: 6:46 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class KernelProduct {

    protected KernelProductManager.KerType kerType;
    int refinementLvl = 1;

    public abstract String getName();

    public abstract double computeKerProd(double [] x0, double [] x1);
    public abstract void setParamInt(int paramInt);
    public abstract void setParamDouble(double paramDouble);

    public abstract int getPreferedCenterInt();
    public abstract double getPreferedCenterDouble();

    public abstract int getLowerBoundInt(int centerParamInt);
    public abstract int getUpperBoundInt(int centerParamInt);

    public abstract double getLowerBoundDouble(double centerParamDouble);
    public abstract double getUpperBoundDouble(double centerParamDouble);

    public abstract int getMaxStepsInt();
    public abstract int getMaxStepsDouble();

    public abstract int getParamValueInt(int lowerBound, int upperBound, int maxSteps, int stepIndex);
    public abstract double getParamValueDouble(double lowerBound, double upperBound, int maxSteps, int stepIndex);

    public void setRefinementLevel(int level){
        Validate.isTrue(level > 0, "The refinement level should always be positive");
        refinementLvl = level;
    }

    public double computeKerProd(PvmEntry e0, PvmEntry e1){
        return computeKerProd(e0.x, e1.x);
    }

    public KernelProductManager.KerType getKerType(){return kerType;}
}
