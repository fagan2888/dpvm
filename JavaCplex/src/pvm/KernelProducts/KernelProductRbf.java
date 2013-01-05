package pvm.KernelProducts;

import org.apache.commons.lang3.Validate;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/5/13
 * Time: 2:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class KernelProductRbf extends KernelProduct{

    double gamma = 1.0;

    static double gammaInitialSearchCenter = Math.pow(2, -6);
    static double gammaInitialPowerRange = 14;
    static int gammaSearchMaxSteps = 29;

    public KernelProductRbf(){
        kerType = KernelProductManager.KerType.KERRBF;
    }

    public double computeKerProd(double [] x0, double [] x1){
        double ret = 0, temp;
        int i, last_i = x0.length;
        if (last_i > x1.length)
            last_i = x1.length;

        for (i = 0; i < last_i; i++){
            temp = x0[i] - x1[i];
            ret += temp * temp;
        }

        ret *= -gamma;

        return Math.exp(ret);
    }
    public void setParamInt(int paramInt){return;}
    public void setParamDouble(double paramDouble){
        Validate.isTrue(paramDouble > 0, "The gamma value for the RBF kernel should always be positive");
        gamma = paramDouble;
    }

    public int getPreferedCenterInt(){return 0;}
    public double getPreferedCenterDouble(){return gammaInitialSearchCenter;}

    public int getLowerBoundInt(int centerParamInt){return 0;}
    public int getUpperBoundInt(int centerParamInt){return 0;}

    public double getLowerBoundDouble(double centerParamDouble){
        Validate.isTrue(centerParamDouble > 0, "Search center for RBF should be positive");
        double pow = Math.log(centerParamDouble) / Math.log(2.0);

        return Math.pow(2, pow - gammaInitialPowerRange / (double)refinementLvl);
    };
    public double getUpperBoundDouble(double centerParamDouble){
        Validate.isTrue(centerParamDouble > 0, "Search center for RBF should be positive");
        double pow = Math.log(centerParamDouble) / Math.log(2.0);

        return Math.pow(2, pow + gammaInitialPowerRange / (double)refinementLvl);
    }

    public int getMaxStepsInt(){return 1;}
    public int getMaxStepsDouble(){
        int ret = gammaSearchMaxSteps / refinementLvl;
        if (ret < 1)
            ret = 1;

        return ret;
    }

    public int getParamValueInt(int lowerBound, int upperBound, int maxSteps, int stepIndex){
        return lowerBound;
    }
    public double getParamValueDouble(double lowerBound, double upperBound, int maxSteps, int stepIndex){

        if (maxSteps < 2)
            return lowerBound;

        double powLower, powUpper, powInterval;
        Validate.isTrue(lowerBound > 0, "Lower parameter bound should be positive");
        Validate.isTrue(upperBound > 0, "Upper parameter bound should be positive");
        Validate.isTrue(upperBound >= lowerBound, "Parameter interval should have higher upper bound than lower bound");

        powLower = Math.log(lowerBound) / Math.log(2.0);
        powUpper = Math.log(upperBound) / Math.log(2.0);

        powInterval = powUpper - powLower;
        powInterval /= (maxSteps - 1);

        return Math.pow(2, powLower + powInterval * stepIndex);
    }
}
