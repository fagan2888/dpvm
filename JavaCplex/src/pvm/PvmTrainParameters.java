package pvm;

import pvm.KernelProducts.KernelProductManager;

import java.util.Comparator;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/6/13
 * Time: 4:05 PM
 */

public class PvmTrainParameters implements Cloneable, Comparator<PvmTrainParameters> {
    public KernelProductManager.KerType kerType = KernelProductManager.KerType.KERSCALAR;
    public int paramInt = 0;
    public double paramDouble = 1.0;
    public double trainBias = 1.0;

    public double accuracy = 0;
    public double sensitivity = 0;
    public double specificity = 0;

    @Override
    protected Object clone() throws CloneNotSupportedException {
	    super.clone();
        PvmTrainParameters ret = new PvmTrainParameters();

        ret.kerType = kerType;
        ret.paramInt = paramInt;
        ret.paramDouble = paramDouble;
        ret.trainBias = trainBias;

        ret.accuracy = accuracy;
        ret.sensitivity = sensitivity;
        ret.specificity = specificity;

        return ret;
    }

	public void copyFrom( PvmTrainParameters other ) {
		this.kerType = other.kerType;
		this.paramInt = other.paramInt;
		this.paramDouble = other.paramDouble;
		this.trainBias = other.trainBias;

		this.accuracy = other.accuracy;
		this.sensitivity = other.sensitivity;
		this.specificity = other.specificity;
	}

    @Override
    public boolean equals(Object other){
        if (other.getClass() != PvmTrainParameters.class)
            return false;

        PvmTrainParameters ptpOther = (PvmTrainParameters)other;

        if (ptpOther.kerType != kerType ||
            ptpOther.paramInt != paramInt ||
            Math.abs(ptpOther.paramDouble - paramDouble) > 1e-20 ||
            Math.abs(ptpOther.trainBias - trainBias) > 1e-20)
           return false;

        return true;
    }

    public int compare(PvmTrainParameters params1, PvmTrainParameters params2){
        if (params1.accuracy > params2.accuracy)
            return -1;
        else if (params1.accuracy < params2.accuracy)
            return 1;

        return 0;
    }

	@Override
	public String toString() {
		return "KTYPE:" + kerType.name() + "/PINT:" + paramInt + "/PDBL:" + paramDouble + "/BIAS:" + trainBias;
	}

	public String toCompleteString() {
		return toString()+ String.format(
			"/ACC:%.05f/SENS:%.05f/SPEC:%.05f",
			accuracy, sensitivity, specificity
		);
	}
}
