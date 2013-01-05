package pvm.KernelProducts;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/5/13
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class KernelProductRbfTest {
    @Test(expected = IllegalArgumentException.class)
    public void test_setParamDouble_FailsOnZeroInput(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.setParamDouble(0.0);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void test_setParamDouble_FailsOnNegativeInput(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.setParamDouble(-1.5);
    }

    @Test
    public void test_setParamDouble_SucceedsOnPositiveInput(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.setParamDouble(1e-10);
    }

    @Test
    public void test_computeKerProd_ReturnsOneOnZeroLengthInput(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        double x[] = new double[0];
        double ret = kerRbf.computeKerProd(x, x);

        Assert.assertTrue(Math.abs(ret - 1.0) < 1e-20);
    }

    @Test
    public void test_computeKerProd_ComputesAccordingToLowestLength(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        double x[] = {1, 2};
        double y[] = {2, 0, 5};
        double ret = kerRbf.computeKerProd(x, y);

        Assert.assertTrue(Math.abs(ret - 0.00673794699908546709663604842315) < 1e-10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getLowerBoundDouble_FailsOnZeroCenter(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.getLowerBoundDouble(0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getLowerBoundDouble_FailsOnNegativeCenter(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.getLowerBoundDouble(-1.0);
    }

    @Test
    public void test_getLowerBoundDouble_SucceedsOnPositiveCenter(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.getLowerBoundDouble(1e-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getUpperBoundDouble_FailsOnZeroCenter(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.getUpperBoundDouble(0.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_getUpperBoundsDouble_FailsOnNegativeCenter(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.getUpperBoundDouble(-1e-20);
    }

    @Test
    public void test_getUpperBoundsDouble_SucceedsOnPositiveCenter(){
        KernelProductRbf kerRbf = new KernelProductRbf();
        kerRbf.getUpperBoundDouble(1e-3);
    }
}
