import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CharCounter implements ICharCounter, IHuffConstants{

	private Map<Integer, Integer> table = new HashMap<Integer, Integer>();
	
	@Override
	public int getCount(int ch) {
		if(ch<0||ch>ALPH_SIZE) {
			throw new IllegalArgumentException("It is not a valid char");
		}
		return table.getOrDefault(ch, 0);
	}

	@Override
	public int countAll(InputStream stream) throws IOException {
		int count = 0;
		int c = -1;
		c = stream.read();
		while(c!=-1) {
			count++;
			table.put(c, table.getOrDefault(c, 0)+1);
			c = stream.read();
		}
		return count;
	}

	@Override
	public void add(int i) {
		table.put(i,table.getOrDefault(i, 0)+1);
	}

	@Override
	public void set(int i, int value) {
		table.put(i, value);
	}

	@Override
	public void clear() {
		for(int i = 0; i < ALPH_SIZE; i++) {
			table.replace(i, 0);
		}
	}

	@Override
	public Map<Integer, Integer> getTable() {
		return table;
	}

}