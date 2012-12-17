
public class Profile {
	public static void main(String[] args) {
		KnowledgeNetwork kn = new KnowledgeNetwork();
		int num = kn.prepare("Resources/answers.csv");
		if (num <= 0) {
			return;
		}
		Network[] network = kn.scan();
		
	}
}
