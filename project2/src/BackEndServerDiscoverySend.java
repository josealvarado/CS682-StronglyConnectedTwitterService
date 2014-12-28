import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.simple.JSONValue;


public class BackEndServerDiscoverySend  implements Runnable{

	private String primaryIP;
	private int primaryPort;
	
	private String status;
	
	private Socket clientSocket;
	
	private String discoveryIP;
	private int discoverySendPort;
	private int discoveryReceivePort;

	private BackEndServerDatabase database;
		
	public BackEndServerDiscoverySend(String primaryIP, int primaryPort, String status, String IP, int discoverySendPort, int discoveryReceivePort, BackEndServerDatabase database){
		this.primaryIP = primaryIP;
		this.primaryPort = primaryPort;
		this.status = status;
		this.discoveryIP = IP;
		this.discoverySendPort = discoverySendPort;
		this.discoveryReceivePort = discoveryReceivePort;
		this.database = database;	
		
		
	}
	
	@Override
	public void run(){
		Socket socket = null;
		DataOutputStream out = null;
		DataInputStream in = null;
		
		HashMap response = null;	
		
		int allowedFailedAttempts = 2;
		int failedAttempts = 0;
	
		while(true){
			System.out.println("HERE STATUS = " + status);
			
			// Sleep for 5 second
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				System.out.println("\nFrontEndServerProcessor UnknownHostException\nError Message: " + e1.getLocalizedMessage() + "\n");
				HTTPServer.log.debug("\nFrontEndServerProcessor UnknownHostException\nError Message: " + e1.getLocalizedMessage() + "\n");
			}
			
			
			
			
			if (status.equals("SECONDARY")){
				
//				int maxAttempts = 5;
//				int countAttempts = 0;
//				
//				do{
					
					System.out.println("I am secondary");
					System.out.println("Connecting to Primary Server with IP " + primaryIP + " PORT " + primaryPort);
					HTTPServer.log.debug("Connecting to Primary Server with IP " + primaryIP + " PORT " + primaryPort);
					
					response = new HashMap();	
					
					try {
						/**
						 * Establish connection
						 */
											
						socket = new Socket(primaryIP, primaryPort);
						out = new DataOutputStream(socket.getOutputStream());
						in = new DataInputStream(socket.getInputStream());
					
						System.out.println("Connected to Primary Server with IP " + primaryIP + " PORT " + primaryPort);
						HTTPServer.log.debug("Connected to Primary Server with IP " + primaryIP + " PORT " + primaryPort);

						
						
						HashMap<String, Object> input = new HashMap<String, Object>();
						input.put("request", "GET /tweets HTTP/1.1");
						
						HashMap<String, Object> infoBody = new HashMap<String, Object>();
						System.out.println("SENDING THIS VERSION #" + this.database.getDatabaseVersion());
						infoBody.put("version", this.database.getDatabaseVersion());
						infoBody.put("IP", this.discoveryIP);
						infoBody.put("PORT", ""+this.discoveryReceivePort);
						
						input.put("body", infoBody);
						
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
						
						System.out.println("Sending keep alive to Primary Server ...");
						HTTPServer.log.debug("Sending keep alive to Primary Server ...");
						
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
					
						failedAttempts++;
					
					} catch (IOException e) {
						response.put("error", "FrontEndServerProcessor IOException");

						System.out.println("\nFrontEndServerProcessor IOException\nError Message: " + e.getLocalizedMessage() + "\n");
						HTTPServer.log.debug("\nFrontEndServerProcessor IOException\nError Message: " + e.getLocalizedMessage() + "\n");
					
						failedAttempts++;
					
					} finally {
						try {
							socket.close();
						} catch (IOException e) {
							response.put("error", "FrontEndServerProcessor IOException closing socket");

							System.out.println("\nFrontEndServerProcessor IOException closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
							HTTPServer.log.debug("\nFrontEndServerProcessor IOException closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
						} catch (Exception e){
							
							System.out.println(response);
							
							response.put("error", "FrontEndServerProcessor Exception closing socket");

							System.out.println("\nFrontEndServerProcessor Exception closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
							HTTPServer.log.debug("\nFrontEndServerProcessor Exception closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
						}
					}
					
					
//					countAttempts++;
//					
//					System.out.println(response);
//					if (response.containsKey("error")){
//						connectToDiscoveryServer();
//						System.out.println("Failed");
//					} else {
//						System.out.println("Success");
//					}
//					
//				} while(countAttempts< maxAttempts && response.containsKey("error"));
				
				
				
				System.out.println("Handle this response from the Primary Server");
				System.out.println(response);
				
				System.out.println("ADD primary List of secondaries to my list of secondaries, but ignore myself");
				
				if (!response.containsKey("error")){
					System.out.println("ADD");
					String processBody = (String) response.get("body");
					System.out.println("BODY: " + processBody);
					
					HTTPRequestLine tempRequestBody = new HTTPRequestLine();
					
					boolean formattedCorrectly = tempRequestBody.setBodyForBackEndDiscoverySend(processBody);
					
					if (formattedCorrectly){
						ArrayList<ArrayList<String>> secondaryList = (ArrayList<ArrayList<String>>)tempRequestBody.getValueFromParam("secondaries");
						
						if (secondaryList != null){
							for (ArrayList<String> second: secondaryList){
								System.out.println("IP: " + second.get(0) + ", PORT: " + second.get(1));
								database.addSecondary(second.get(0), second.get(1));
							}
						}
						
						
						ArrayList<HashMap> logs = (ArrayList<HashMap>) tempRequestBody.getValueFromParam("data");
						if (logs != null){
							int count = 1;
							for(HashMap log: logs){
								System.out.println("COUNT: " + count);
								ArrayList<String> hashtags = (ArrayList<String>) log.get("hashtags");
								String tweet = (String) log.get("tweet");
								
								this.database.addTweet(hashtags, tweet);
							}
						} else {
							System.out.println("SERVER IS UP TO DATE, NO DATA RECEIVED");
						}
					} else {
						System.out.println("THIS SHOULD NEVER HAPPEN");
					}
				}
				
				if (failedAttempts == allowedFailedAttempts){
					status = "DISCOVERY";
					failedAttempts = 0;
				}
				
			} else if (status.equals("DISCOVERY")){      
				System.out.println("PRIMARY SERVER FAILED! Attempting to establish a new primary");
				
				forwardToSecondaries();
				
			} else {
				System.out.println("I am a the primary");
				
				contactDiscoveryServer();
			}
		}
	}
	
	public void forwardToSecondaries(){
		
		System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
		
		ArrayList<ArrayList<String>> secondaries = this.database.getSecondaries();
		
		System.out.println("Known Secondaries: " + secondaries.size());
		
		ArrayList<ArrayList<Object>> secondaryResponses = new ArrayList<ArrayList<Object>>();
		
		if (secondaries.size() == 0){
			status = "PRIMARY";
			System.out.println("Notify disocvery server");
		} else {
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
					sendThis.put("ID", discoveryReceivePort);
					sendThis.put("Version", this.database.getDatabaseVersion());
					String jsonText = JSONValue.toJSONString(sendThis);
					
					String request = "POST /election HTTP/1.1";
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
					
					HTTPRequestLine tempRequestLine = new HTTPRequestLine();
					String body2 = (String) response.get("body");

					boolean correctlyFormatted = tempRequestLine.setBodyForBackEndDiscoverySend(body2);
					
					if (correctlyFormatted){
						String serverType =  (String) tempRequestLine.getValueFromParam("serverType");								
						System.out.println(serverType);
						
						ArrayList<Object> secondaryInfo = new ArrayList<Object>();
						secondaryInfo.add(serverType);
						secondaryInfo.add(secondaryIP);
						secondaryInfo.add(""+secondaryPORT);
						
						ArrayList<HashMap> logs = (ArrayList<HashMap>) tempRequestLine.getValueFromParam("data");

						if (logs != null){
							secondaryInfo.add(logs);
							secondaryInfo.add(""+logs.size());
						}
						
						secondaryResponses.add(secondaryInfo);
					}

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
			}
			
			boolean iAmTheNewPrimary = true;
			String newPrimaryIP = "";
			String newPrimaryPort = "";
			
			ArrayList<HashMap> addToDatabase = new ArrayList<HashMap>();
			int maxSize = -1;
			
			for (ArrayList<Object> secondaryInfo: secondaryResponses){
				String serverType = (String) secondaryInfo.get(0);
				System.out.println("Roles of other secondaries: " + serverType);
				
				if (serverType.equals("PRIMARY")){
					iAmTheNewPrimary = false;
					newPrimaryIP = (String) secondaryInfo.get(1);
					newPrimaryPort = (String) secondaryInfo.get(2);
					break;
				}
				
				int secondaryInfoSize = secondaryInfo.size();
				if (secondaryInfoSize > 3){
					int currentSize = Integer.parseInt(""+secondaryInfo.get(4));
					if (currentSize > maxSize){
						maxSize = currentSize;
						addToDatabase = (ArrayList<HashMap>) secondaryInfo.get(3);
					}
				}
			}
			
			if (maxSize != -1){
				////////////////////////////////////////////////
				System.out.println("INCONSISTENT DATABASES AFTER ELECTING A NEW PRIMARY. FIXING DBS");
				int count = 1;
				for(HashMap log: addToDatabase){
					System.out.println("COUNT: " + count);
					ArrayList<String> hashtags = (ArrayList<String>) log.get("hashtags");
					String tweet = (String) log.get("tweet");
					
					this.database.addTweet(hashtags, tweet);					
				}
			}
			
			if (iAmTheNewPrimary){
				status = "PRIMARY";
			} else {
				status = "SECONDARY";
				primaryIP = newPrimaryIP;
				primaryPort = Integer.parseInt(newPrimaryPort);
				this.database.setPrimaryInfo(newPrimaryIP, Integer.parseInt(newPrimaryPort));
			}
			this.database.setStatus(status);
		}
	}
	
	public void contactDiscoveryServer(){
		String discoveryIP2 = database.getDiscoveryIP();
		Integer discoveryPort2 = database.getDiscoveryPort();
		
		Socket socket = null;
		DataOutputStream out = null;
		DataInputStream in = null;
		
		HashMap response = new HashMap();
		
		System.out.println("Connecting to Discovery ...");
		System.out.println("Secondary IP: " + discoveryIP2);
		System.out.println("Secondary Port: " + discoveryPort2);
		HTTPServer.log.debug("Connecting to Discovery ...");
		
		try {
			/**
			 * Establish connection
			 */
			socket = new Socket(discoveryIP2, discoveryPort2);
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(socket.getInputStream());
		
			System.out.println("Connected to Secondary with IP " + discoveryIP2 + " PORT " + discoveryPort2);
			HTTPServer.log.debug("Connected to Secondary with IP " + discoveryIP2 + " PORT " + discoveryPort2);

			/**
			 * Write to BackEndServer
			 */
			HashMap<String, Object> sendThis = new HashMap<String, Object>();
			sendThis.put("IP", discoveryIP);
			sendThis.put("PORT", discoveryReceivePort);
			String jsonText = JSONValue.toJSONString(sendThis);
			
			String request = "POST /discovery HTTP/1.1";
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
				
				String disIP = this.database.getDiscoveryIP();
				Integer disPort = this.database.getDiscoveryPort();
				
				socket = new Socket(disIP, disPort);
				out = new DataOutputStream(socket.getOutputStream());
				in = new DataInputStream(socket.getInputStream());
			
				System.out.println("Connected to BackEndServer with IP " + disIP + " PORT " + disPort);
				HTTPServer.log.debug("Connected to BackEndServer with IP " + disIP + " PORT " + disPort);

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
					
					this.primaryIP = newPrimaryIP;
					this.primaryPort = Integer.parseInt(newPrimaryPort);
					
					this.database.setPrimaryInfo(newPrimaryIP, Integer.parseInt(newPrimaryPort));
				}
			}
	}
}
