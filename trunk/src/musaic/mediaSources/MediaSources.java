package musaic.mediaSources;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import musaic.musicEntities.Song;
import musaic.utils.HttpTools;
import musaic.webAPIs.YouTubeQuery;
import org.json.*;

public class MediaSources implements Serializable
{
	private static final long serialVersionUID = -2895015180043622459L;
	
	private Song song;
	private ArrayList<String> YouTubeIds = new ArrayList<String>();
	boolean firstQuery = false; boolean secondQuery = false;
	private String lastSource;
	
	public MediaSources(Song song)
	{
		this.song = song;
	}
	
	public String getNextSource() throws IOException, YouTubeException
	{
		if (!firstQuery)
		{
			String queryTerms = "q=" + URLEncoder.encode(song.getArtist().getName() + " " + song.getTitle(), "UTF-8");
			int durationTolerance = song.getDuration() / 10;
			int minDuration = song.getDuration() - durationTolerance, maxDuration = song.getDuration() + durationTolerance;
			String conditions = "[media:group/yt:duration/@seconds>"+minDuration+"%20and%20media:group/yt:duration/@seconds<"+maxDuration+"]";
			
			JSONObject YouTubeResponse = YouTubeQuery.get(queryTerms + "&fields=entry"+conditions+"(media:group(yt:videoid))" + "&max-results=50");
			YouTubeIds.addAll(extractIdsFromResponse(YouTubeResponse));
			firstQuery = true;
		}

		int seekPos;
		String response;
		do
		{
			if (YouTubeIds.isEmpty() && !secondQuery)
			{
				String queryTerms = "q=" + URLEncoder.encode(song.getArtist().getName() + " " + song.getTitle(), "UTF-8");
				JSONObject YouTubeResponse = YouTubeQuery.get(queryTerms + "&fields=entry(media:group(yt:videoid))" + "&max-results=50");
				YouTubeIds.addAll(extractIdsFromResponse(YouTubeResponse));
				secondQuery = true;
			}
			if (YouTubeIds.isEmpty())
				throw new YouTubeException("No source found for this song.");
			
	    	String videoPageUrl = "http://www.youtube.com/watch?nomobile=1&html5=True&v=" + YouTubeIds.get(0);
	    	YouTubeIds.remove(0);
	    	
			response = HttpTools.getStringResp(videoPageUrl);
	
			seekPos = response.lastIndexOf("url_encoded_fmt_stream_map");
			seekPos = response.indexOf("type=video%2Fmp4", seekPos);
		}
		while (seekPos == -1);			// No PLAYABLE source for this video

		seekPos = response.lastIndexOf("http", seekPos);
		int endPos = response.indexOf("\\u0026", seekPos);
		String encodedUrl = response.substring(seekPos, endPos);
		
		String YouTubeVideoSource = URLDecoder.decode(encodedUrl, "UTF-8");
		
		lastSource = YouTubeVideoSource;
    	return YouTubeVideoSource;
	}
	
	public String getLastSource()
	{
		return lastSource;
	}

	private static ArrayList<String> extractIdsFromResponse(JSONObject YouTubeResponse)
	{
		ArrayList<String> YouTubeIds = new ArrayList<String>();
		
		JSONObject feed = YouTubeResponse.optJSONObject("feed");
		if (feed.has("entry"))
		{
			JSONArray entries = feed.optJSONArray("entry");
			for (int i = 0; i < entries.length(); i++)
				YouTubeIds.add(entries.optJSONObject(i).optJSONObject("media$group").optJSONObject("yt$videoid").optString("$t"));
		}
		return YouTubeIds;
	}

	public static class YouTubeException extends Exception
	{
		private static final long serialVersionUID = 6056832024662738552L;
		private String message;
		
		public YouTubeException(String message)
		{
			this.message = message;
		}
		
		public String getMessage()
		{
			return message;
		}
	}
}
