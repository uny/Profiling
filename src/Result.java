import java.util.ArrayList;
import java.util.List;


public class Result {
	// public variable
	public String who;
	public List<Integer> stage;
	public List<String> topic;
	public List<String> question;
	public List<String> answer;
	// constructor
	Result(){
		stage = new ArrayList<Integer>();
		topic = new ArrayList<String>();
		question = new ArrayList<String>();
		answer = new ArrayList<String>();
	}
}
