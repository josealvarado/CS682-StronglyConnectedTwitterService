import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
HTTPRequestLine is a data structure that stores a Java representation of the parsed Request-Line.
 **/
public class HTTPRequestLine {

	private HTTPConstants.HTTPMethod method;
	private String uripath;
	private HashMap<String, Object> parameters;
	private String httpversion;
	
    /*
      You are expected to add appropriate constructors/getters/setters to access and update the data in this class.
     */
	
	public HTTPRequestLine(){
		this.method = null;
		this.uripath = "";
		this.parameters = new HashMap<String, Object>();
		this.httpversion = "";
	}

	private HTTPConstants.HTTPMethod setHTTPMethod(String method){
		switch(method){
	        case "OPTIONS" :
	            return HTTPConstants.HTTPMethod.OPTIONS;
	        case "GET":
	            return HTTPConstants.HTTPMethod.GET;
	        case "HEAD":
	            return HTTPConstants.HTTPMethod.HEAD;
	        case "POST":
	            return HTTPConstants.HTTPMethod.POST;
	        case "PUT":
	            return HTTPConstants.HTTPMethod.PUT;
	        case "DELETE":
	            return HTTPConstants.HTTPMethod.DELETE;
	        case "TRACE":
	            return HTTPConstants.HTTPMethod.TRACE;
	        case "CONNECT":
	            return HTTPConstants.HTTPMethod.CONNECT;
	    }
		return null;
	}

	public void setMethod(String method) throws HTTPMethodException{
		this.method = setHTTPMethod(method);
		if (method == null)
			throw new HTTPMethodException();
	}
	
	public void setUri(String uriPath) throws HTTPUriException{
		if (!uriPath.startsWith("/"))
			throw new HTTPUriException();
				
		if (uriPath.contains("?")){
			String[] splitUri = uriPath.split("\\?");
									
			String concat = "";
			for (int i = 1; i < splitUri.length; i++)
				concat += splitUri[i];
		
			String[] paramters = concat.split("&");
						
			for (String param: paramters){
				String[] splitParam = param.split("=");
									
				if (splitParam.length != 2)
					throw new HTTPUriException();
				
				if (splitParam[0].trim().length() < 1)
					throw new HTTPUriException();
				
				if (this.parameters.containsKey(splitParam[0]))
					throw new HTTPUriException();
				
				this.parameters.put(splitParam[0], splitParam[1]);
			}
		} 
		this.uripath = uriPath;
	}
	
	public void setHttpverstion(String version) throws HTTPVersionException {
		if (!version.startsWith("HTTP/"))
			throw new HTTPVersionException();
		if (!version.endsWith("1.0") && !version.endsWith("1.1"))
			throw new HTTPVersionException();
		this.httpversion = version;
	}
	
	public HTTPConstants.HTTPMethod getMethod(){
		return this.method;
	}
	
	public String getURIPathWithOutParams(){
		if (this.uripath.contains("?")){
			String[] splitURI = this.uripath.split("\\?");
			return splitURI[0];
		}
		return this.uripath;
	}
	
	public String getUriPath(){
		return this.uripath;
	}
	
	public Object getValueFromParam(String param){
		if (this.parameters.containsKey(param))
			return this.parameters.get(param);
		return null;
	}
	
	public String getHttpVerstion(){
		return this.httpversion;
	}
	
	public boolean setBodyForBackEndDiscoveryReceive(String body){
		System.out.println("Received Body: " + body);
		
		HashMap json = (HashMap) jsonStringToMap(body);
		if (json == null){
			return false;
		}
		
		for (Object key: json.keySet()){
			String keyString = (String) key;
			System.out.println("Key: " + key);
			System.out.println("Value: " + json.get(key));
			if (keyString.equals("version")){
				this.parameters.put(keyString, json.get(key));
			} else if (keyString.equals("IP")){
				this.parameters.put(keyString, json.get(key));
			} else if (keyString.equals("PORT")){
				this.parameters.put(keyString, json.get(key));
			} 
		}
		
		return true;
	}
	
	public boolean setBodyForBackEndDiscoverySend(String body){
		System.out.println("Received Body: " + body);
		
		HashMap json = (HashMap) jsonStringToMap(body);
		if (json == null){
			return false;
		}
		
		for (Object key: json.keySet()){
			String keyString = (String) key;
			System.out.println("Key: " + key);
			System.out.println("Value: " + json.get(key));
			if (keyString.equals("secondaries")){
				this.parameters.put(keyString, json.get(key));
			} else if (keyString.equals("message")){
				this.parameters.put(keyString, json.get(key));
			} else if (keyString.equals("serverType")){
				this.parameters.put(keyString, json.get(key));
			} else if (keyString.equals("data")){
				this.parameters.put(keyString, json.get(key));
			} 
//			else if (keyString.equals("dataSize")){
//				this.parameters.put(keyString, json.get(key));
//			} 
			else if (keyString.equals("IP")){
				this.parameters.put(keyString, json.get(key));
			} else if (keyString.equals("Port")){
				this.parameters.put(keyString, json.get(key));
			} 
		}
		
		return true;
	}
	
	public boolean setBodyForBackEndServer(String body){
		System.out.println("Received Body: " + body);
		
		HashMap json = (HashMap) jsonStringToMap(body);
		if (json == null){
			return false;
		}
		
		for (Object key: json.keySet()){
			String keyString = (String) key;
			System.out.println("Key " + key);
			System.out.println("Value " + json.get(key));
			if (keyString.equals("tweet")){
				this.parameters.put(keyString, (String)json.get(key));
				System.out.println("Saved tweet");
			} else if (keyString.equals("hashtags")){
				ArrayList<String> hashtags = (ArrayList<String>) json.get(keyString);
				for (String hashtag: hashtags){
					System.out.println("hashtag: " + hashtag);
				}
				this.parameters.put(keyString, hashtags);
				System.out.println("Saved hashtags");
			} else if (keyString.equals("ID")){
				this.parameters.put(keyString, ""+json.get(key));
				System.out.println("Saved ID");
			} else if (keyString.equals("IP")){
				this.parameters.put(keyString, ""+json.get(key));
			} else if (keyString.equals("PORT")){
				this.parameters.put(keyString, ""+json.get(key));
			} else if (keyString.equals("Version")){
				this.parameters.put(keyString, ""+json.get(key));
			} else if (keyString.equals("delay")){
				this.parameters.put(keyString, ""+json.get(key));
			}
		}
		
		return true;
	}
	
	public boolean setBodyForFrontEndServer(String body){
		System.out.println("Received Body: " + body);
		
		HashMap json = (HashMap) jsonStringToMap(body);
		if (json == null){
			return false;
		}
		
		for (Object key: json.keySet()){
			String keyString = (String) key;
			System.out.println("Key " + key);
			System.out.println("Value " + json.get(key));
			if (keyString.equals("text")){
				this.parameters.put(keyString, (String)json.get(key));
				System.out.println("Saved tweet");
			} else if (keyString.equals("delay")){
				this.parameters.put(keyString, ""+json.get(key));
			}
		}
		
		return true;
	}
	
	public boolean setBodyForBackEndServerResponseToFrontEndServer(String body){
		System.out.println("Received Body: " + body);
		
		HashMap json = (HashMap) jsonStringToMap(body);
		if (json == null){
			return false;
		}
		
		for (Object key: json.keySet()){
			String keyString = (String) key;
			System.out.println("Key " + key);
			System.out.println("Value " + json.get(key));
			if (keyString.equals("q")){
				this.parameters.put(keyString, (String)json.get(key));
				System.out.println("Saved tweet");
			} else if (keyString.equals("v")){
				this.parameters.put(keyString, json.get(key));
				System.out.println("Saved version");
			} else if (keyString.equals("tweets")){
				ArrayList<String> tweets = (ArrayList<String>) json.get(keyString);
//				for (String tweet: tweets){
//					System.out.println("tweets: " + tweet);
//				}
				this.parameters.put(keyString, tweets);
				System.out.println("Saved tweets");
			} 
		}
		
		return true;
	}
	
	private Map jsonStringToMap(String jsonString){
		JSONParser parser = new JSONParser();

		ContainerFactory containerFactory = new ContainerFactory(){
		    public List creatArrayContainer() {
		    	return new ArrayList();
		    }

		    public Map createObjectContainer() {
		    	return new LinkedHashMap();
		    }
		    
		};
		
		Map jsonMap = null;
		try {
			jsonMap = (Map)parser.parse(jsonString, containerFactory);
		} catch (ParseException e) {
			System.out.println("HTTPRequestLine ParseException.\nError Message: " + e.getLocalizedMessage());
		}
		return jsonMap;
	}
}