package pvm.KernelProducts;

import pvm.PvmEntry;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/30/12
 * Time: 7:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class KernelProductManager {

    public static KernelProductManager kernelProductManager;

    public enum KerType{
        KERSCALAR, KERPOLY, KERRBF
    }

    KernelProduct kernels[];
    KernelProduct activeKernel;

    KerType kerType = KerType.KERSCALAR;

    public KernelProductManager(){
        int i;
        KerType kerTypes[] = KerType.values();
        kernels = new KernelProduct[kerTypes.length];

        for (i = 0; i < kernels.length; i++){
            switch (kerTypes[i]){
                case KERSCALAR: kernels[i] = new KernelProductScalar(); break;
                case KERPOLY: kernels[i] = new KernelProductPoly(); break;
                case KERRBF: kernels[i] = new KernelProductRbf(); break;

                default: kernels[i] = new KernelProductScalar(); break;
            }
        }

        if (kernels.length > 0)
            activeKernel = kernels[i];
        else
            activeKernel = null;
    }

    public static double ComputeKerProd(PvmEntry e0, PvmEntry e1){
        return kernelProductManager.activeKernel.computeKerProd(e0, e1);
    }

    public static void setParamInt(int paramInt){
        kernelProductManager.activeKernel.setParamInt(paramInt);
    }

    public static void setParamDouble(double paramDouble){
        kernelProductManager.activeKernel.setParamDouble(paramDouble);
    }

    public static int getMinParamI(){
        int centerInt = kernelProductManager.activeKernel.getPreferedCenterInt();
        return kernelProductManager.activeKernel.getLowerBoundInt(centerInt);
    }

    public static int getMaxParamI(){
        int centerInt = kernelProductManager.activeKernel.getPreferedCenterInt();
        return kernelProductManager.activeKernel.getUpperBoundInt(centerInt);
    }

    public static double getMinParamD(){
        double centerDouble = kernelProductManager.activeKernel.getPreferedCenterDouble();
        return kernelProductManager.activeKernel.getLowerBoundDouble(centerDouble);
    }

    public static  double getMaxParamD(){
        double centerDouble = kernelProductManager.activeKernel.getPreferedCenterDouble();
        return kernelProductManager.activeKernel.getUpperBoundDouble(centerDouble);
    }

    public static int getParamDMaxStepsCount(){
        return kernelProductManager.activeKernel.getMaxStepsDouble();
    }

    public static double getParamDValue(int cStep, int maxSteps){

        double boundLow, boundHigh;

        boundLow = getMinParamD();
        boundHigh = getMaxParamD();
        return kernelProductManager.activeKernel.getParamValueDouble(boundLow, boundHigh, maxSteps, cStep);
    }

    public static void setKernelTypeGlobal(KerType kerType){
        kernelProductManager.setKernelType(kerType);
    }

    public void setKernelType(KerType kerType){
        for (KernelProduct kernel : kernels)
            if (kernel.kerType == kerType){
                activeKernel = kernel;
                break;
            }
    }
}
