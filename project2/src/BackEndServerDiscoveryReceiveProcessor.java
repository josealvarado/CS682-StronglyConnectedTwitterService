import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;


public class BackEndServerDiscoveryReceiveProcessor implements Runnable {

	/**
	 * BackEndServerProcessor variables: clientSocket, database
	 */
	private Socket clientSocket;
	private BackEndServerDatabase database;
	
	/**
	 * BackEndServerProcessor constructor
	 * @param clientSocket
	 * @param database
	 */
	public BackEndServerDiscoveryReceiveProcessor(Socket clientSocket, BackEndServerDatabase database) {
		this.clientSocket = clientSocket;
		
		this.database = database;
		
//		if(this.database == null)
//			System.out.println("Is this null?????");
//		
//		String info = clientSocket.getRemoteSocketAddress().toString();
//		info = info.substring(1);
//		String[] info2 = info.split(":");
//		
//		this.database.addSecondary(info2[0],info2[1]);
	}
	
	/**
	 * Run the thread
	 */
	@Override
	public void run() {
		System.out.println("BackEndServerProcessor started");
		HTTPServer.log.debug("BackEndServerProcessor started");
		
		/**
		 * Get InputStream for the FrontEndServer
		 */
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {			
			System.out.println("BackEndServerProcessor IOException failed to create BufferedReader\nError Message: " + e.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServerProcessor IOException failed to create BufferedReader\nError Message: " + e.getLocalizedMessage());
			return;
		} 
		
		/**
		 * Get OutputStreadm for the FrontEndServer
		 */
		OutputStream out = null;
		try {
			out = clientSocket.getOutputStream();
		} catch (IOException e) {
			System.out.println("BackEndServerProcessor IOException failed to create OutputStream\nError Message: " + e.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServerProcessor IOException failed to create OutputStream\nError Message: " + e.getLocalizedMessage());
			return;
		}

		String request = "";
		String body = "";
		String line = "";
		int length = 0;
		boolean savedFirstLine = false;
		
		/**
		 * Read incoming request line by line
		 */
		try {
			while ((line = in.readLine()) != null) {
				
				System.out.println("BackEndServerProcessor RECEIVED" + line);
				HTTPServer.log.debug("BackEndServerProcessor RECEIVED" + line);
				
				/*
				 * Last line of request header is a blank line
				 * Followed by the request body
				 */
				if (line.equals("") ) {
					System.out.println("BACKENDSERVER STOP READING");
					HTTPServer.log.debug("BACKENDSERVER STOP READING");
					break; 
				}
				
				/*
				 * Check if line has information about Content-Length
				 * This determines if it has a message body or not
				 */
				if (line.startsWith("Content-Length: ")) { 
					int index = line.indexOf(':') + 1;
					String len = line.substring(index).trim();
					length = Integer.parseInt(len);
					break;
				}
				
				/**
				 * Saves the METHOD URI VERSION
				 */
				if(!savedFirstLine){
					request = line;
					savedFirstLine = true;
				}				
			}
		} catch (NumberFormatException | IOException e) {
			System.out.println("BackEndServerProcessor IOException failed to read request.\nError Message: " + e.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServerProcessor IOException failed to read request.\nError Message: " + e.getLocalizedMessage());
			return;
		} 		
		
		/*
		 * If a message body was found, read it
		 */
		if (length > 0) {
			int read;
			try {
				while ((read = in.read()) != -1) {
					body+= (char) read;
					if (body.length() == length)
					break;
				}
			} catch (IOException e) {
				System.out.println("BackEndServerProcessor IOException failed to read message body.\nError Message: "+ e.getLocalizedMessage());
				HTTPServer.log.debug("BackEndServerProcessor IOException failed to read message body.\nError Message: "+ e.getLocalizedMessage());
				return;
			}
		}
		
		
		System.out.println("Request: " + request);
		System.out.println("Body: " + body);
		HTTPServer.log.debug("Request: " + request);
		HTTPServer.log.debug("Body: " + body);
		
		String responseBody = "";
		String responseHeader = "";
		
		HTTPRequestLine requestLine = HTTPRequestLineParser.parse(request);
		
		JSONObject json = new JSONObject();
		json.put("statusCode", "400");

		if (requestLine != null){
			
			String version = requestLine.getHttpVerstion();
			HTTPConstants.HTTPMethod method = requestLine.getMethod();
			String uri = requestLine.getURIPathWithOutParams();
			System.out.println("Method: " + method);
			System.out.println("URI: " + uri);
			System.out.println("Version: " + version);
			HTTPServer.log.debug("Method: " + method);
			HTTPServer.log.debug("URI: " + uri);
			HTTPServer.log.debug("Version: " + version);
			
			/**
			 * Handle incoming POST requests
			 */
			if (method.toString().equals("POST")){
								
				/**
				 * No request body found
				 */
				if (body.length() <= 0){
					json.put("message", "ERROR BackEndServer MISSTING BODY");
					responseBody = json.toJSONString();
					responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
				} else {
					
					/**
					 * Verify request body was formatted properly
					 */
					boolean correctlyFormatted = requestLine.setBodyForBackEndServer(body);
					
					if (correctlyFormatted){
						
						/**
						 * Verify request URI
						 */
						if (uri.equals("/tweets")){
							
							/**
							 * Parse request body parameters
							 */
							String tweet = (String) requestLine.getValueFromParam("tweet");
							ArrayList<String> hashtags = (ArrayList<String>)requestLine.getValueFromParam("hashtags");
							System.out.println("Tweet: " + tweet);
							
							/**
							 * Respond to POST request
							 */
							if (tweet != null && hashtags!= null){
								
								database.addTweet(hashtags, tweet);
								
								json.put("statusCode", "201");
								json.put("message", "POST Correctly formattted Created for valid request");
								responseBody = json.toJSONString();
								responseHeader = "HTTP/1.1 201 Created\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";								
							} else {
								json.put("message", "ERROR POST MISSING CORRECT KEYS");
								responseBody = json.toJSONString();
								responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
							}
						} else if (uri.equals("/election")){
							String ID = (String) requestLine.getValueFromParam("ID");
							String version2 = "" + requestLine.getValueFromParam("Version");
							Integer version3 = Integer.parseInt(version2);
							
							Integer dbversion = this.database.getDatabaseVersion();
							
							if (dbversion > version3){
								ArrayList<HashMap<String, Object>> data = database.getLog(dbversion - version3);
								json.put("data", data);
//								json.put("dataSize", data.size());
							}
							
							System.out.println("I received the following ID: " + ID);
							
							if (Integer.parseInt(ID) > this.database.getDiscoveryReceivePort()){
								json.put("message", "You win!!");
								json.put("serverType", "SECONDARY");
							} else {
								json.put("message", "I win!!");
								json.put("serverType", "PRIMARY");
							}
							json.put("statusCode", "200");
							
							json.put("message", "ELECTION TIME!!");
							responseBody = json.toJSONString();
							responseHeader = "HTTP/1.1 200 Good Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
						}
						else {
							json.put("message", "ERROR POST MISSING URI /tweets");
							responseBody = json.toJSONString();
							responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
						}
					} else {
						json.put("message", "ERROR IMPROPERLY FORMATTED BODY");
						responseBody = json.toJSONString();
						responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
					}
				}
			} 
			/**
			 * Handle incoming GET requests
			 */
			else if (method.toString().equals("GET")){
				
				boolean correctlyFormatted = requestLine.setBodyForBackEndDiscoveryReceive(body);
				
				if (correctlyFormatted){
				
					/**
					 * Verify request URI
					 */
					if (uri.equals("/tweets")){
						
						/**
						 * Parse request body parameters
						 */
						Integer receivedBackEndDiscoveryVersion =  ((Long) requestLine.getValueFromParam("version")).intValue();								
						String receivedIP =  (String) requestLine.getValueFromParam("IP");								
						String receivedPORT = (String) requestLine.getValueFromParam("PORT");	
												
						this.database.addSecondary(receivedIP, receivedPORT);
						
						/**
						 * Respond to GET request
						 */
						if (receivedBackEndDiscoveryVersion != null){
											
							
							if (database == null)
								System.out.println("Really??");
							
							Integer databaseVersion = database.getDatabaseVersion();
							
							System.out.println("BackEndServerProcessor databaseVersion: " + databaseVersion);
							
							ArrayList<ArrayList<String>> secondaries = this.database.getSecondaries();
							
							if (receivedBackEndDiscoveryVersion == databaseVersion){
								
								HashMap json2 = new HashMap();
								json2.put("statusCode", "304");
								json2.put("message", "GET Correctly Formatted");
								json2.put("secondaries", secondaries);
								json2.put("v", databaseVersion);
							
								String jsonText = JSONValue.toJSONString(json2);
								responseBody = jsonText;
								responseHeader = "HTTP/1.1 304 Not Modified\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
							} else {						
								
								ArrayList<HashMap<String, Object>> data = database.getLog();
								
								HashMap json2 = new HashMap();
								json2.put("statusCode", "200");
								json2.put("message", "GET Correctly Formatted");
								json2.put("data", data);
								json2.put("secondaries", secondaries);
								json2.put("v", databaseVersion);
								
								String jsonText = JSONValue.toJSONString(json2);
								responseBody = jsonText;
								responseHeader = "HTTP/1.1 200 OK\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
							}
						} else {						
							json.put("message", "ERROR GET MISSING CORRECT KEYS");
							responseBody = json.toJSONString();
							responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
						}
					} else {
						json.put("message", "ERROR GET MISSING URI /tweets");
						responseBody = json.toJSONString();
						responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
					}
					
					
				} else {
					json.put("message", "ERROR IMPROPERLY FORMATTED BODY");
					responseBody = json.toJSONString();
					responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
				}
				
				
			} else {
				json.put("message", "ERROR MISSING CORRECT METHOD");
				responseBody = json.toJSONString();
				responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
			}
		} else {
			json.put("message", "ERROR IMPROPERLY FORMATTED REQUEST");
			responseBody = json.toJSONString();
			responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
		}
		
		/**
		 * Respond back to original request
		 */
		try {
			out.write(responseHeader.getBytes());
		} catch (IOException e) {
			System.out.println("BackEndServerProcessor IOException failed to write response header.\nError Message: "+ e.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServerProcessor IOException failed to write response header.\nError Message: "+ e.getLocalizedMessage());
			return;
		}
		try {
			out.write(responseBody.getBytes());
		} catch (IOException e) {
			System.out.println("BackEndServerProcessor IOException failed to write message body.\nError Message: "+ e.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServerProcessor IOException failed to write message body.\nError Message: "+ e.getLocalizedMessage());
			return;
		}		
		
		System.out.println("BackEndServerProcessor finished");
		HTTPServer.log.debug("BackEndServerProcessor finished");
	}
}

