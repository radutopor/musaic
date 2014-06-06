package musaic.musicEntities;

import java.io.*;
import java.net.URLEncoder;
import org.json.*;
import musaic.mediaSources.MediaSources;
import musaic.webAPIs.*;
import musaic.webAPIs.EchoNestQuery.EchoNestException;

public class Song implements MusicEntity
{
	private static final long serialVersionUID = -2206314726210739285L;
	
	private Artist artist;
	private String title;
	private String EchoNestId;
	private int duration = -1;
	
	public MediaSources mediaSources = new MediaSources(this);
	
	public Song(String artistName, String EchoNestArtistId, String title, String EchoNestId, int duration)
	{
		this.artist = new Artist(artistName, EchoNestArtistId);
		this.title = title;
		this.EchoNestId = EchoNestId;
		this.duration = duration;
	}
	
	public Song(String artistName, String EchoNestArtistId, String title, String EchoNestId)
	{
		this.artist = new Artist(artistName, EchoNestArtistId);
		this.title = title;
		this.EchoNestId = EchoNestId;
	}
	
	public Song(String artistName, String title)
	{
		this.artist = new Artist(artistName);
		this.title = title;
	}
	
	public Song(String EchoNestId)
	{
		this.EchoNestId = EchoNestId;
	}
	
	public void fillInEchoNestInfo() throws IOException, EchoNestException
	{
		if (artist == null || title == null)
		{
			JSONObject songProfile = null;
			try {
				songProfile = EchoNestQuery.get("song", "profile", "id="+EchoNestId + "&bucket=audio_summary");
			} catch (EchoNestException e) {}
			
			JSONObject song = songProfile.optJSONArray("songs").optJSONObject(0);
			
			artist = new Artist(song.optString("artist_name"), song.optString("artist_id"));
			title = song.optString("title");
			duration = song.optJSONObject("audio_summary").optInt("duration");
		}
		else if (EchoNestId == null)
		{
			String artistName = URLEncoder.encode(artist.getName(), "UTF-8");
			String title = URLEncoder.encode(this.title, "UTF-8");
			
			JSONObject songSearch = null;
			try {
				songSearch = EchoNestQuery.get("song", "search", "artist="+artistName + "&title="+title + "&results=1" + "&bucket=audio_summary");
			} catch (EchoNestException e) {}
			
			JSONArray songs = songSearch.optJSONArray("songs");
			if (songs.length() == 0)
				throw new EchoNestException(6, "No items found.");
			
			JSONObject song = songs.optJSONObject(0);
			
			artist = new Artist(song.optString("artist_name"), song.optString("artist_id"));
			EchoNestId = song.optString("id");
			duration = song.optJSONObject("audio_summary").optInt("duration");
		}
		else if (duration == -1)
		{
			JSONObject songProfile = null;
			try {
				songProfile = EchoNestQuery.get("song", "profile", "id="+EchoNestId + "&bucket=audio_summary");
			} catch (EchoNestException e) {}

			duration = songProfile.optJSONArray("songs").optJSONObject(0).optJSONObject("audio_summary").optInt("duration");
		}
	}

	public Artist getArtist()
	{
		if (artist == null)
			try {
				fillInEchoNestInfo();
			} catch (Exception e) {}
		
		return artist;
	}
	
	public String getTitle()
	{
		if (title == null)
			try {
				fillInEchoNestInfo();
			} catch (Exception e) {}
		
		return title;
	}
	
	public String getEchoNestId()
	{
		if (EchoNestId == null)
			try {
				fillInEchoNestInfo();
			} catch (Exception e) {}
		
		return EchoNestId;
	}
	
	public int getDuration()
	{
		if (duration == 0)
			try {
				fillInEchoNestInfo();
			} catch (Exception e) {}
		
		return duration;
	}
	
	public String toString()
	{
		return artist.toString() + " - " + getTitle();
	}

	public boolean equals(Object songObj)
	{
		if (!(songObj instanceof Song))
			return false;

		Song song = (Song)songObj;
		return (artist.equals(song.getArtist()) && title.equalsIgnoreCase(song.getTitle()));
	}
}