import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class HTTPRequestLineParser {

	/**
	 * This method takes as input the Request-Line exactly as it is read from the socket.
	 * It returns a Java object of type HTTPRequestLine containing a Java representation of
	 * the line.
	 *
	 * The signature of this method may be modified to throw exceptions you feel are appropriate.
	 * The parameters and return type may not be modified.
	 *
	 * 
	 * @param line
	 * @return
	 */
	public static HTTPRequestLine parse (String line){
	    //A Request-Line is a METHOD followed by SPACE followed by URI followed by SPACE followed by VERSION
	    //A VERSION is 'HTTP/' followed by 1.0 or 1.1
	    //A URI is a '/' followed by PATH followed by optional '?' PARAMS 
	    //PARAMS are of the form key'='value'&'

		String[] params = line.split(" ");
		
		if (params.length != 3){
			System.out.println("HTTPRequestLine ERROR PARAM LENGTH SHOULD BE 3 NOT " + params.length);
			return null;
		}
		
		HTTPRequestLine httpRequest = new HTTPRequestLine();
		try {
			httpRequest.setMethod(URLDecoder.decode(params[0], "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("HTTPRequestLine UnsupportedEncodingException.\nError Message: " + e.getLocalizedMessage());
			return null;
		} catch (HTTPMethodException e) {
			System.out.println("HTTPRequestLine HTTPMethodException.\nError Message: " + e.getLocalizedMessage());
			return null;
		}

		try {
			httpRequest.setUri(URLDecoder.decode(params[1], "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("HTTPRequestLine UnsupportedEncodingException.\nError Message: " + e.getLocalizedMessage());
			return null;
		} catch (HTTPUriException e) {
			System.out.println("HTTPRequestLine HTTPUriException.\nError Message: " + e.getLocalizedMessage());
			return null;
		}

		try {
			httpRequest.setHttpverstion(URLDecoder.decode(params[2], "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("HTTPRequestLine UnsupportedEncodingException.\nError Message: " + e.getLocalizedMessage());
			return null;
		} catch (HTTPVersionException e) {
			System.out.println("HTTPRequestLine HTTPVersionException.\nError Message: " + e.getLocalizedMessage());
			return null;
		}
	
		return httpRequest;
	}	
	
}
