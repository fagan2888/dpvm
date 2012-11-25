package dsolve.lfs;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/3/12
 * Time: 6:19 PM
 */

public class LfsReaderTest {

    @Test
    public void testReadingCorrectFile() throws IOException {
        String filePath = "test-input-files/lfs-reader-test-1.txt";
        LfsReader unit = new LfsReader( filePath );

        LfsConstraint constraint = null;
        int lineCount = 0;
        while( (constraint = unit.readConstraint()) != null ) {
            assertTrue( constraint.getIndex().length != 0 );
            lineCount ++;
        }

        Assert.assertTrue( "couldn't read any constraints", lineCount > 0);
    }

    @Test ( expected = ArrayIndexOutOfBoundsException.class )
    public void exceptionThrownIfInvalidConstraint() throws IOException {
        String filePath = "test-input-files/lfs-reader-test-2.txt";
        LfsReader unit = new LfsReader( filePath );

        unit.readConstraint();
    }

    @Test ( expected = IllegalArgumentException.class )
    public void exceptionThrownIfNoTermsInConstraint() throws IOException {
        String filePath = "test-input-files/lfs-reader-test-3.txt";
        LfsReader unit = new LfsReader( filePath );

        unit.readConstraint();
    }

}
