import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class BackEndServerProcessor implements Runnable{

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
	public BackEndServerProcessor(Socket clientSocket, BackEndServerDatabase database) {
		this.clientSocket = clientSocket;
		this.database = database;
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
							String delay = (String) requestLine.getValueFromParam("delay");
							ArrayList<String> hashtags = (ArrayList<String>)requestLine.getValueFromParam("hashtags");
							System.out.println("Tweet: " + tweet);
							
							/**
							 * Respond to POST request
							 */
							if (tweet != null && hashtags!= null){
								
								database.addTweet(hashtags, tweet);
								
								forwardToSecondaries(hashtags, tweet, delay);
								
								json.put("statusCode", "201");
								json.put("message", "POST Correctly formattted Created for valid request");
								responseBody = json.toJSONString();
								responseHeader = "HTTP/1.1 201 Created\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";								
							} else {
								json.put("message", "ERROR POST MISSING CORRECT KEYS");
								responseBody = json.toJSONString();
								responseHeader = "HTTP/1.1 400 Bad Request\n" + "Content-Length: " + responseBody.getBytes().length + "\n\n";
							}
						} else {
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
				
				/**
				 * Verify request URI
				 */
				if (uri.equals("/tweets")){
					
					/**
					 * Parse request body parameters
					 */
					String query = (String) requestLine.getValueFromParam("q");
					String queryVersion = (String) requestLine.getValueFromParam("v");
							
					
					/**
					 * Respond to GET request
					 */
					if (query != null && queryVersion != null){
												
						Integer databaseVersion = database.getHashTagVersion(query);
						
						System.out.println("BackEndServerProcessor databaseVersion: " + databaseVersion + " q: " + query);
						
						if (databaseVersion == Integer.parseInt(queryVersion)){		
							json.put("statusCode", "304");
							json.put("message", "GET Correctly Formatted");
							responseBody = json.toJSONString();
							responseHeader = "HTTP/1.1 304 Not Modified\n" + "Content-Length: " + 0 + "\n\n";
						} else {							
							ArrayList<String> queries = database.getTweet(query);
							
							HashMap json2 = new HashMap();
							json2.put("statusCode", "200");
							json2.put("message", "GET Correctly Formatted");
							json2.put("q", query);
							json2.put("v", databaseVersion);
							json2.put("tweets", queries);
							
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
	
	public void forwardToSecondaries(ArrayList<String> hashtags, String tweet, String delay){
		
		
		System.out.println("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM");
		
		
		ArrayList<ArrayList<String>> secondaries = this.database.getSecondaries();
		
		for(ArrayList<String> secondary: secondaries){
			String secondaryIP = secondary.get(0);
			Integer secondaryPORT = Integer.parseInt(secondary.get(1));
			
			Socket socket = null;
			DataOutputStream out = null;
			DataInputStream in = null;
			
			HashMap response = new HashMap();
			
			System.out.println("Connecting to Secondary ...");
			System.out.println("Secondary IP: " + secondaryIP);
			System.out.println("Secondary Port: " + secondaryPORT);
			HTTPServer.log.debug("Connecting to Secondary ...");
			
			try {
				/**
				 * Establish connection
				 */
				socket = new Socket(secondaryIP, secondaryPORT);
				out = new DataOutputStream(socket.getOutputStream());
				in = new DataInputStream(socket.getInputStream());
			
				System.out.println("Connected to Secondary with IP " + secondaryIP + " PORT " + secondaryPORT);
				HTTPServer.log.debug("Connected to Secondary with IP " + secondaryIP + " PORT " + secondaryPORT);

				/**
				 * Write to BackEndServer
				 */
				HashMap<String, Object> sendThis = new HashMap<String, Object>();
				sendThis.put("hashtags", hashtags);
				sendThis.put("tweet", tweet);
				String jsonText = JSONValue.toJSONString(sendThis);
				
				String request = "POST /tweets HTTP/1.1";
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
			System.out.println(response);
			
			if (delay != null){
				System.out.println("JUST FINISHED PROPAGATING POST TO A SEONDARY");
				System.out.println("JUST FINISHED PROPAGATING POST TO A SEONDARY");
				System.out.println("BREAK IT NOW BEFORE THE INFORMATION IN PROPATATED AGAIN");
				System.out.println("BREAK IT NOW BEFORE THE INFORMATION IN PROPATATED AGAIN");
				
				// Sleep for 5 second
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					System.out.println("\nFrontEndServerProcessor UnknownHostException\nError Message: " + e1.getLocalizedMessage() + "\n");
					HTTPServer.log.debug("\nFrontEndServerProcessor UnknownHostException\nError Message: " + e1.getLocalizedMessage() + "\n");
				}
			}
		}
	}
}
