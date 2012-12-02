package pvm;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/30/12
 * Time: 7:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class KerProduct {

    public enum KerType{
        KERSCALAR, KERPOLY, KERRBF
    }

    public static KerType kerType = KerType.KERSCALAR;
    public static double paramD = 1.0;
    public static int paramI = 1;

    private static int minParamI = 1;
    private static int maxParamI = 7;
    private static int minParamDExponent = -20;
    private static int maxParamDExponent = -1;
    private static int maxParamDSteps = 20;

    public static double ComputeKerProd(PvmEntry e0, PvmEntry e1)
    {
        switch (kerType)
        {
            case KERSCALAR: return ComputeKerProdScalar(e0, e1);
            case KERPOLY: return ComputeKerProdPoly(e0, e1);
            case KERRBF: return ComputeKerProdRbf(e0, e1);

            default: return ComputeKerProdScalar(e0, e1);
        }
    }

    public static int getMinParamI(){

        switch (kerType){
            case KERSCALAR: return 0;
            case KERRBF: return 0;
            case KERPOLY: return minParamI;

            default: return minParamI;
        }
    }

    public static int getMaxParamI(){
        switch (kerType){
            case KERSCALAR: return 0;
            case KERRBF: return 0;
            case KERPOLY: return maxParamI;

            default: return maxParamI;
        }
    }

    public static double getMinParamD(){
        switch (kerType)
        {
            case KERPOLY: return 0;
            case KERRBF: return Math.exp(-20);

            default : return 0;
        }
    }

    public static  double getMaxParamD(){
        if (kerType == KerType.KERSCALAR)
            return 0.0;

        return 1.0;
    }

    public static int getParamDMaxStepsCount(){

        switch(kerType){
            case KERSCALAR: return 1;
            case KERPOLY: return 4;
            case KERRBF: return maxParamDSteps;

            default: return 1;
        }
    }

    public static double getParamDValue(int cStep, int maxSteps){

        if (kerType == KerType.KERSCALAR)
            return 0.0;

        assert(maxSteps > 1 && cStep >= 0 && cStep < maxSteps);
        double r = (double)(cStep) / (double)(maxSteps - 1);

        switch (kerType){
            case KERPOLY: return r;
            case KERRBF: return Math.exp(minParamDExponent + r * (maxParamDExponent - minParamDExponent));

            default: return r;
        }
    }

    public static double ComputeKerProdScalar(PvmEntry e0, PvmEntry e1){
        int i;
        double res = 0;

        assert(e0.x.length == e1.x.length);
        for (i = 0; i < e0.x.length; i++)
            res += e0.x[i] * e1.x[i];

        return res;
    }

    public static double ComputeKerProdPoly(PvmEntry e0, PvmEntry e1){
        return Math.pow(ComputeKerProdScalar(e0, e1) + paramD, paramI);
    }

    public static double ComputeKerProdRbf(PvmEntry e0, PvmEntry e1){
        int i;
        double res = 0, temp;

        assert(e0.x.length == e1.x.length);
        for (i = 0; i < e0.x.length; i++)
        {
            temp = e0.x[i] - e1.x[i];
            res += temp * temp;
        }

        return Math.exp(- paramD * res);
    }
}
