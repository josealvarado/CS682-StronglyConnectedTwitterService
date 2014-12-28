import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class FrontEndServerProcessor implements Runnable{

	/**
	 * FrontEndServerProcessor variables: clientSocket, cache, backEndIP, backEndPort
	 */
	private Socket clientSocket;
	private FrontEndServerCache cache;
	
	private String backEndIP;
	private int backEndPort;
	
	private String discoveryIP;
	private int discoveryPort;
	
	/**
	 * FrontEndServerProcessor constructor
	 * @param clientSocket
	 * @param cache
	 * @param IP
	 * @param PORT
	 */
	public FrontEndServerProcessor(Socket clientSocket, FrontEndServerCache cache, String IP, int PORT, String discoveryIP, int discoveryPort) {
		this.clientSocket = clientSocket;
		this.cache = cache;
		this.backEndIP = IP;
		this.backEndPort = PORT;
		this.discoveryIP = discoveryIP;
		this.discoveryPort = discoveryPort;
	}
	
	/**
	 * Run the thread
	 */
	@Override
	public void run() {
		System.out.println("FrontEndServerProcessor started");
		HTTPServer.log.debug("FrontEndServerProcessor started");
		
		/**
		 * Get InputStream for the BackEndServer
		 */
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		} catch (IOException e) {			
			System.out.println("FrontEndServerProcessor failed to create BufferedReader\nError Message: " + e.getLocalizedMessage());
			HTTPServer.log.debug("FrontEndServerProcessor failed to create BufferedReader\nError Message: " + e.getLocalizedMessage());
			return;
		} 
		
		/**
		 * Get OutputStream for the BackEndServer
		 */
		OutputStream out = null;
		try {
			out = clientSocket.getOutputStream();
		} catch (IOException e) {
			System.out.println("FrontEndServerProcessor failed to create OutputStream\nError Message: " + e.getLocalizedMessage());
			HTTPServer.log.debug("FrontEndServerProcessor failed to create OutputStream\nError Message: " + e.getLocalizedMessage());
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
				
				/*
				 * Last line of request header is a blank line
				 * Followed by the request body
				 */
				if ( line.equals("") ) {
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
			System.out.println("FrontEndServerProcessor failed to read request.\nError Message: " + e.getLocalizedMessage());
			HTTPServer.log.debug("FrontEndServerProcessor failed to read request.\nError Message: " + e.getLocalizedMessage());
			return;
		} 

		/*
		 * If a request body was found, read it
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
				System.out.println("FrontEndServerProcessor failed to read message body.\nError Message: "+ e.getLocalizedMessage());
				HTTPServer.log.debug("FrontEndServerProcessor failed to read message body.\nError Message: "+ e.getLocalizedMessage());
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
					boolean correctlyFormatted = requestLine.setBodyForFrontEndServer(body);
					
					if (correctlyFormatted){
						
						/**
						 * Verify request URI
						 */
						if (uri.equals("/tweets")){
							
							/**
							 * Parse request body parameters
							 */
							String tweet = (String) requestLine.getValueFromParam("text");
							System.out.println("Tweet: " + tweet);
							if (tweet != null){
								
								String[] splitTweet = tweet.split(" ");
								ArrayList<String> hashtags = new ArrayList<String>();
								boolean emptyHashTag = false;
								
								for (String word: splitTweet){
									if (word.startsWith("#")){
										if (word.length() == 1){
											emptyHashTag = true;
										} else {
											hashtags.add(word.substring(1));
										}
									}
								}
								
								
								/**
								 * Respond to POST request
								 */
								if (hashtags.size() <= 0){
									json.put("message", "ERROR POST MISSING HASHTAGS");
									responseBody = json.toJSONString();
									responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
								} else if (emptyHashTag){
									json.put("message", "ERROR POST EMPTY HASHTAG");
									responseBody = json.toJSONString();
									responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
								} else {
									
									System.out.println("Send tweet + hashtags to BackEndServer");
																		
									HashMap<String, Object> info = new HashMap<String, Object>();
									info.put("request", "POST /tweets HTTP/1.1");
									
									HashMap<String, Object> infoBody = new HashMap<String, Object>();
									infoBody.put("tweet", tweet);
									infoBody.put("hashtags", hashtags);
									
									String delay = (String) requestLine.getValueFromParam("delay");
									infoBody.put("delay", delay);
									
									info.put("body", infoBody);
									
									/**
									 * Forward request to BackEndServer
									 */
									HashMap<String, Object> response = connectToBackEndSever(info);
									
									if (response.containsKey("error")){
										json.put("message", "ERROR POST " + response.get("error"));
										responseBody = json.toJSONString();
										responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
									} else {
										json.put("statusCode", "201");
										json.put("message", "POST Correctly formattted Created for valid request");
										responseBody = json.toJSONString();
										responseHeader = "HTTP/1.1 201 Created\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
									}
								}
							} else {
								json.put("message", "ERROR POST MISSING CORRECT KEYS");
								responseBody = json.toJSONString();
								responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
							}
						} else {
							json.put("statusCode", "404");
							json.put("message", "ERROR POST MISSING URI /tweets");
							responseBody = json.toJSONString();
							responseHeader = "HTTP/1.1 404 Not Found\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
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
				
				/**
				 * Verify request URI
				 */
				if (uri.equals("/tweets")){
					
					/**
					 * Parse request body parameters
					 */
					String query = (String) requestLine.getValueFromParam("q");
					System.out.println("q " + query);		
					
					if (query != null){		
						
						
						Integer cacheVersion = cache.getVersion(query);
						
						System.out.println("FrontEndServer cachedVersion: " + cacheVersion + " q: " + query);
						
						System.out.println("Send hashtag + version to BackEndServer");
						
						HashMap<String, Object> info = new HashMap<String, Object>();
						info.put("request", "GET /tweets?q=" + query + "&v=" + cacheVersion + " HTTP/1.1");

						/**
						 * Forward request to BackEndServer
						 */
						HashMap<String, Object> response = connectToBackEndSever(info);
						
						/**
						 * Respond to GET request
						 */
						if (response.containsKey("error")){
							json.put("message", "ERROR POST " + response.get("error"));
							responseBody = json.toJSONString();
							responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
						} else {
							
							String status = (String) response.get("response");
							String[] statusSplit = status.split(" ");
							status = statusSplit[1];
							String responseBody2 = (String) response.get("body");
							
							System.out.println("STATUS " + status);
							
							if (status.equals("200")){

								boolean correctlyFormatted = requestLine.setBodyForBackEndServerResponseToFrontEndServer(responseBody2);

								if (correctlyFormatted){
									long newVersion = (long) requestLine.getValueFromParam("v");
									System.out.println("NewVersion: " + newVersion);
									
									ArrayList<String> tweets = (ArrayList<String>) requestLine.getValueFromParam("tweets");
									
									cache.updateQuery(Integer.parseInt(""+newVersion), query, tweets);
									
									json.put("statusCode", "200");
									json.put("message", "POST Correctly formattted Created for valid request");
									json.put("q", query);
									json.put("tweets", tweets);
									
									responseBody = json.toJSONString();
									responseHeader = "HTTP/1.1 200 OK\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
								} else {
									json.put("message", "ERROR POST Body Incorrectly formattted");
									responseBody = json.toJSONString();
									responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
								}
							} else if (status.equals("304")){
								
								ArrayList<String> tweets = cache.getTweet(query);
								
								json.put("statusCode", "200");
								json.put("message", "POST Correctly formattted Created for valid reques");
								json.put("q", query);
								json.put("tweets", tweets);
								
								responseBody = json.toJSONString();
								
								responseHeader = "HTTP/1.1 200 OK\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
							} else {
								json.put("message", "POST UNKNOWN STATUS");
								responseBody = json.toJSONString();
								responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
							}
						}
					} else {	
						json.put("message", "ERROR GET MISSING CORRECT KEYS");
						responseBody = json.toJSONString();
						responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
					}
				} else {
					json.put("message", "ERROR GET MISSING URI /tweets ");
					responseBody = json.toJSONString();
					responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
				}
			} else {
				json.put("message", "ERROR MISSING CORRECT METHOD");
				responseBody = json.toJSONString();
				responseHeader = "HTTP/1.1 400 BAD Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
			}
	
		} else {
			json.put("message", "ERROR MISSING CORRECT METHOD");
			responseBody = json.toJSONString();
			responseHeader = "HTTP/1.1 400 BAD REQUEST\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
		}
		
		/**
		 * Respond back to original request
		 */
		try {
			out.write(responseHeader.getBytes());
		} catch (IOException e) {
			System.out.println("FrontEndServerProcessor failed to write response header.\nError Message: "+ e.getLocalizedMessage());
			HTTPServer.log.debug("FrontEndServerProcessor failed to write response header.\nError Message: "+ e.getLocalizedMessage());
			return;
		}
		try {
			out.write(responseBody.getBytes());
		} catch (IOException e) {
			System.out.println("FrontEndServerProcessor failed to write message body.\nError Message: "+ e.getLocalizedMessage());
			HTTPServer.log.debug("FrontEndServerProcessor failed to write message body.\nError Message: "+ e.getLocalizedMessage());
			return;
		}		
		
		System.out.println("FrontEndServerProcessor Thread finished");
		HTTPServer.log.debug("FrontEndServerProcessor Thread finished");
	}
	
	/**
	 * Connect to BackEndServer
	 * Forward request
	 * @param input
	 * @return
	 */
	public HashMap connectToBackEndSever(HashMap input){
		Socket socket = null;
		DataOutputStream out = null;
		DataInputStream in = null;
		
		HashMap response = null;
		
		System.out.println("Connecting to BackEndServer with IP " + backEndIP + " PORT " + backEndPort);
		HTTPServer.log.debug("Connecting to BackEndServer with IP " + backEndIP + " PORT " + backEndPort);
		
		int maxAttempts = 5;
		int countAttempts = 0;
		
		do{
			response = new HashMap();
			try {
				/**
				 * Establish connection
				 */
				socket = new Socket(backEndIP, backEndPort);
				out = new DataOutputStream(socket.getOutputStream());
				in = new DataInputStream(socket.getInputStream());
			
				System.out.println("Connected to BackEndServer with IP " + backEndIP + " PORT " + backEndPort);
				HTTPServer.log.debug("Connected to BackEndServer with IP " + backEndIP + " PORT " + backEndPort);

				/**
				 * Write to BackEndServer
				 */
				String jsonText = JSONValue.toJSONString(input.get("body"));
				
				String request = (String) input.get("request");
				String body = "" + jsonText + " ";
				
				out.writeBytes(request);
				out.writeBytes("\n");
				
				out.writeBytes("Content-Length: " + body.getBytes().length);
				out.writeBytes("\n");
				out.writeBytes("\n");

				out.writeBytes(body);
				
				out.flush();
				
				System.out.println("Sent request to backend");
				
				String line = "";
				boolean readFirstLine = false;
				int length = 0;
				
				/**
				 * Read response from BackEndServer
				 */
				try {
					while ((line = in.readLine()) != null) {
						
						/*
						 * Last line of request header is a blank line
						 * Quit while loop when last line of header is reached
						 */
						if (line.equals("")) {
							System.out.println("FrontEndServerProcessor STOP READING");
							HTTPServer.log.debug("FrontEndServerProcessor STOP READING");
							break; 
						}
						
						if (!readFirstLine){
							response.put("response", line);
							readFirstLine = true;
						}
						
						if (line.startsWith("Content-Length: ")) { 
							int index = line.indexOf(':') + 1;
							String len = line.substring(index).trim();
							length = Integer.parseInt(len);
						}
						
						System.out.println("FrontEndServer RECEIVED: " + line);
						HTTPServer.log.debug("FrontEndServer RECEIVED: " + line);
					}
				} catch (NumberFormatException | IOException e) {
					response.put("error", "FrontEndServerProcessor failed to read request");
					System.out.println("FrontEndServerProcessor failed to read request.\nError Message: " + e.getLocalizedMessage());
					HTTPServer.log.debug("FrontEndServerProcessor failed to read request.\nError Message: " + e.getLocalizedMessage());
				} 

				String responseBody = "";
				
				/*
				 * If a message body was found, read it
				 */
				if (length > 0) {
					int read;
					try {
						while ((read = in.read()) != -1) {
							responseBody+= (char) read;
							if (responseBody.length() == length)
							break;
						}
					} catch (IOException e) {
						System.out.println("FrontEndServerProcessor failed to read message body.\nError Message: "+ e.getLocalizedMessage());
						HTTPServer.log.debug("FrontEndServerProcessor failed to read message body.\nError Message: "+ e.getLocalizedMessage());
					}
				}
				
				System.out.println("FrontEndServerProcessor RECEIVED BODY " + responseBody);
				HTTPServer.log.debug("FrontEndServerProcessor RECEIVED BODY " + responseBody);
				response.put("body", responseBody);
					
				out.flush();

				out.close();
				in.close();

			} catch (UnknownHostException e) {
				response.put("error", "FrontEndServerProcessor UnknownHostException");

				System.out.println("\nFrontEndServerProcessor UnknownHostException\nError Message: " + e.getLocalizedMessage() + "\n");
				HTTPServer.log.debug("\nFrontEndServerProcessor UnknownHostException\nError Message: " + e.getLocalizedMessage() + "\n");
			} catch (IOException e) {
				response.put("error", "nFrontEndServerProcessor IOException");

				System.out.println("\nFrontEndServerProcessor IOException\nError Message: " + e.getLocalizedMessage() + "\n");
				HTTPServer.log.debug("\nFrontEndServerProcessor IOException\nError Message: " + e.getLocalizedMessage() + "\n");
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					response.put("error", "FrontEndServerProcessor IOException closing socket");

					System.out.println("\nFrontEndServerProcessor IOException closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
					HTTPServer.log.debug("\nFrontEndServerProcessor IOException closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
				} catch (Exception e){
					response.put("error", "FrontEndServerProcessor Exception closing socket");

					System.out.println("\nFrontEndServerProcessor Exception closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
					HTTPServer.log.debug("\nFrontEndServerProcessor Exception closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
				}
			}
			
			countAttempts++;
			
			System.out.println(response);
			if (response.containsKey("error")){
				connectToDiscoveryServer();
				System.out.println("Failed");
			} else {
				System.out.println("Success");
			}
		}
		while(countAttempts< maxAttempts && response.containsKey("error"));
			
			
			
			
		
		
		
		return response;
	}
	
	public void connectToDiscoveryServer(){
		Socket socket = null;
		DataOutputStream out = null;
		DataInputStream in = null;
		
		HashMap response = null;
		
		System.out.println("Connecting to BackEndServer ...");
		HTTPServer.log.debug("Connecting to BackEndServer ...");
		
		int maxAttempts = 5;
		int countAttempts = 0;
		
			response = new HashMap();
			try {
				/**
				 * Establish connection
				 */
				socket = new Socket(discoveryIP, discoveryPort);
				out = new DataOutputStream(socket.getOutputStream());
				in = new DataInputStream(socket.getInputStream());
			
				System.out.println("Connected to BackEndServer with IP " + discoveryIP + " PORT " + discoveryPort);
				HTTPServer.log.debug("Connected to BackEndServer with IP " + discoveryIP + " PORT " + discoveryPort);

				/**
				 * Write to BackEndServer
				 */
				
				String request = "GET /discovery HTTP/1.1";
				String body = "";
				
				out.writeBytes(request);
				out.writeBytes("\n");
				
				out.writeBytes("Content-Length: " + body.getBytes().length);
				out.writeBytes("\n");
				out.writeBytes("\n");

				out.writeBytes(body);
				
				out.flush();
				
				System.out.println("Sent request to backend");
				
				String line = "";
				boolean readFirstLine = false;
				int length = 0;
				
				/**
				 * Read response from BackEndServer
				 */
				try {
					while ((line = in.readLine()) != null) {
						
						/*
						 * Last line of request header is a blank line
						 * Quit while loop when last line of header is reached
						 */
						if (line.equals("")) {
							System.out.println("FrontEndServerProcessor STOP READING");
							HTTPServer.log.debug("FrontEndServerProcessor STOP READING");
							break; 
						}
						
						if (!readFirstLine){
							response.put("response", line);
							readFirstLine = true;
						}
						
						if (line.startsWith("Content-Length: ")) { 
							int index = line.indexOf(':') + 1;
							String len = line.substring(index).trim();
							length = Integer.parseInt(len);
						}
						
						System.out.println("FrontEndServer RECEIVED: " + line);
						HTTPServer.log.debug("FrontEndServer RECEIVED: " + line);
					}
				} catch (NumberFormatException | IOException e) {
					response.put("error", "FrontEndServerProcessor failed to read request");
					System.out.println("FrontEndServerProcessor failed to read request.\nError Message: " + e.getLocalizedMessage());
					HTTPServer.log.debug("FrontEndServerProcessor failed to read request.\nError Message: " + e.getLocalizedMessage());
				} 

				String responseBody = "";
				
				/*
				 * If a message body was found, read it
				 */
				if (length > 0) {
					int read;
					try {
						while ((read = in.read()) != -1) {
							responseBody+= (char) read;
							if (responseBody.length() == length)
							break;
						}
					} catch (IOException e) {
						System.out.println("FrontEndServerProcessor failed to read message body.\nError Message: "+ e.getLocalizedMessage());
						HTTPServer.log.debug("FrontEndServerProcessor failed to read message body.\nError Message: "+ e.getLocalizedMessage());
					}
				}
				
				System.out.println("FrontEndServerProcessor RECEIVED BODY " + responseBody);
				HTTPServer.log.debug("FrontEndServerProcessor RECEIVED BODY " + responseBody);
				response.put("body", responseBody);
					
				out.flush();

				out.close();
				in.close();

			} catch (UnknownHostException e) {
				response.put("error", "FrontEndServerProcessor UnknownHostException");

				System.out.println("\nFrontEndServerProcessor UnknownHostException\nError Message: " + e.getLocalizedMessage() + "\n");
				HTTPServer.log.debug("\nFrontEndServerProcessor UnknownHostException\nError Message: " + e.getLocalizedMessage() + "\n");
			} catch (IOException e) {
				response.put("error", "nFrontEndServerProcessor IOException");

				System.out.println("\nFrontEndServerProcessor IOException\nError Message: " + e.getLocalizedMessage() + "\n");
				HTTPServer.log.debug("\nFrontEndServerProcessor IOException\nError Message: " + e.getLocalizedMessage() + "\n");
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					response.put("error", "FrontEndServerProcessor IOException closing socket");

					System.out.println("\nFrontEndServerProcessor IOException closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
					HTTPServer.log.debug("\nFrontEndServerProcessor IOException closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
				} catch (Exception e){
					response.put("error", "FrontEndServerProcessor Exception closing socket");

					System.out.println("\nFrontEndServerProcessor Exception closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
					HTTPServer.log.debug("\nFrontEndServerProcessor Exception closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
				}
			}
						
			if (!response.containsKey("erorr")){
				System.out.println(response);
				
				HTTPRequestLine tempRequestLine = new HTTPRequestLine();
				String body2 = (String) response.get("body");

				boolean correctlyFormatted = tempRequestLine.setBodyForBackEndDiscoverySend(body2);
				
				if (correctlyFormatted){
					String newPrimaryIP =  (String) tempRequestLine.getValueFromParam("IP");								
					String newPrimaryPort =  "" + tempRequestLine.getValueFromParam("Port");								
					System.out.println("New Primary at " + newPrimaryIP + " " + newPrimaryPort);
					
					this.backEndIP = newPrimaryIP;
					this.backEndPort = Integer.parseInt(newPrimaryPort);
				}
			}
	}
}
