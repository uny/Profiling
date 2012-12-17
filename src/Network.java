import java.util.ArrayList;
import java.util.List;


public class Network {
	// public static final
	
	// public enum
	public static enum Netlink {
		NOTLINKED,
		PRELINKED,
		LINKING,
		LINKED,
		SEARCHED,
	}
	// public variable
	public List<String> words;
	public List<Netlink> linked; // 0:not-linked, 1:pre-linked, 2:linked, 3:pre-searched 4:searched
	public List<List<Double>> weights;
	// constructor
	Network() {
		words = new ArrayList<String>();
		linked = new ArrayList<Netlink>();
		weights = new ArrayList<List<Double>>();
	}
	public void expandWeights() {
		List<Double> emptyrow = new ArrayList<Double>(weights.size());
		for(int i=0; i<weights.size(); i++) {
			emptyrow.add(0.0);
		}
		weights.add(emptyrow);
		for (List<Double> row : weights) {
			row.add(0.0);
		}
	}
	public void setWeight(int index0, int index1, double weight) {
		if (weights.isEmpty()) {
			List<Double> tmp = new ArrayList<Double>();
			tmp.add(0.0);
			weights.add(tmp);
		}
		int bigger = (index0 < index1) ? index1 : index0;
		while (weights.size() <= bigger) {
			expandWeights();
		}
		weights.get(index0).set(index1, weight);
		weights.get(index1).set(index0, weight);
		//System.out.println(weights);
	}
	public void deleteWeight(int index) {
		weights.remove(index);
		for (List<Double> row : weights) {
			row.remove(index);
		}
	}
}
