import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DiscoveryServer {
	/**
	 * BackEndServer variables: IP, PORT, database
	 */
	private int PORT;
	private String IP;
	private BackEndServerDatabase serverList;
	
	/**
	 * BackEndServer constructor
	 * @param ip
	 * @param port
	 */
	public DiscoveryServer(String ip, int port){
		IP = ip;
		PORT = port;
		serverList = new BackEndServerDatabase();		
	}
	
	/**
	 * Start BackEndServer 
	 */
	public void startServer(){
		System.out.println("BackEndServer Started");
		
		ExecutorService executor = Executors.newFixedThreadPool(100);		
		ServerSocket serverSocket = null;
		InetAddress bindAddr = null;
		try {
			bindAddr = InetAddress.getByName(IP);
		} catch (UnknownHostException e1) {
			System.out.println("BackEndServer UnknownHostException unable to get IP " + IP);
			System.out.println("Error Message: " + e1.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServer UnknownHostException unable to get IP " + IP);
			HTTPServer.log.debug("Error Message: " + e1.getLocalizedMessage());
			return;
		}

		/**
		 * Bind Server with IP and PORT 
		 */
		try {
			serverSocket = new ServerSocket(PORT, 10000, bindAddr);			
		} catch (IOException e) {
			System.out.println("BackEndServer IOException unable to create ServerSocket with port " + PORT + " and IP " + IP);
			System.out.println("Error Message: " + e.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServer IOException unable to create ServerSocket with port " + PORT + " and IP " + IP);
			HTTPServer.log.debug("Error Message: " + e.getLocalizedMessage());
			return;
		}

		/**
		 * BackEndServer waits for requests
		 * creates a new BackEndServerProcessor thread for each request
		 */
		while (true) {
		   	System.out.println("BackEndServer Waiting for request");
		   	HTTPServer.log.debug("BackEndServer Waiting for request");
		   	Socket clientSocket = null;
		   	try {
				clientSocket = serverSocket.accept();
			} catch (IOException e) {
				System.out.println("BackEndServer IOException unable to accept requests\nError Message: " + e.getLocalizedMessage());
				HTTPServer.log.debug("BackEndServer IOException unable to accept requests\nError Message: " + e.getLocalizedMessage());
			}
		    
			executor.execute(new DiscoveryServerProcessor(clientSocket, serverList));	
		}
	}
}
