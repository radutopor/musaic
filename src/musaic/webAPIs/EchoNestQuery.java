package musaic.webAPIs;

import musaic.utils.*;
import java.io.IOException;
import org.json.*;

public class EchoNestQuery 				// clasa statica
{
	private static String API_URL = "http://developer.echonest.com/api/v4/";
	private static String API_KEY = "?api_key=ERFLBGNKOMHVWISP9&";
	
	public static JSONObject get(String Class, String method, String params) throws IOException, EchoNestException
	{
		String queryUrl = API_URL + Class + "/" + method + API_KEY + params;
		try {
			JSONObject response = HttpTools.getJSONResp(queryUrl);
		    return validateResponse(response);
		} catch (JSONException e) {
			return null; }
	}
	
	public static JSONObject post(String Class, String method, String params, String contentType, byte[] postData) throws IOException, EchoNestException
	{
		String postUrl = API_URL + Class + "/" + method + API_KEY + params;
		try {
			JSONObject response = HttpTools.postJSONResp(postUrl, contentType, postData);
			return validateResponse(response);
		} catch (JSONException e) {
			return null; }
	}
	
	private static JSONObject validateResponse(JSONObject response) throws EchoNestException
	{
	    response = response.optJSONObject("response");

	    JSONObject status = response.optJSONObject("status");
	    if (status.optInt("code") != 0)
	    	throw new EchoNestException(status.optInt("code"), status.optString("message"));
	    
	    response.remove("status");
	    return response;
	}
	
	public static class EchoNestException extends Exception
	{
		private static final long serialVersionUID = 632953562566461928L;
		private int code;
		private String message;
		
		public EchoNestException(int code, String message)
		{
			this.code = code;
			this.message = message;
		}
		
		public int getCode()
		{
			return code;
		}
		
		public String getMessage()
		{
			return message;
		}
	}
}