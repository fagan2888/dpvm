package pvm.KernelProducts;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/4/13
 * Time: 7:03 PM
 */
public class KernelProductScalar extends KernelProduct{

    double scale = 1.0;

    public KernelProductScalar(){kerType = KernelProductManager.KerType.KERSCALAR;}

    public String getName(){return "KERSCALAR";};
    public double computeKerProd(double [] x0, double [] x1){
        double ret = 0;
        int i, last_i = x0.length;
        if (last_i > x1.length)
            last_i = x1.length;

        for (i = 0; i < last_i; i++)
            ret += x0[i] * x1[i];

        return scale * ret;
    }

    public void setParamInt(int paramInt){};
    public void setParamDouble(double paramDouble){scale = paramDouble;}

    public int getPreferedCenterInt(){return 0;}

    public double getPreferedCenterDouble(){return 1.0;}

    public int getLowerBoundInt(int centerParamInt){return 0;}
    public int getUpperBoundInt(int centerParamInt){return 0;}

    public int getMaxStepsInt(){return 1;}
    public int getMaxStepsDouble(){return 1;}

    public double getLowerBoundDouble(double centerParamDouble){return 1.0;}
    public double getUpperBoundDouble(double centerParamDouble){return 1.0;}

    public int getParamValueInt(int lowerBound, int upperBound, int maxSteps, int stepIndex){return 0;}
    public double getParamValueDouble(double lowerBound, double upperBound, int maxSteps, int stepIndex){return 1;}
}
