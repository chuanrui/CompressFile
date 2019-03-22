import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class HuffTest {

	String testString;
	InputStream ins;
	
	@Before
	public void setUp() throws Exception {
		testString = "helloworld";
		ins = new ByteArrayInputStream(testString.getBytes("UTF-8"));
		String str1 = "worldhello";
		FileOutputStream outputStream = new FileOutputStream("EmptyFileForTest.txt");
		outputStream.close();
		outputStream = new FileOutputStream("ToBeCompressed_Write.txt");
		byte[] strToBytes = str1.getBytes();
	    outputStream.write(strToBytes);
	    outputStream.close();
	    String str2 = "When a compressed file is written the last bits written should be the bits that correspond to the pseudo-EOF char. You will have to write these bits explicitly. These bits will be recognized during the decompression process. This means that your decompression program will never actually run out of bits if it's processing a properly compressed file (you may need to think about this to really believe it). In other words, when decompressing you will read bits, traverse a tree, and eventually find a leaf-node representing some character. When the pseudo-EOF leaf is found, the program can terminate because all decompression is done. If reading a bit fails because there are no more bits (the bit-reading method returns -1) the compressed file is not well-formed. Your program should cope with files that are not well-formed, be sure to test for this, i.e., test decompression with plain (uncompressed) files. Your program should throw an IOException when such a file is found.";
	    outputStream = new FileOutputStream("LargeFile.txt");
	    strToBytes = str2.getBytes();
	    outputStream.write(strToBytes);
	    outputStream.close(); 
	}

	@Test
	public void testMakeHuffTree() throws IOException {
		Huff HTree = new Huff();
		HuffTree testTree = HTree.makeHuffTree(ins);
		assertEquals(testTree.root().weight(), 11);
		assertEquals(((HuffInternalNode) testTree.root()).left().weight(), 4);
		assertEquals(((HuffInternalNode) testTree.root()).right().weight(), 7);
	}

	@Test
	public void testMakeTable() throws IOException {
		Huff HTree = new Huff();
		HTree.makeHuffTree(ins);
		Map<Integer, String> codeTable = HTree.makeTable();
		assertEquals(codeTable.get((int)'o'),"110");
		assertEquals(codeTable.get((int)'l'),"10");
		assertEquals(codeTable.get((int)'h'),"010");
		assertEquals(codeTable.get((int)'e'),"011");
		assertEquals(codeTable.get((int)'w'),"1111");
		assertEquals(codeTable.get((int)'r'),"000");
		assertEquals(codeTable.get((int)'d'),"1110");
		assertEquals(codeTable.get(256),"001");
	}

	@Test
	public void testGetCode() throws IOException {
		Huff HTree = new Huff();
		HTree.makeHuffTree(ins);
		assertEquals(HTree.getCode('o'),"");
		HTree.makeTable();
		assertEquals(HTree.getCode('o'),"110");
		assertEquals(HTree.getCode('l'),"10");
	}
	@Test
	public void testGetCodeException() {
		Huff HTree = new Huff();
		try {
			HTree.getCode(-1);
			fail("should be Illegal");
		}
		catch(IllegalArgumentException e){
			assertEquals(e.getMessage(),"It is not a valid char");
		}
		try {
			HTree.getCode(257);
			fail("should be Illegal");
		}
		catch(IllegalArgumentException e){
			assertEquals(e.getMessage(),"It is not a valid char");
		}
	}

	@Test
	public void testShowCounts() throws IOException {
		Huff HTree = new Huff();
		assertEquals(HTree.showCounts(), null);
		HTree.makeHuffTree(ins);
		Map<Integer, Integer> countTable = HTree.showCounts();
		assertEquals(countTable.get((int)'o'), 2, 0.001);
	}

	@Test
	public void testHeaderSize() throws IOException {
		Huff HTree = new Huff();
		HTree.makeHuffTree(ins);
		assertEquals(119, HTree.headerSize());//8 leafNodes + 7 internalNodes + magic
	}
	@Test
	public void testWriteHeader() throws IOException {
		Huff HTree = new Huff();
		HTree.makeHuffTree(ins);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertEquals(119, HTree.writeHeader(new BitOutputStream(out)));//8 leafNodes + 7 internalNodes + magic
		out.close();
	}
	@Test
	public void testReadHeader() throws IOException {
		Huff HTree = new Huff();
		HTree.makeHuffTree(ins);
		BitOutputStream output = new BitOutputStream("compressedhead.txt");
		HTree.writeHeader(output);
		output.close();
		BitInputStream input = new BitInputStream("compressedhead.txt");
		HuffTree rootTree = HTree.readHeader(input);
		assertEquals(rootTree.root().weight(), 1);
		input.close();
	}
	@Test
	public void testReadHeaderMagicException() {
		Huff HTree = new Huff();
		BitInputStream wronginput = new BitInputStream("EmptyFileForTest.txt");
		try {
			HTree.readHeader(wronginput);
			fail("Magic number is wrong");
		}
		catch (IOException e) {
			assertEquals(e.getMessage(),"magic number not right");
		}
	}
	@Test
	public void testWrite() throws IOException {
		Huff HTree = new Huff();
		int size = HTree.write("ToBeCompressed_Write.txt", "Compressed_Write.txt", true);
		assertEquals(size, 180);
		size = HTree.write("ToBeCompressed_Write.txt", "NewFile.txt", false);//compressed file is larger
		assertEquals(size, 151);//no file been created
		size = HTree.write("LargeFile.txt", "CompressedLargeFile.txt", false);//compressed file is smaller
		assertEquals(size, 9041);//file been created
	}
	
	@Test
	public void testUncompress() throws IOException {
		Huff HTree = new Huff();
		int size = HTree.uncompress("Compressed_Write.txt", "UnCompressed.txt");
		assertEquals(size, 80);
		size = HTree.uncompress("CompressedLargeFile.txt", "UnCompressedLargeFile.txt");
		assertEquals(size, 7832);
	}
	@Test
	public void testUncompressException() {
		Huff HTree = new Huff();
		try {
			HTree.uncompress("compressedhead.txt", "NewFile.txt");
			fail("should throw unexpected end of input file");
		}
		catch (IllegalArgumentException e) {
			assertEquals(e.getMessage(),"unexpected end of input file");
		}
	}
}
