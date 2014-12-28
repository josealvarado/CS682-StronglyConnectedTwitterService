import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class BackEndServerTester {
	
	/**
	 * POST Example
	 * curl -H "Content-Type: application/json" -d '{"tweet": "#hello i am a #tweet", "hashtags":["hello", "tweet"]}' http://localhost:9002/tweets
	 */
	
	/**
	 * BackEndServerTester variables: backEndServerIP, backEndServerPORT, input
	 */
	private String backEndServerIP;
	private int backEndServerPORT;
	private ArrayList<HashMap<String, Object>> input;
	
	/**
	 * BackEndServerTester constructor
	 * @param backEndServerIP
	 * @param backEndServerPORT
	 */
	public BackEndServerTester(String backEndServerIP, int backEndServerPORT){
		this.backEndServerIP = backEndServerIP;
		this.backEndServerPORT = backEndServerPORT;
	}
	
	/**
	 * Initialize the test cases
	 */
	private void initializeTests(){
		input = new ArrayList<HashMap<String, Object>>();
		
		/*
		 * POST Request 1
		 */
		HashMap<String, Object> info = new HashMap<String, Object>();
		info.put("request", "POST /tweets HTTP/1.1");
		
		HashMap<String, Object> infoBody = new HashMap<String, Object>();
		infoBody.put("tweet", "#hello i am a #tweet");
		ArrayList<String> hashtags = new ArrayList<String>();
		hashtags.add("hello");
		hashtags.add("tweet");
		infoBody.put("hashtags", hashtags);
		
		info.put("body", infoBody);
		
		input.add(info);
		
		/*
		 * POST Request 2
		 */
		HashMap<String, Object> info2 = new HashMap<String, Object>();
		info2.put("request", "POST /tweets HTTP/1.1");
		
		HashMap<String, Object> infoBody2 = new HashMap<String, Object>();
		infoBody2.put("tweet", "#hello i am a #tweet #testing");
		ArrayList<String> hashtags2 = new ArrayList<String>();
		hashtags2.add("hello");
		hashtags2.add("tweet");
		hashtags2.add("testing");
		infoBody2.put("hashtags", hashtags2);
		
		info2.put("body", infoBody2);
		
		input.add(info2);
		
		/*
		 * GET Request 1
		 */
		HashMap<String, Object> info3 = new HashMap<String, Object>();
		info3.put("request", "GET /tweets?q=hello&v=0 HTTP/1.1");
		
		input.add(info3);
		
		/*
		 * GET Request 1
		 */
		HashMap<String, Object> info4 = new HashMap<String, Object>();
		info4.put("request", "GET /tweets?q=testing&v=0 HTTP/1.1");
		
		input.add(info4);
	}
	
	/**
	 * Run a test case very second
	 */
	public void startServer(){
		
		initializeTests();
		
		ExecutorService executor = Executors.newFixedThreadPool(100);		

		while(true){
			
			executor.execute(new TESTING());	

			try {
			    Thread.sleep(2000);                 //1000 milliseconds is one second.
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
		}
	}
	
	/**
	 * Private TESTING class
	 * @author josealvarado
	 *
	 */
	private class TESTING implements Runnable{

		/**
		 * Run the thread
		 */
		@Override
		public void run() {

			Socket socket = null;
			DataOutputStream out = null;
			DataInputStream in = null;
					
			HashMap response = new HashMap();
			
			try {
				/**
				 * Establish connection
				 */
				socket = new Socket(backEndServerIP, backEndServerPORT);
				out = new DataOutputStream(socket.getOutputStream());
				in = new DataInputStream(socket.getInputStream());
			
				System.out.println("Connected to BackEndServer with IP " + backEndServerIP + " PORT " + backEndServerPORT);

				/**
				 * Write to BackEndServer
				 */
				int num = (int)(Math.random()*4);
				
				HashMap<String, Object> test = input.get(num);
				
				String jsonText = JSONValue.toJSONString(test.get("body"));
				
				String request = (String) test.get("request");
				String body = "" + jsonText + " ";
			
				out.writeBytes(request);
				out.writeBytes("\n");
				
				out.writeBytes("Content-Length: " + body.getBytes().length);
				out.writeBytes("\n");
				out.writeBytes("\n");

				out.writeBytes(body);
				out.writeBytes("\n");
				
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
						 * Last line of request message
						 * Header is a blank line
						 * Quit while loop when last line of header is reached
						 */
						if (line.equals("") || line.startsWith("STOP")) {
							System.out.println("BackEndServerTester STOP READING");
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
						
						System.out.println("BackedtEndServerTester RECEIVED: " + line);
						HTTPServer.log.debug("BackedtEndServerTester RECEIVED: " + line);
					}
				} catch (NumberFormatException | IOException e) {
					response.put("error", "BackedtEndServerTester failed to read request");
					System.out.println("BackedtEndServerTester failed to read request.\nError Message: " + e.getLocalizedMessage());
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
						System.out.println("BackedtEndServerTester IOException failed to read message body.\nError Message: "+ e.getLocalizedMessage());
					}
				}
				
				System.out.println("BackedtEndServerTester RECEIVED BODY " + responseBody);
				response.put("body", responseBody);
				
				out.flush();

				out.close();
				in.close();

			} catch (UnknownHostException e) {
				response.put("error", "BackedtEndServerTester UnknownHostException");

				System.out.println("\nBackedtEndServerTester UnknownHostException\nError Message: " + e.getLocalizedMessage() + "\n");
			} catch (IOException e) {
				response.put("error", "nBackedtEndServerTester IOException");

				System.out.println("\nBackedtEndServerTester IOException\nError Message: " + e.getLocalizedMessage() + "\n");
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					response.put("error", "BackedtEndServerTester IOException closing socket");

					System.out.println("\nBackedtEndServerTester IOException closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
				} catch (Exception e){
					response.put("error", "BackedtEndServerTester Exception closing socket");

					System.out.println("\nBackedtEndServerTester Exception closing socket\nError Message: " + e.getLocalizedMessage() + "\n");
				}
			}
			
			String status = (String) response.get("response");
			String responseBody2 = (String) response.get("body");
			
			System.out.println("RESPONSE Header: " + status);
			System.out.println("RESPONSE Body: " + responseBody2);
		}
		
	}
}
