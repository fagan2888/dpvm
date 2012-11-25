package dsolve;

import cgl.imr.base.SerializationException;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/24/12
 * Time: 6:43 PM
 */

public class NamedCoordListTest {
	@Before
	public void setup() {

	}

	@Test
	public void testBytesTransformation () throws SerializationException {
		NamedCoordList unitInput = new NamedCoordList();
		unitInput.add( new NamedCoord( "t1", 0.1 ) );
		unitInput.add( new NamedCoord( "t2", 1.2 ) );
		unitInput.rebuild();
		byte[] bytes = unitInput.getBytes();

		NamedCoordList unitOutput = new NamedCoordList();
		unitOutput.fromBytes( bytes );

		Assert.assertTrue( unitInput.size() == unitOutput.size() );
		Assert.assertEquals( unitInput.get(0), unitOutput.get(0) );
		Assert.assertEquals( unitInput.get(1), unitOutput.get(1) );
	}
}
