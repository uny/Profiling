import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.arnx.jsonic.JSON;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


public class NaturalLanguage {
	// public static final
	public static final double THRESHOLD = 0.2; // キーワード閾値
	// private static final
	private static final int TOPICSEARCH = 30;
	private static final double BOTH = 2.0/3;
	private static final int MAXMORPH = 20000;
	// public variable
	public List<String[]> words = new ArrayList<String[]>();
	// private variable
	private String exclude = "";
	// constructor
	NaturalLanguage() {
		FileReader fr;
		try {
			fr = new FileReader("Resources/exclude.csv");
			BufferedReader br = new BufferedReader(fr);
			String line = "";
			try {
				while ((line = br.readLine()) != null) {
					exclude += line;
				}
			} catch (IOException e) {
				exclude = "";
			}
			try {
				br.close();
			} catch (IOException e) {
			}
			try {
				fr.close();
			} catch (IOException e) {
			}
		} catch (FileNotFoundException e) {
			exclude = "";
		}
		System.out.println("exclude: " + exclude);
	}
	
	// public instance method
	public void collectWords(Qadata qadata, Network network) {
		String[][] morphed = morph((String[])qadata.answer.toArray(new String[0]));
		List<String> cluster = new ArrayList<String>();
		boolean noun = false;
		//System.out.println(morphed.length);
		for (int i=0; i<morphed.length; i++) {
			if (morphed[i][1].equals("名詞") || morphed[i][1].equals("接頭辞") || morphed[i][1].equals("接尾辞")) {
				// 数字のみは除外
				if (!morphed[i][0].matches("^[-.0-9０-９]+&")) {
					cluster.add(morphed[i][0]);
					noun = true;
				}
			}
			else if (noun == true) {
				//System.out.println("名詞以外");
				String appended = "";
				for (String str : cluster) {
					appended += str;
				}
				boolean nothing = true;
				for (String str : network.words) {
					if (appended.equals(str)) {
						//System.out.println("networkに同じのあり");
						nothing = false;
						break;
					}
				}
				if (nothing && exclude.indexOf(appended) == -1) {
					if (cluster.size() > 1) {
						network.words.add(appended);
						network.linked.add(Network.Netlink.NOTLINKED);
						System.out.print(appended + " ");
					}
					// 1文字のみは除外
					else if (cluster.size() == 1 && cluster.get(0).length() > 1) {
						network.words.add(appended);
						network.linked.add(Network.Netlink.NOTLINKED);
						System.out.print(appended + " ");
					}
				}
				cluster.clear();
				noun = false;
			}
			else if (noun == false &&
					(morphed[i][1].equals("形容詞") ||
							morphed[i][1].equals("形容動詞") ||
							morphed[i][1].equals("動詞"))) {
				if (!morphed[i][0].matches("^[ぁ-ゞ]+$")) {
					boolean nothing = true;
					for (String str : network.words) {
						if (morphed[i][0].equals(str)) {
							//System.out.println("networkに同じのあり");
							nothing = false;
							break;
						}
					}
					if (nothing && exclude.indexOf(morphed[i][0]) == -1) {
						network.words.add(morphed[i][0]);
						network.linked.add(Network.Netlink.NOTLINKED);
						System.out.print(morphed[i][0] + " ");
					}
				}
			}
		}
		System.out.println();
		while (network.weights.size() < network.words.size()) {
			network.expandWeights();
		}
	}
	// 
	public Keyweight getKeywords(Qadata qadata, Topicpos[] topics) {
		System.out.println("-" + qadata.who + "-");
		Keyweight result = new Keyweight();
		result.key = new String[topics.length][];
		result.weight = new Double[topics.length][];
		for (int i=0; i<topics.length; i++) {
			System.out.print(i + " : " + topics[i].topic + " : ");
			List<String> keywords = new ArrayList<String>();
			List<Double> weights = new ArrayList<Double>();
			String [][] nouns = connectNouns(qadata.answer.get(topics[i].pos), topics[i].topic);
			for (String[] noun : nouns) {
				words.add(noun);
			}
			for (int j=0; j<nouns.length; j++) {
				double prob = getCoOccurrenceBySearch(topics[i].topic, nouns[j]);
				if (THRESHOLD < prob) {
					String key = "";
					for(String str : nouns[j]) {
						key += str;
					}
					keywords.add(key);
					weights.add(prob);
					System.out.print(key + "->" + prob + ", ");
				}
			}
			result.key[i] = (String[])keywords.toArray(new String[0]);
			result.weight[i] = (Double[])weights.toArray(new Double[0]);
			System.out.println();
		}
		return result;
	}
	// get cooccurrence between two words
	public double getCoOccurrenceBySearch(String key, String obj) {
		String[] titles = getTitleThroughBingApi(obj + " " + key, TOPICSEARCH);
		int one = 0;
		int both = 0;
		try {
			for (String title : titles) {
				try {
					if (title.indexOf(obj) != -1) {
						one++;
						if (title.indexOf(key) != -1) {
							both++;
						}
					}
				} catch (NullPointerException e) {
					one = 0;
					both = 0;
				}
			}
		} catch (NullPointerException e) {
			return 0.0;
		}
		return BOTH * ((double)both / TOPICSEARCH) + (1 - BOTH) * ((double)one / TOPICSEARCH);
	}
	// private method
	private String[][] morph(String sentence) {
		return morphByYahoo(sentence);
	}
	private String[][] morph(String[] sentences) {
		return morphByYahoo(sentences);
	}
	private String[][] morphByYahoo(String sentence) {
		List<String[]> result = new ArrayList<String[]>();
		try {
			URL requrl = new URL("http://jlp.yahooapis.jp/MAService/V1/parse");
			String appid = "4tgJRISxg66ej1kilYiWN4JJ_vC2rbNYxbbqI_IuRv9wdbHv21DzFhFuV0lunLobmg--";
			try {
				HttpURLConnection connect = (HttpURLConnection) requrl.openConnection();
				connect.setRequestMethod("POST");
				connect.setDoOutput(true);
				connect.connect();
				OutputStreamWriter osw = new OutputStreamWriter( connect.getOutputStream() );
				String filter = "1|2|3|4|5|6|7|8|9|10|11|12|13";
				String contents =
					"appid=" + appid
					+ "&sentence=" + URLEncoder.encode(sentence,"UTF-8")
					+ "&results=" + "ma"
					+ "&filter=" + filter
					+ "&response=" + "baseform,pos";
				osw.write(contents);
				osw.flush();
				osw.close();
				InputSource inputSource = new InputSource(connect.getInputStream());
				XPath xpath = XPathFactory.newInstance().newXPath();
				NodeList nodes = null;
				try {
					nodes = (NodeList) xpath.evaluate("//urn:yahoo:jp:jlp:word", inputSource, XPathConstants.NODESET);
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
				for (int i = 0; i < nodes.getLength(); ++i) {
					Node word = nodes.item(i);
					Node baseform = null;
					try {
						baseform = (Node) xpath.evaluate("urn:yahoo:jp:jlp:baseform", word, XPathConstants.NODE);
					} catch (XPathExpressionException e) {
						e.printStackTrace();
					}
					Node pos = null;
					try {
						pos = (Node) xpath.evaluate("urn:yahoo:jp:jlp:pos", word, XPathConstants.NODE);
					} catch (XPathExpressionException e) {
						e.printStackTrace();
					}
					String[] tmp = new String[2];
					tmp[0] = baseform.getTextContent();
					tmp[1] = pos.getTextContent();
					result.add(tmp);
					// System.out.printf("%s, %s\n", surface.getTextContent(), pos.getTextContent()); // debug
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return (String[][])result.toArray(new String[0][0]);
	}
	private String[][] morphByYahoo(String[] sentences) {
		List<String[]> result = new ArrayList<String[]>();
		int answerposition = 0;
		while (answerposition < sentences.length) {
			String allanswer = "";
			for (int i=answerposition; i<sentences.length; i++) {
				if (allanswer.getBytes().length < MAXMORPH) {
					allanswer += sentences[i];
					answerposition++;
				}
				else {
					break;
				}
			}
			for (String[] strings : morph(allanswer)) {
				result.add(strings);
			}
		}
		 return (String[][])result.toArray(new String[0][0]);
	}
	// make keywords by connecting words
	private String[][] connectNouns(String sentence, String topic) {
		List<String[]> result = new ArrayList<String[]>();
		String[][] morphed = morph(sentence);
		List<String> cluster = new ArrayList<String>();
		boolean noun = false;
		for (int i=0; i<morphed.length; i++) {
			if (morphed[i][1].equals("名詞") || morphed[i][1].equals("接頭辞")) {
				// 数字のみは除外
				if (!morphed[i][0].matches("^[-.0-9０-９]+$")) {
					cluster.add(morphed[i][0]);
					noun = true;
				}
			}
			else if (noun == true) {
				String appended = "";
				for (String str : cluster) {
					appended += str;
				}
				//System.out.println(":" + appended + ":");
				if (!appended.equals(topic) && exclude.indexOf(appended) == -1) {
					if (cluster.size() > 1) {
						result.add((String[])cluster.toArray(new String[0]));
					}
					// 1文字のみは除外
					else if (cluster.size() == 1 && cluster.get(0).length() > 1) {
						result.add((String[])cluster.toArray(new String[0]));
					}
				}
				cluster.clear();
				noun = false;
			}
		}
		for (int i=result.size()-1; i>=0; i--) {
			String joined = StringUtils.join(result.get(i));
			for (int j=0; j<result.size(); j++) {
				if (i != j && StringUtils.join(result.get(j)).indexOf(joined) != -1) {
					result.remove(i);
					break;
				}
			}
		}
		/* debug
		for (String[] i : result) {
			for (String j : i) {
				System.out.print(j + " ");
			}
			System.out.println();
		}
		*/
		return (String[][])result.toArray(new String[0][0]);
	}
	// search and get titles by bing api - less than 7 times per second
	private String[] getTitleThroughBingApi(String key, int num) {
		String[] result;
		try {
			String bing = "http://api.bing.net/json.aspx?"
				+ "Appid=86CF0C5BC700E6B4EF10055D0794D6D52A16F87A"
				+ "&query=" + URLEncoder.encode(key, "UTF-8")
				+ "&sources=web"
				+ "&web.count" + num; // 1 <= num <= 50
			try {
				URL url = new URL(bing);
				try {
					HttpURLConnection connect = (HttpURLConnection) url.openConnection();
					connect.connect();
					Map<String, Map<String, Map<String, List<Map<String, String>>>>> map = JSON.decode(new InputStreamReader(connect.getInputStream()));
					List<Map<String, String>> sitelist = map.get("SearchResponse").get("Web").get("Results");
					try {
						result = new String[sitelist.size()];
						for (int i=0; i<result.length; i++) {
							result[i] = sitelist.get(i).get("Title");
						}
						/* debug
						for (int i=0; i<result.length; i++) {
							System.out.println(result[i]);
						}
						 */
					} catch (NullPointerException e) {
						System.out.print("Bing Search Error ");
						return null;
					}
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
	// get cooccurrence between string[] and key
	private double getCoOccurrenceBySearch(String key, String[] obj) {
		double result = 0;
		int objlen = obj.length;
		int all = objlen*(objlen+1)/2;
		List<Double[]> probList = new ArrayList<Double[]>();
		for (int i=1; i<=objlen; i++) {
			Double[] probs = new Double[objlen - i + 1];
			for (int j=0; i+j<=objlen; j++) {
				String joined = "";
				for (int k=j; k<i+j; k++) {
					joined += obj[k];
				}
				probs[j] = getCoOccurrenceBySearch(key, joined);
			}
			probList.add(probs);
		}
		double[] each = new double[objlen];
		for (int i=0; i<objlen; i++) {
			Double[] probs = probList.get(i);
			Arrays.sort(probs);
			int probslen = probs.length;
			int probsall = probslen*(probslen+1)/2;
			each[i] = 0;
			for (int j=1; j<=probslen; j++) {
				each[i] += j * probs[j-1] / probsall;
			}
		}
		for (int i=1; i<=objlen; i++) {
			result += i * each[i-1] / all;
		}
		return result;
	}
	/*
	// search and get titles by google api
	private String[] getTitleThroughGoogleApi(String key, int num) {
		String[] result = new String[num];
		return result;
	}
	// search and get titles by google
	private String[] getTitleThroughGoogle(String key, int num) {
		String[] result = new String[num];
		try {
			String google = "http://www.google.co.jp/search?"
				+ "filter=1"
				+ "&lr=lang_ja"
				+ "&ie=UTF-8"
				+ "&oe=UTF-8"
				+ "&as_qdr=all"
				+ "&num=" + num
				+ "&as_q=" + URLEncoder.encode(key, "UTF-8");
			try {
				URL url = new URL(google);
				try {
					HttpURLConnection connect = (HttpURLConnection) url.openConnection();
					connect.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/534.52.7 (KHTML, like Gecko) Version/5.1.2 Safari/534.52.7");
					connect.connect();
					Source source = new Source(new InputStreamReader(connect.getInputStream()));
					List<Element> lists = source.getAllElements("h3");
					if (lists != null) {
						int min = (num < lists.size()) ? num : lists.size();
						for (int i=0; i<min; i++) {
							result[i] = lists.get(i).getTextExtractor().toString();
						}
					}
					// debug
					for (int i=0; i<result.length; i++) {
						System.out.println(result[i]);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
	// search and get titles by yahoo api
	// APIを利用して作成したものがエンドユーザーへの検索サービス提供を目的としない、APIアクセスによるデータ取得・蓄積での利用の場合？
	private String[] getTitleThroughYahooApi(String key, int num) {
		String[] result = new String[num];
		
		return result;
	}
	// search and get titles by yahoo
	private String[] getTitleThroughYahoo(String key, int num) {
		String[] result = new String[num];
		try {
			String yahoo = "http://search.yahoo.co.jp/search?"
				+ "ei=UTF-8"
				+ "&va"
				+ "&yuragi=off"
				+ "&n=" + num // 10, 15, 20, 30, 40, 100
				+ "&p=" + URLEncoder.encode(key, "UTF-8");
			try {
				URL url = new URL(yahoo);
				try {
					HttpURLConnection connect = (HttpURLConnection) url.openConnection();
					connect.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/534.52.7 (KHTML, like Gecko) Version/5.1.2 Safari/534.52.7");
					connect.connect();
					Source source = new Source(new InputStreamReader(connect.getInputStream()));
					List<Element> lists = source.getAllElements("h3");
					if (lists != null) {
						int min = (num < lists.size()) ? num : lists.size();
						for (int i=0; i<min; i++) {
							result[i] = lists.get(i).getTextExtractor().toString();
						}
					}
					// debug
					for (int i=0; i<result.length; i++) {
						System.out.println(result[i]);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
	*/
}