package musaic.utils;

import java.net.*;
import java.io.*;
import org.json.*;

public class HttpTools 			// clasa statica
{
	public static JSONObject getJSONResp(String urlString) throws MalformedURLException, IOException, JSONException
	{
		String responseString = getStringResp(urlString);
		JSONObject responseJSON = new JSONObject(responseString);
		return responseJSON;
	}
	
	public static JSONObject postJSONResp(String urlString, String contentType, byte[] postData) throws MalformedURLException, IOException, JSONException
	{
		String responseString = postStringResp(urlString, contentType, postData);
		JSONObject responseJSON = new JSONObject(responseString);
		return responseJSON;
	}
	
	public static String getStringResp(String urlString) throws MalformedURLException, IOException
	{
		URL url = new URL(urlString);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setDoInput(true);
		
		String responseString = readStream(httpConn.getInputStream());
		return responseString;
	}
	
	public static String postStringResp(String urlString, String contentType, byte[] postData) throws MalformedURLException, IOException
	{
		URL url = new URL(urlString);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		httpConn.setRequestProperty("Content-Type", contentType);
		httpConn.setDoOutput(true);
		httpConn.setDoInput(true);
		
		OutputStream output = httpConn.getOutputStream();
		output.write(postData);
		output.flush();
		output.close();
        
		String responseString = readStream(httpConn.getInputStream());
		return responseString;
	}
	
	private static String readStream(InputStream in) throws IOException
	{
		ByteArrayOutputStream responseBytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		
		int bytesRead;
		while ((bytesRead = in.read(buffer)) > 0)
			responseBytes.write(buffer, 0, bytesRead);

		in.close();

		String responseString = responseBytes.toString("UTF-8");
        return responseString;
	}
}