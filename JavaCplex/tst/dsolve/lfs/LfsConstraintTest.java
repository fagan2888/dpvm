package dsolve.lfs;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/12/12
 * Time: 11:23 PM
 */

public class LfsConstraintTest {

    LfsConstraint unit = null;

    @Before
    public void runBeforeEveryTest() {

    }

    @Test
    public void testBubbleSortFunction() {
        unit = new LfsConstraint();

        int[] index = { 5, 4, 3, 2, 1 } ;
        int[] expectedIndex = { 1, 2, 3, 4, 5 } ;

        double [] terms = { 1.0, 1.1, 1.2, 1.3, 1.4 };
        double [] expectedTerms = { 1.4, 1.3, 1.2, 1.1, 1.0 };

        unit.setIndex( index );
        unit.setTerms( terms );

        unit.sortIndex();

        assertArrayEquals( unit.getIndex(), expectedIndex );
        assertArrayEquals( unit.getTerms(), expectedTerms, 0 );
    }
}
