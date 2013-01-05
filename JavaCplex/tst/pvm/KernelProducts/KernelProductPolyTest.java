package pvm.KernelProducts;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/4/13
 * Time: 7:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class KernelProductPolyTest {

    @Test(expected = IllegalArgumentException.class)
    public void test_setParamInt_FailsOnNullPowerInput(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.setParamInt(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_setParamInt_FailsOnNegativePowerInput(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.setParamInt(-2);
    }

    @Test
    public void test_setParamInt_SucceedsOnPositivePowerInput(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.setParamInt(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_setParamDouble_FailsOnNegativeOffsetInput(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.setParamDouble(-1e-20);
    }

    @Test
    public void test_setParamDouble_SucceedsOnNullOffsetInput(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.setParamDouble(0);
    }

    @Test
    public void test_setParamDouble_SucceedsOnPositiveInput(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.setParamDouble(0.5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_setRefinementLevel_FailsOnNegativeInput(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.setRefinementLevel(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getParamValueInt_FailsOnStepIndexGreaterThanMaxSteps(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.getParamValueInt(1, 3, 2, 2);
    }

    @Test
    public void test_getParamValueInt_ForSmallMaxStepsReturnsLowerBound(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        Assert.assertEquals(kerpoly.getParamValueInt(1, 3, 1, 0), 1);
    }

    @Test
    public void test_getParamValueInt_ReturnCorrectForUsualCase(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        Assert.assertEquals(kerpoly.getParamValueInt(1, 3, 2, 0), 1);
        Assert.assertEquals(kerpoly.getParamValueInt(1, 3, 2, 1), 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getParamValueDouble_FailsOnStepIndexGreaterThanMaxSteps(){
        KernelProductPoly kerpoly = new KernelProductPoly();
        kerpoly.getParamValueDouble(1, 3, 2, 2);
    }

    @Test
    public void test_getParamValueDouble_ForSmallMaxStepsReturnsLowerBound(){
        KernelProductPoly kernelProduct = new KernelProductPoly();
        double retVal = kernelProduct.getParamValueDouble(1.0 , 3.0, 1, 0);
        Assert.assertTrue(Math.abs(retVal - 1.0) < 1e-20);
    }

    @Test
    public void test_getParamValueDouble_ReturnCorrectForUsualCase(){
        KernelProductPoly kernelProduct = new KernelProductPoly();

        double retVal = kernelProduct.getParamValueDouble(0.0 , 0.5, 2, 0);
        Assert.assertTrue(Math.abs(retVal) < 1e-20);

        retVal = kernelProduct.getParamValueDouble(0.0 , 0.5, 2, 1);
        Assert.assertTrue(Math.abs(retVal - 0.5) < 1e-20);
    }
}
