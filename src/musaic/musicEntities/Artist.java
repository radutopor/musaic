package musaic.musicEntities;

import java.io.*;
import java.net.URLEncoder;
import org.json.*;
import musaic.webAPIs.EchoNestQuery;
import musaic.webAPIs.EchoNestQuery.EchoNestException;

public class Artist implements MusicEntity
{
	private static final long serialVersionUID = -2406423644018969138L;
	
	private String name;
	private String EchoNestId;
	
	public Artist(String name, String EchoNestId)
	{
		this.name = name;
		this.EchoNestId = EchoNestId;
	}
	
	public Artist(String nameOrEchoNestId)
	{
		if (nameOrEchoNestId.startsWith("AR") && nameOrEchoNestId.length()==18 && !nameOrEchoNestId.contains(" "))
			this.EchoNestId = nameOrEchoNestId;
		else
			this.name = nameOrEchoNestId;
	}
	
	public void fillInEchoNestInfo() throws IOException, EchoNestException
	{
		if (name == null)
		{
			JSONObject artistProfile = null;
			try {
				artistProfile = EchoNestQuery.get("artist", "profile", "id="+EchoNestId);
			} catch (EchoNestException e) {}
			
			name = artistProfile.optJSONObject("artist").optString("name");
		}
		else if (EchoNestId == null)
		{
			String name = URLEncoder.encode(this.name, "UTF-8");
			JSONObject artistProfile = EchoNestQuery.get("artist", "profile", "name="+name);
			
			EchoNestId = artistProfile.optJSONObject("artist").optString("id");
		}
	}
	
	public String getName()
	{
		if (name == null)
			try {
				fillInEchoNestInfo();
			} catch (Exception e) {}
		
		return name;
	}
	
	public String getEchoNestId()
	{
		if (EchoNestId == null)
			try {
				fillInEchoNestInfo();
			} catch (Exception e) {}
		
		return EchoNestId;
	}
	
	public String toString()
	{
		return getName();
	}

	public boolean equals(Object artistObj)
	{
		if (!(artistObj instanceof Artist))
			return false;

		Artist artist = (Artist)artistObj;
		return EchoNestId.equals(artist.getEchoNestId());
	}
}