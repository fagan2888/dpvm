package pvm.KernelProducts;

import junit.framework.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 1/4/13
 * Time: 7:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class KernelProductScalarTest {
    @Test
    public void test_computeKerProduct_ReturnsZeroOnZeroLengthInput(){
        KernelProductScalar kerscalar = new KernelProductScalar();
        double x[] = new double[0];

        double ret = kerscalar.computeKerProd(x, x);

        Assert.assertEquals(ret, 0.0);
    }

    @Test
    public void test_computeKerProduct_ComputesAccordingToLowestLength(){
        KernelProductScalar kerscalar = new KernelProductScalar();
        double x[] = {1, 2};
        double y[] = {3, 2, 1};

        double ret = kerscalar.computeKerProd(x, y);

        Assert.assertTrue(Math.abs(ret - 7) < 1e-10);
    }
}
