import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class HTTPServer {
	
	static Logger log = Logger.getLogger(
			HTTPServer.class.getName());
	
	public static void main(String[] args) throws Exception {
		
		PropertyConfigurator.configure("log4j.properties");
			
		String fileName = "configuration.json";
		
		if(args.length > 0){
			fileName = args[0];
		}
		
		String configuration = "";		
		try {
			configuration = readFile(System.getProperty("user.dir") + "/" + fileName, StandardCharsets.UTF_8);
		} catch (IOException e) {
			System.out.println("HTTPServer IOException Reading keys.json\nError Message: " + e.getLocalizedMessage());
			log.debug("HTTPServer IOException Reading keys.json\nError Message: " + e.getLocalizedMessage());
		}

		Map jsonConfiguration = jsonStringToMap(configuration);
		
		String serverType = (String) jsonConfiguration.get("type");
		
		System.out.println("Server Type: " + serverType);
		
		if (serverType.equals("backend")){
			String serverIP = (String) jsonConfiguration.get("backendIP");
			String serverPort = (String) jsonConfiguration.get("backendPort");
			String primaryIP = (String) jsonConfiguration.get("primaryIP");
			String primaryPort = (String) jsonConfiguration.get("primaryPort");
			String discoverySend = (String) jsonConfiguration.get("discoverySend");
			String discoveryReceive = (String) jsonConfiguration.get("discoveryReceive");
			String discoveryIP = (String) jsonConfiguration.get("discoveryIP");
			String discoveryPort = (String) jsonConfiguration.get("discoveryPort");
			
			System.out.println("Backend IP: " + serverIP);
			System.out.println("Backend PORT: " + serverPort);
			System.out.println("Primary IP: " + primaryIP);
			System.out.println("Primary PORT: " + primaryPort);
			System.out.println("Discovery Send: " + discoverySend);
			System.out.println("Discovery Receive: " + discoveryReceive);
			
			BackEndServer backEndServer = new BackEndServer(serverIP, Integer.parseInt(serverPort), primaryIP, Integer.parseInt(primaryPort), Integer.parseInt(discoverySend), Integer.parseInt(discoveryReceive), discoveryIP, Integer.parseInt(discoveryPort));
			backEndServer.startServer();
		} else if (serverType.equals("frontend")){
			String serverIP = (String) jsonConfiguration.get("frontendIP");
			String serverPort = (String) jsonConfiguration.get("frontendPort");
			String backEndServerIP = (String) jsonConfiguration.get("backendIP");
			String backEndServerPORT = (String) jsonConfiguration.get("backendPort");
			String discoveryIP = (String) jsonConfiguration.get("discoveryIP");
			String discoveryPort = (String) jsonConfiguration.get("discoveryPort");
			
			System.out.println("Server IP: " + serverIP);
			System.out.println("Server PORT: " + serverPort);
			System.out.println("Server BACKEND IP: " + backEndServerIP);
			System.out.println("Server BACKEND PORT: " + backEndServerPORT);
			
			FrontEndServer frontEndServer = new FrontEndServer(serverIP, Integer.parseInt(serverPort), backEndServerIP, Integer.parseInt(backEndServerPORT), discoveryIP, Integer.parseInt(discoveryPort));
			frontEndServer.startServer();
		} else if (serverType.equals("discovery")){
			String serverIP = (String) jsonConfiguration.get("IP");
			String serverPort = (String) jsonConfiguration.get("PORT");
			
			System.out.println("Server IP: " + serverIP);
			System.out.println("Server PORT: " + serverPort);
			
			DiscoveryServer frontEndServer = new DiscoveryServer(serverIP, Integer.parseInt(serverPort));
			frontEndServer.startServer();
		} else if (serverType.equals("backendTester")){
			String serverIP = (String) jsonConfiguration.get("backendip");
			String serverPort = (String) jsonConfiguration.get("backendport");
			
			System.out.println("Server Type: " + serverIP);
			System.out.println("Server Type: " + serverPort);
			
			BackEndServerTester backEndServerTester = new BackEndServerTester(serverIP, Integer.parseInt(serverPort));
			backEndServerTester.startServer();
		} else if (serverType.equals("frontendTester")){
			String serverIP = (String) jsonConfiguration.get("backendip");
			String serverPort = (String) jsonConfiguration.get("backendport");
			
			System.out.println("Server Type: " + serverIP);
			System.out.println("Server Type: " + serverPort);
			
			FrontEndServerTester frontEndServerTester = new FrontEndServerTester(serverIP, Integer.parseInt(serverPort));
			frontEndServerTester.startServer();
		}
	}
	
	private static String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	private static Map jsonStringToMap(String jsonString){
		JSONParser parser = new JSONParser();

		ContainerFactory containerFactory = new ContainerFactory(){
		    public List creatArrayContainer() {
		    	return new LinkedList();
		    }

		    public Map createObjectContainer() {
		    	return new LinkedHashMap();
		    }
		    
		};
		
		Map jsonMap = null;
		try {
			jsonMap = (Map)parser.parse(jsonString, containerFactory);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return jsonMap;
	}
}

