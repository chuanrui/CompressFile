import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Huff implements ITreeMaker, IHuffEncoder, IHuffModel, IHuffHeader{

	private Map<Integer, Integer> countTable;
	private Map<Integer, String> codeTable;
	private HuffTree tree;
	
	@Override
	public HuffTree makeHuffTree(InputStream stream) throws IOException {
		CharCounter cc = new CharCounter();
		cc.countAll(stream);
		countTable = cc.getTable();
		Comparable[] heap = new HuffTree[countTable.keySet().size()+1];
		int i = 0;
		//put nodes into minHeap
		for(Integer o:countTable.keySet()) {
			heap[i] = new HuffTree(o, countTable.get(o));
			i++;
		}
		heap[i] = new HuffTree(PSEUDO_EOF, 1);
		MinHeap TreeHeap = new MinHeap(heap, heap.length, ALPH_SIZE+1);
		HuffTree tmp1, tmp2, tmp3=null;
		//build HuffTree
		if(TreeHeap.heapsize()==1) return (HuffTree) TreeHeap.removemin();
		while(TreeHeap.heapsize()>1) {
			tmp1 = (HuffTree) TreeHeap.removemin();
			tmp2 = (HuffTree) TreeHeap.removemin();
			tmp3 = new HuffTree(tmp1.root(), tmp2.root(), tmp1.weight()+tmp2.weight());
			TreeHeap.insert(tmp3);
		}
		tree = tmp3;
		return tree;
	}

	@Override
	public Map<Integer, String> makeTable() {
		codeTable = new HashMap<Integer, String>();
		makeCode(codeTable, tree.root(), "");
		return codeTable;
	}
	/**
	 * Helper function to encode 
	 * @param codeTable
	 * @param root
	 * @param code
	 */
	private void makeCode(Map<Integer, String> codeTable, IHuffBaseNode root, String code) {
		if(root.isLeaf()) {
			codeTable.put(((HuffLeafNode)root).element(), code);
			return;
		}
		String tmpPath = code;
		makeCode(codeTable, ((HuffInternalNode)root).left(), tmpPath+"0");
		makeCode(codeTable, ((HuffInternalNode)root).right(), tmpPath+"1");
	}

	@Override
	public String getCode(int i) {
		if(i<0||i>ALPH_SIZE) {
			throw new IllegalArgumentException("It is not a valid char");
		}
		if(codeTable == null) return "";
		return codeTable.getOrDefault(i, "");
	}

	@Override
	public Map<Integer, Integer> showCounts() {
		return countTable;
	}

	@Override
	public int headerSize() {
		//size of tree plus magic number
		return tree.size()+BITS_PER_INT;
	}

	@Override
	public int writeHeader(BitOutputStream out) {
		out.write(BITS_PER_INT, MAGIC_NUMBER);
		return BITS_PER_INT+writeHelper(out, tree.root());
	}
	/**
	 * Helper function of writeHeader
	 * @param out
	 * @param root
	 */
	private int writeHelper(BitOutputStream out, IHuffBaseNode root) {
		if(root.isLeaf()) {
			out.write(1, 1);
			out.write(9, ((HuffLeafNode)root).element());
			return 10;
		}
		out.write(1, 0);
		return 1+writeHelper(out, ((HuffInternalNode)root).left())+writeHelper(out, ((HuffInternalNode)root).right());
	}

	@Override
	public HuffTree readHeader(BitInputStream in) throws IOException {
		int magic = in.read(BITS_PER_INT);
		if(magic!=MAGIC_NUMBER) {
			throw new IOException("magic number not right");
		}
		tree = buildTree(in);
		return tree;
	}
	/**
	 * Helper function of readHeader
	 * @param in
	 * @return root
	 * @throws IOException 
	 */
	private HuffTree buildTree(BitInputStream in) throws IOException {
		int isLeaf = in.read(1);
		if(isLeaf == 0) {//not leaf
			HuffTree tmp1, tmp2;
			tmp1 = buildTree(in);
			tmp2 = buildTree(in);
			return new HuffTree(tmp1.root(), tmp2.root(), 1);//weight does not matter
		}
		else {//leaf
			return new HuffTree(in.read(9), 1);//weight does not matter
		}
	}

	@Override
	public int write(String inFile, String outFile, boolean force){
		BitInputStream copyIn = new BitInputStream(inFile);//generating header
		BitInputStream in = new BitInputStream(inFile);
		try {
			this.makeHuffTree(copyIn);
		} catch (IOException e) {
			e.printStackTrace();
		}
		copyIn.close();
		this.makeTable();
		int oriSize = 0, comSize = this.headerSize();
		//calculate compressed and uncompressed size
		for(Integer i: this.countTable.keySet()) {//notice that countTable does not have PSEUDO_EOF
			oriSize += this.countTable.get(i)*BITS_PER_WORD;
			comSize += this.countTable.get(i)*this.codeTable.get(i).length();
		}
		comSize += this.codeTable.get(PSEUDO_EOF).length();//PSEUDO_EOF
		if(comSize>=oriSize&&!force) {
			in.close();
			return comSize;
		}
		//start compressing
		BitOutputStream out = new BitOutputStream(outFile);
		this.writeHeader(out);
		int ch= -1;
		try {
			ch = in.read(BITS_PER_WORD);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(ch!=-1) {
			out.write(this.codeTable.get(ch).length(), Integer.parseInt(this.codeTable.get(ch), 2));
			comSize+=this.codeTable.get(ch).length();
			try {
				ch = in.read();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		out.write(this.codeTable.get(PSEUDO_EOF).length(), Integer.parseInt(this.codeTable.get(PSEUDO_EOF), 2));
		in.close();
		out.close();
		return comSize;
	}

	@Override
	public int uncompress(String inFile, String outFile)  {
		BitInputStream in = new BitInputStream(inFile);
		BitOutputStream out = new BitOutputStream(outFile);
		try {
			this.readHeader(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		IHuffBaseNode tmpTree = this.tree.root();
		int uncomSize = 0;
		int bits=-1;
		while (true) {
			try {
				bits = in.read(1);
			} catch (IOException e) {
				e.printStackTrace();
			}
		    if (bits == -1)
		    {
		    	throw new IllegalArgumentException("unexpected end of input file");
		    }
		    else
		    { 
		      // use the zero/one value of the bit read
		      // to traverse Huffman coding tree
		      // if a leaf is reached, decode the character and print UNLESS
		      // the character is pseudo-EOF, then decompression done
			     if ( (bits & 1) == 0) { // read a 0, go left in tree
			    	 tmpTree = ((HuffInternalNode)tmpTree).left();
			     }
			     else { // read a 1, go right in tree
			    	 tmpTree = ((HuffInternalNode)tmpTree).right();
			     }
			     if (tmpTree.isLeaf()){
			    	if (((HuffLeafNode)tmpTree).element()==PSEUDO_EOF) {
			    		break; // out of loop
			    	}
			    	else {
			    		try {
							out.write(((HuffLeafNode)tmpTree).element());
						} catch (IOException e) {
							e.printStackTrace();
						}
			    		uncomSize+=BITS_PER_WORD;
			    		tmpTree = this.tree.root();
			    	}
			     }
		    }
		}
		in.close();
		out.close();
		return uncomSize;
	}

}
