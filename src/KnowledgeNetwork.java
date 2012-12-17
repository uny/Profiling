import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


public class KnowledgeNetwork {
	
	// private final
	private final double ATHRESHOLD = 0.0; // 回答内共起確率閾値
	private final double STHRESHOLD = 0.2; // 検索タイトル内共起確率閾値
	// private variable
	private Qadata[] qadata;
	private int pernum;
	private NaturalLanguage nl;
	private int topicindex;
	
	KnowledgeNetwork(){
		nl = new NaturalLanguage();
	}
	// public instance method
	// make Qadata
	public int prepare(String csvName) {
		List<Qadata> resultList = new ArrayList<Qadata>();
		try {
			FileReader csvFile = new FileReader(csvName);
			BufferedReader br = new BufferedReader(csvFile);
			String line = "";
			try {
				while ((line = br.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(line, ",");
					int i = 0;
					String who = new String("");
					Qadata tmp = new Qadata();
					String[] strs = new String[4];
					while (st.hasMoreTokens()) {
						if(i == 0){
							who = st.nextToken();
							if (who.charAt(0) == 'P') {
								tmp.who = who; 
							}
						}
						else if (!who.equals("")) {
							if ((i-1)%4 == 0) {
								strs[0] = st.nextToken();
							}
							else if ((i-1)%4 == 1) {
								strs[1] = st.nextToken();
							}
							else if ((i-1)%4 == 2) {
								strs[2] = st.nextToken();
							}
							else if ((i-1)%4 == 3) {
								strs[3] = st.nextToken();
								if (!strs[3].equals("-")) {
									try {
										int stage = Integer.parseInt(strs[0]);
										tmp.stage.add(stage);
										tmp.topic.add(strs[1]);
										tmp.question.add(strs[2]);
										tmp.answer.add(strs[3]);
									} catch (NumberFormatException e) {
									}
								}
							}
						}
						else {
							st.nextToken();
							i++;
							break;
						}
						i++;
					}
					if (tmp.who != null) {
						resultList.add(tmp);
					}
				}
				br.close();
				csvFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		qadata = (Qadata[])resultList.toArray(new Qadata[0]);
		pernum = qadata.length;
		return pernum;
	}
	// make Network
	public Network[] scan() {
		Network[] network = new Network[pernum];
		System.out.println("----------Start Scanning----------");
		for (int per=0; per<qadata.length; per++) {
			network[per] = new Network();
			Qadata perdata = qadata[per];
			network[per].words.add(perdata.who);
			network[per].linked.add(Network.Netlink.SEARCHED);
			Topicpos[] topics = makeTopicList(perdata);
			for (int i=0; i<topics.length; i++) {
				network[per].words.add(topics[i].topic);
				network[per].setWeight(0, i+1, 1.0);
				network[per].linked.add(Network.Netlink.SEARCHED);
			}
			System.out.println("-------Search Keywords-------");
			topicindex = topics.length;
			Keyweight keyweight = nl.getKeywords(perdata, topics);
			int offset = 0;
			for (int i=1; i<=keyweight.key.length; i++) {
				for (int j=0; j<keyweight.key[i-1].length; j++) {
					network[per].words.add(keyweight.key[i-1][j]);
					network[per].setWeight(i, topicindex+offset+1, keyweight.weight[i-1][j]);
					network[per].linked.add(Network.Netlink.LINKED);
					offset++;
				}
			}
			System.out.println("-------Collect Words-------");
			nl.collectWords(perdata, network[per]);
			System.out.println("-------Get Weights-------");
			getNetworkWeights(network[per], (String[])perdata.answer.toArray(new String[0]));
		}
		return network;
	}
	// private method
	// make qadata.topics
	private Topicpos[] makeTopicList(Qadata qadata) {
		Topicpos[] result;
		List<Integer> position = new ArrayList<Integer>();
		for (int i=0; i<qadata.stage.size(); i++) {
			if (qadata.stage.get(i) == 2) {
				position.add(i);
			}
		}
		result = new Topicpos[position.size()];
		for (int i=0; i<position.size(); i++) {
			result[i] = new Topicpos();
			result[i].pos = position.get(i);
			result[i].topic = qadata.topic.get(position.get(i));
		}
		return result;
	}
	// get cooccurrence between two words
	private void getNetworkWeights(Network network, String[] answers) {
		while (network.linked.indexOf(Network.Netlink.LINKED) != -1) {
			List<Integer> indexes = new ArrayList<Integer>();
			List<String> keys = new ArrayList<String>();
			int index;
			while ((index = network.linked.indexOf(Network.Netlink.LINKED)) != -1) {
				indexes.add(index);
				keys.add(network.words.get(index));
				network.linked.set(index, Network.Netlink.SEARCHED);
			}
			List<Integer> linkingindexes = new ArrayList<Integer>();
			List<String> linkingwords = new ArrayList<String>();
			while ((index = network.linked.indexOf(Network.Netlink.NOTLINKED)) != -1) {
				linkingindexes.add(index);
				linkingwords.add(network.words.get(index));
				network.linked.set(index, Network.Netlink.PRELINKED);
			}
			System.out.println("-----Link Words-----");
			String allanswer = "";
			for (String answer : answers) {
				allanswer += answer;
			}
			int[][] cooccucnts = new int[indexes.size()][linkingindexes.size()]; 
			String[] sentences = allanswer.split("．");
			for (String sentence : sentences) {
				for (int i=0; i<indexes.size(); i++) {
					if (sentence.indexOf(keys.get(i)) != -1) {
						for (int j=0; j<linkingindexes.size(); j++) {
							if (sentence.indexOf(linkingwords.get(j)) != -1) {
								cooccucnts[i][j]++;
							}
						}
					}
				}
			}
			for (int i=0; i<indexes.size(); i++) {
				System.out.print(keys.get(i) + " : ");
				for (int j=0; j<linkingindexes.size(); j++) {
					double coprob = (double)cooccucnts[i][j] / sentences.length;
					if (ATHRESHOLD < coprob) {
						network.setWeight(indexes.get(i), linkingindexes.get(j), coprob);
						network.linked.set(j, Network.Netlink.LINKING);
						System.out.print(linkingwords.get(j) + "->" + coprob + ", ");
					}
				}
				System.out.println();
			}
			//System.out.println(network.weights);
			while ((index = network.linked.indexOf(Network.Netlink.PRELINKED)) != -1) {
				network.linked.set(index, Network.Netlink.NOTLINKED);
			}
			System.out.println("-----Exclude Words-----");
			linkingindexes.clear();
			linkingwords.clear();
			while ((index = network.linked.indexOf(Network.Netlink.LINKING)) != -1) {
				linkingindexes.add(index);
				linkingwords.add(network.words.get(index));
				network.linked.set(index, Network.Netlink.PRELINKED);
			}
			for (int i=0; i<indexes.size(); i++) {
				for (int j=0; j<linkingindexes.size(); j++) {
					//System.out.println(network.weights.get(indexes.get(i)).get(linkingindexes.get(j)));
					if (ATHRESHOLD < network.weights.get(indexes.get(i)).get(linkingindexes.get(j))) {
						System.out.println("through");
						double coprob =  nl.getCoOccurrenceBySearch(keys.get(i), linkingwords.get(j));
						if (STHRESHOLD < coprob) {
							System.out.print(linkingwords.get(j) + "->" + coprob + ", ");
							network.linked.set(linkingindexes.get(j), Network.Netlink.LINKED);
						}
					}
				}
			}
			while ((index = network.linked.indexOf(Network.Netlink.LINKING)) != -1) {
				network.linked.set(index, Network.Netlink.NOTLINKED);
			}
		}
	}
}
