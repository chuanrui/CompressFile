import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import java.io.*;
public class CharCounterTest {

	ICharCounter cc;
	String testString;
	InputStream ins;
	@Before
	public void setUp() throws Exception {
		cc = new CharCounter();
		testString = "helloworld";
		ins = new ByteArrayInputStream(testString.getBytes("UTF-8"));
	}

	
	@Test
	public void testGetCount() throws IOException{
		cc.countAll(ins);
		assertEquals(cc.getCount('o'), 2);
		assertEquals(cc.getCount('a'), 0);
	}
	
	@Test
	public void testGetCountException() {
		try {
			cc.getCount(-1);
			fail("should be Illegal");
		}
		catch(IllegalArgumentException e){
			assertEquals(e.getMessage(),"It is not a valid char");
		}
		try {
			cc.getCount(257);
			fail("should be Illegal");
		}
		catch(IllegalArgumentException e){
			assertEquals(e.getMessage(),"It is not a valid char");
		}
	}


	@Test
	public void testCountAll() throws IOException{
		assertEquals(cc.countAll(ins), 10);
	}

	@Test
	public void testAdd() throws IOException{
		cc.countAll(ins);
		cc.add('o');
		assertEquals(cc.getCount('o'),3);
		cc.add('a');
		assertEquals(cc.getCount('a'),1);
	}

	@Test
	public void testSet() throws IOException {
		cc.countAll(ins);
		cc.set('b', 3);
		assertEquals(cc.getCount('b'), 3);
	}

	@Test
	public void testClear() throws IOException {
		cc.countAll(ins);
		assertEquals(cc.getCount('o'), 2);
		cc.clear();
		assertEquals(cc.getCount('o'), 0);
	}

	@Test
	public void testGetTable() throws IOException {
		cc.countAll(ins);
		assertEquals(cc.getTable().get((int)'o'), 2, 0.001);
	}

}
