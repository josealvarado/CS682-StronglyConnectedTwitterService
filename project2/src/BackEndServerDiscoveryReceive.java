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

import org.json.simple.JSONValue;


public class BackEndServerDiscoveryReceive implements Runnable{

	/*
	 * Either PRIMARY OR SECONDARY
	 */
	private String status;
	
	private Socket clientSocket;
	private BackEndServerDatabase database;
	
	private String primaryIP;
	private int primaryPort;
	
	private String discoveryIP;
	private int discoveryReceive;
	
	private ArrayList<Socket> secondaryList;
	
	/**
	 * BackEndServerProcessor constructor
	 * @param clientSocket
	 * @param database
	 */
	public BackEndServerDiscoveryReceive(String primaryIP, int primaryPort, String status, String IP, int discoveryReceive, BackEndServerDatabase database) {
		this.primaryIP = primaryIP;
		this.primaryPort = primaryPort;
		this.status = status;
		this.discoveryIP = IP;
		this.discoveryReceive = discoveryReceive;
		
		this.database = database;
		secondaryList = new ArrayList<Socket>();
	}
	
	@Override
	public void run() {
		System.out.println("BackEndServer Discovery Service Started");
		
		ExecutorService executor = Executors.newFixedThreadPool(100);		
		ServerSocket serverSocket = null;
		InetAddress bindAddr = null;
		try {
			bindAddr = InetAddress.getByName(discoveryIP);
		} catch (UnknownHostException e1) {
			System.out.println("BackEndServer UnknownHostException unable to get IP " + discoveryIP);
			System.out.println("Error Message: " + e1.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServer UnknownHostException unable to get IP " + discoveryIP);
			HTTPServer.log.debug("Error Message: " + e1.getLocalizedMessage());
			return;
		}

		/**
		 * Bind Server with IP and PORT 
		 */
		try {
			serverSocket = new ServerSocket(discoveryReceive, 10000, bindAddr);			
		} catch (IOException e) {
			System.out.println("BackEndServer IOException unable to create ServerSocket with port " + discoveryReceive + " and IP " + discoveryIP);
			System.out.println("Error Message: " + e.getLocalizedMessage());
			HTTPServer.log.debug("BackEndServer IOException unable to create ServerSocket with port " + discoveryReceive + " and IP " + discoveryIP);
			HTTPServer.log.debug("Error Message: " + e.getLocalizedMessage());
			return;
		}
		
		/**
		 * BackEndServer waits for requests
		 * creates a new BackEndServerProcessor thread for each request
		 */
		while (true) {
		   	System.out.println("BackEndServer Discovery Service Waiting for request");
		   	HTTPServer.log.debug("BackEndServer Discovery Service Waiting for request");
		   	Socket clientSocket = null;
		   	try {
				clientSocket = serverSocket.accept();
			} catch (IOException e) {
				System.out.println("BackEndServer IOException unable to accept requests\nError Message: " + e.getLocalizedMessage());
				HTTPServer.log.debug("BackEndServer IOException unable to accept requests\nError Message: " + e.getLocalizedMessage());
			}
		    
			executor.execute(new BackEndServerDiscoveryReceiveProcessor(clientSocket, database));	
		}
	}
}
