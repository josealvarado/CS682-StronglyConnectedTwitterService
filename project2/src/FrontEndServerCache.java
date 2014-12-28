import java.util.ArrayList;
import java.util.HashMap;

public class FrontEndServerCache {
	
	/**
	 * FrontEndServerCache variables: tweetMap, versionMap, lock
	 */
	private HashMap<String, ArrayList<String>> tweetMap;
	private HashMap<String, Integer> versionMap;
	
	private MultiReaderLock lock = new MultiReaderLock();
	
	/**
	 * Default constructor
	 */
	public FrontEndServerCache(){
		tweetMap = new HashMap<String, ArrayList<String>>();
		versionMap = new HashMap<String, Integer>();
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
	public Integer getVersion(String hashtag){
		lock.lockRead();
		Integer version = 0;
		if (versionMap.containsKey(hashtag)){
			version = versionMap.get(hashtag);
		}
		lock.unlockRead();
		return version;
	}
	
	/**
	 * Update tweets and version for the corresponding hashtag
	 * @param newVersion
	 * @param query
	 * @param tweets
	 */
	public void updateQuery(Integer newVersion, String query, ArrayList<String> tweets){
		lock.lockWrite();
		versionMap.put(query, newVersion);
		tweetMap.put(query, tweets);
		lock.unlockWrite();
	}
}
