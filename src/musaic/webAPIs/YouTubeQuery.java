package musaic.webAPIs;

import musaic.utils.HttpTools;
import org.json.*;
import java.io.IOException;

public class YouTubeQuery 				// clasa statica
{
	private static String API_URL = "http://gdata.youtube.com/feeds/api/videos";
	private static String STATIC_PARAMS = "?v=2&alt=json&";
	
	public static JSONObject get(String params) throws IOException
	{
		String queryUrl = API_URL + STATIC_PARAMS + params;
		try {
			return HttpTools.getJSONResp(queryUrl);
		} catch (JSONException e) {
			return null; }
	}
	
	public static JSONObject getForId(String Id, String params) throws IOException
	{
		String queryUrl = API_URL + "/"+Id+"/" + STATIC_PARAMS + params;
		try {
			return HttpTools.getJSONResp(queryUrl);
		} catch (JSONException e) {
			return null; }
	}
}