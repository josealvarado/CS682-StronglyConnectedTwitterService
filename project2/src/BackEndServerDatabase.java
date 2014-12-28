import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class BackEndServerDatabase {
	
	/**
	 * BackEndServerDatabase variables: tweetMap, versionMap,  lock
	 */
	private HashMap<String, ArrayList<String>> tweetMap;
	private HashMap<String, Integer> versionMap;
	
	private MultiReaderLock lock = new MultiReaderLock();
	
	private int databaseVersion;
	
	private ArrayList<HashMap<String, Object>> log = new ArrayList<HashMap<String, Object>>();
	
	private ArrayList<ArrayList<String>> secondaries = new ArrayList<ArrayList<String>>();

	private String primaryIP;
	private int primaryPort;
	
	private String backendIP;
	private int backendPort;
	
	private int discoverySendPort;
	private int discoveryReceivePort;
	
	private String status;
	
	private String disoveryIP;
	private Integer discoveryPort;
	
	/**
	 * Default constructor
	 */
	public BackEndServerDatabase(){
		tweetMap = new HashMap<String, ArrayList<String>>();
		versionMap = new HashMap<String, Integer>();
		
		databaseVersion = 0;
		
	}
	
	/**
	 * Add tweet to database for every hashtag in hashtags
	 * Increment current version of tweets for each hashtag
	 * @param hashtags
	 * @param tweet
	 */
	public void addTweet(ArrayList<String> hashtags, String tweet){
		lock.lockWrite();
		
		HashMap<String, Object> addTweet = new HashMap<String, Object>();
		addTweet.put("hashtags", hashtags);
		addTweet.put("tweet", tweet);
		this.log.add(addTweet);
		
		for (String hashtag: hashtags){
//			System.out.println("hashtag: " + hashtag + " successfully added to database");
			
			if (tweetMap.containsKey(hashtag)){
				ArrayList<String> tweets = tweetMap.get(hashtag);
				tweets.add(tweet);
				versionMap.put(hashtag, versionMap.get(hashtag) + 1);
			} else {
				ArrayList<String> tweets = new ArrayList<String>();
				tweets.add(tweet);
				tweetMap.put(hashtag, tweets);
				versionMap.put(hashtag, 1);
			}
		}
		this.databaseVersion += 1;
		lock.unlockWrite();
	}
	
	/**
	 * Return all tweets with the corresponding hashtag
	 * @param hashtag
	 * @return
	 */
	public ArrayList<String> getTweet(String hashtag){
		lock.lockRead();
		ArrayList<String> tweets = null;
		if (tweetMap.containsKey(hashtag)){
			tweets = tweetMap.get(hashtag);
		}
		lock.unlockRead();
		return tweets;
	}
	
	/**
	 * Return current version of tweets for corresponding hashtag
	 * @param hashtag
	 * @return
	 */
	public Integer getHashTagVersion(String hashtag){
		lock.lockRead();
		Integer version = 0;
		if (versionMap.containsKey(hashtag)){
			version = versionMap.get(hashtag);
		}
		lock.unlockRead();
		return version;
	}
	
	public Integer getDatabaseVersion(){
		int version = 0;
		lock.lockRead();
		version = this.databaseVersion;
		lock.unlockRead();
		return version;
	}
	
	public ArrayList<HashMap<String, Object>> getLog(){
		ArrayList<HashMap<String, Object>> temp = new ArrayList<HashMap<String, Object>>();
		lock.lockRead();		
		for (HashMap<String, Object> addTweet: this.log){
			HashMap<String, Object> temp2 = new HashMap<String, Object>();
			temp2.put("tweet", addTweet.get("tweet"));
			temp2.put("hashtags", addTweet.get("hashtags"));
			temp.add(temp2);
		}
		lock.unlockRead();
		return temp;
	}
	
	public ArrayList<HashMap<String, Object>> getLog(Integer num){
		ArrayList<HashMap<String, Object>> temp = new ArrayList<HashMap<String, Object>>();
		lock.lockRead();	
		
		System.out.println("INTEGER: " + num);
		System.out.println("Log Size: " + log.size());
		
		for (int i = this.log.size() - 1; i >= 0; i--){
			System.out.println("INDEX I: " + i);
			HashMap<String, Object> addTweet = this.log.get(i);
			HashMap<String, Object> temp2 = new HashMap<String, Object>();
			temp2.put("tweet", addTweet.get("tweet"));
			temp2.put("hashtags", addTweet.get("hashtags"));
			temp.add(temp2);
		}
		lock.unlockRead();
		return temp;
	}
	
	public void setBackendInfo(String backendIP, int discoveryReceivePort){
		lock.lockWrite();
		this.backendIP = backendIP;
		this.discoveryReceivePort = discoveryReceivePort;
		lock.unlockWrite();
	}
	
	public void setPrimaryInfo(String primaryIP, int primaryPort){
		lock.lockWrite();
		this.primaryIP = primaryIP;
		this.primaryPort = primaryPort;
		
		System.out.println("Saved data: " + this.primaryIP + " " + this.primaryPort);
		lock.unlockWrite();
	}
	
	public String getPrimaryIP(){
		String IP;
		lock.lockRead();
		IP = this.primaryIP;
		System.out.println("IP " + IP);
		lock.unlockRead();
		return IP;
	}
	
	public Integer getPrimaryPort(){
		Integer port;
		lock.lockRead();
		port = this.primaryPort;
		System.out.println("PORT " + port);
		lock.unlockRead();
		return port;
	}
	
	public void setDiscoveryInfo(String primaryIP2, int primaryPort2){
		lock.lockWrite();
		this.disoveryIP = primaryIP2;
		this.discoveryPort = primaryPort2;
		lock.unlockWrite();
	}
	
	public String getDiscoveryIP(){
		String IP;
		lock.lockRead();
		IP = this.disoveryIP;
		lock.unlockRead();
		return IP;
	}
	
	public Integer getDiscoveryPort(){
		Integer port;
		lock.lockRead();
		port = this.discoveryPort;
		lock.unlockRead();
		return port;
	}
	
	public int getDiscoveryReceivePort(){
		int port = 0;
		lock.lockRead();
		port = this.discoveryReceivePort;
		lock.unlockRead();
		return port;
	}
	
	public void setStatus(String status){
		lock.lockWrite();
		this.status = status;
		lock.unlockWrite();
	}
	
	public String getStatus(){
		String tempStatus = "";
		lock.lockRead();
		tempStatus = this.status;
		lock.unlockRead();
		return tempStatus;
	}
	
	public void addSecondary(String ip, String port){
		lock.lockWrite();
		ArrayList<String> secondary = new ArrayList<String>();
		secondary.add(ip);
		secondary.add(port);
		
		boolean exist = false;
		
		if (ip.equals(backendIP) && port.equals(""+discoveryReceivePort)){
			exist = true;
			System.out.println("DO NOT ADD THIS, IT'S MY IP AND PORT!!!!");
		} else {
			for(ArrayList temp: this.secondaries){
				System.out.println("Secondary | IP: " + temp.get(0) + ", PORT: " + temp.get(1));
				System.out.println("Backend | IP: " + backendIP + ", PORT: " + discoveryReceivePort);
				if ( temp.get(0).equals(ip) && temp.get(1).equals(port)){
					exist = true;
					break;
				}
			}
		}
		
		
		
		if (!exist){
			System.out.println("SECONDARY WAS ADDED, WAS THIS A MISTAKE????");
			this.secondaries.add(secondary);
		}
		lock.unlockWrite();
	}
	
	public ArrayList<ArrayList<String>> getSecondaries(){
		ArrayList<ArrayList<String>> secondary = new ArrayList<ArrayList<String>>();
		lock.lockRead();
		for(ArrayList<String> second: this.secondaries){
			ArrayList<String> s = new ArrayList<String>();
			s.add(second.get(0));
			s.add(second.get(1));
			secondary.add(s);
		}
		lock.unlockRead();
		return secondary;
	}
}
