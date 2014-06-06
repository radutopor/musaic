package musaic.playlist;

import java.io.*;
import java.util.*;
import org.json.*;
import musaic.musicEntities.*;
import musaic.musicSearch.SearchResult;
import musaic.webAPIs.EchoNestQuery;
import musaic.webAPIs.EchoNestQuery.EchoNestException;

public class EchoNestPlaylist extends Playlist
{
	private static final long serialVersionUID = -4841160698250331159L;

	public static enum SessionType
	{
		SONG_RADIO,
		ARTIST,
		ARTIST_DESCRIPTION
	}
	private SessionType sessionType;
	private String sessionId;
	
	private SearchResult seed;
	private String artistConstraintsString = "";
	private String styleMoodConstraintsString = "";
	
	public EchoNestPlaylist(SearchResult seed, int numOptions, PlaylistListener listener)
	{
		this.seed = seed;
		this.numOptions = numOptions;
		this.listener = listener;
	}
	
	public void initPlaylist() throws IOException
	{
		JSONObject dynamicPlaylistCreate = null;
		try {
			switch (seed.getResultType())
			{
				case SONG:
					sessionType = SessionType.SONG_RADIO;
					Song seedSong = (Song)seed.getResult();
	
					dynamicPlaylistCreate = EchoNestQuery.get("playlist", "dynamic/create", "type=song-radio" + "&song_id="+seedSong.getEchoNestId() +
							"&song_id=-"+seedSong.getEchoNestId() + 
							"&distribution=focused" + "&variety=0" + "&bucket=audio_summary");
					
					sessionId = dynamicPlaylistCreate.optString("session_id");
					changeCurrentSong(seedSong);
					break;
	
				case ARTIST_GROUP:
					sessionType = SessionType.ARTIST;
					Artist[] seedArtists = (Artist[])seed.getResult();
					addConstraints(seedArtists);
					
					dynamicPlaylistCreate = EchoNestQuery.get("playlist", "dynamic/create", "type=artist" + artistConstraintsString +
							"&distribution=focused" + "&variety=0" + "&bucket=audio_summary");
					
					sessionId = dynamicPlaylistCreate.optString("session_id");
					retrieveNewCurrentSong();
					break;
					
				case STYLEMOOD_GROUP:
					sessionType = SessionType.ARTIST_DESCRIPTION;
					StyleMood[] seedStylesMoods = (StyleMood[])seed.getResult();
					addConstraints(seedStylesMoods);
					
					dynamicPlaylistCreate = EchoNestQuery.get("playlist", "dynamic/create", "type=artist-description" + styleMoodConstraintsString +
							"&distribution=focused" + "&variety=0" + "&bucket=audio_summary");
					
					sessionId = dynamicPlaylistCreate.optString("session_id");
					retrieveNewCurrentSong();
			}
		} catch (EchoNestException e) {}
		
		listener.onPlaylistConstraintsChanged();
		retrieveNewOptions();
		makeCurrentPrevious();
	}

	public void steerPlaylist(SearchResult driver) throws IOException
	{
		switch (driver.getResultType())
		{
			case SONG:
				Song song = (Song)driver.getResult();
				if (!currentSong.equals(song))
				{
					changeCurrentSong(song);
					reInitPlaylist(SessionType.SONG_RADIO);
				}
				break;
			case ARTIST_GROUP:
				if (sessionType == SessionType.ARTIST_DESCRIPTION)
				{
					styleMoodConstraints.clear();
					styleMoodConstraintsString = "";
				}
				if (!driver.addToPlaylist)
				{
					artistConstraints.clear();
					artistConstraintsString = "";
				}
				Artist[] artists = (Artist[])driver.getResult();
				if (addConstraints(artists))
					reInitPlaylist(SessionType.ARTIST);
				break;
			case STYLEMOOD_GROUP:
				if (sessionType == SessionType.ARTIST)
				{
					artistConstraints.clear();
					artistConstraintsString = "";
				}
				if (!driver.addToPlaylist)
				{
					styleMoodConstraints.clear();
					styleMoodConstraintsString = "";
				}
				StyleMood[] stylesMoods = (StyleMood[])driver.getResult();
				if (addConstraints(stylesMoods))
					reInitPlaylist(SessionType.ARTIST_DESCRIPTION);
		}
	}
	
	private void reInitPlaylist(SessionType newSessionType) throws IOException
	{
		try {
			switch (newSessionType)
			{
				case SONG_RADIO:
					EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId + 
							"&unplay_song="+firstOption.getEchoNestId() + "&play_song="+currentSong.getEchoNestId());
					if(sessionType != SessionType.SONG_RADIO)
					{
						artistConstraints.clear(); styleMoodConstraints.clear();
						artistConstraintsString = styleMoodConstraintsString = "";
						
						EchoNestQuery.get("playlist", "dynamic/restart", "session_id="+sessionId + "&type=song-radio" + "&song_id="+currentSong.getEchoNestId() +
								"&distribution=focused" + "&variety=0" + "&bucket=audio_summary");
					}
					break;
				case ARTIST:
					EchoNestQuery.get("playlist", "dynamic/restart", "session_id="+sessionId + "&type=artist" + artistConstraintsString +
							"&distribution=focused" + "&variety=0" + "&bucket=audio_summary");
					if (!artistConstraints.contains(currentSong.getArtist()))
					{
						EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId +
								"&unplay_song="+firstOption.getEchoNestId() + "&unplay_song="+currentSong.getEchoNestId());
						retrieveNewCurrentSong();
					}
					else
						EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId + "&unplay_song="+firstOption.getEchoNestId());
					break;
				case ARTIST_DESCRIPTION:
					EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId +
							"&unplay_song="+firstOption.getEchoNestId() + "&unplay_song="+currentSong.getEchoNestId());
					EchoNestQuery.get("playlist", "dynamic/restart", "session_id="+sessionId + "&type=artist-description" + styleMoodConstraintsString +
							"&distribution=focused" + "&variety=0" + "&bucket=audio_summary");
					retrieveNewCurrentSong();
			}
		} catch (EchoNestException e) {}
		
		sessionType = newSessionType;
		listener.onPlaylistConstraintsChanged();
		retrieveNewOptions();
		makeCurrentPrevious();
	}
	
	public void removePlaylistConstraint(int constraintIndex) throws IOException
	{
		@SuppressWarnings("unchecked")
		ArrayList<MusicEntity> constraintsList = (ArrayList<MusicEntity>)((sessionType == SessionType.ARTIST) ? artistConstraints : styleMoodConstraints);
		
		if (constraintIndex < 0 || constraintIndex >= constraintsList.size())
			return;
		
		MusicEntity constraint = constraintsList.get(constraintIndex);
		if (sessionType == SessionType.ARTIST)
			artistConstraintsString = artistConstraintsString.replace("&artist_id="+((Artist)constraint).getEchoNestId(), "");
		else
			styleMoodConstraintsString = styleMoodConstraintsString.replaceAll("&(style|mood|description)="+((StyleMood)constraint).getTerm(), "");

		constraintsList.remove(constraintIndex);
		listener.onPlaylistConstraintsChanged();
		
		if (!constraintsList.isEmpty())
			reInitPlaylist(sessionType);
		else
			reInitPlaylist(SessionType.SONG_RADIO);
	}
	
	// aux methods
	
	private boolean addConstraints(Artist[] artists)
	{
		boolean seedArtistsListChanged = false;
		for (int i = 0; (i < artists.length) && (artistConstraints.size() < 5); i++)
		{
			if (artistConstraints.contains(artists[i]))
				continue;

			artistConstraints.add(artists[i]);
			artistConstraintsString += "&artist_id="+artists[i].getEchoNestId();
			seedArtistsListChanged = true;
		}
		return seedArtistsListChanged;
	}
	
	private boolean addConstraints(StyleMood[] stylesMoods)
	{
		boolean stylesMoodsListChanged = false;
		for (StyleMood styleMood : stylesMoods)
		{
			if (styleMoodConstraints.contains(styleMood))
				continue;
			
			styleMoodConstraints.add(styleMood);
			switch (styleMood.getTermType())
			{
				case STYLE:
					styleMoodConstraintsString += "&style="+styleMood.getTermUrlEnc();
					break;
				case MOOD:
					styleMoodConstraintsString += "&mood="+styleMood.getTermUrlEnc();
					break;
				case DESCRIPTION:
					styleMoodConstraintsString += "&description="+styleMood.getTermUrlEnc();
			}
			stylesMoodsListChanged = true;
		}
		return stylesMoodsListChanged;
	}

	// import songs methods

	private Song retrieveDynamicNext() throws IOException
	{
		JSONObject dynamicPlaylistNext = null;
		try {
			dynamicPlaylistNext = EchoNestQuery.get("playlist", "dynamic/next", "session_id="+sessionId);
		} catch (EchoNestException e) {}

		JSONObject song = dynamicPlaylistNext.optJSONArray("songs").optJSONObject(0);
		int duration = song.optJSONObject("audio_summary").optInt("duration");
		return new Song(song.optString("artist_name"), song.optString("artist_id"), song.optString("title"), song.optString("id"), duration);
	}
	
	protected void retrieveNewCurrentSong()
	{
		try {
			changeCurrentSong(retrieveDynamicNext());
		} catch (IOException e) {}
	}
	
	protected void retrieveNewOptions() throws IOException
	{
		// adu prima optiune din playlist-ul dinamic
		firstOption = retrieveDynamicNext();
		currentOptions.clear();
		currentOptions.add(firstOption);
		
		if (numOptions == 1)
			return;
		
		// adu restul optiunilor pana la numOptions dintr-un playlist static
		ArrayList<Song> excludes = new ArrayList<Song>();
		excludes.add(firstOption);
		if (previousSong != null)
			excludes.add(previousSong);
		
		ArrayList<Song> newOptions = retrieveMoreOptions(currentSong, excludes, numOptions-1);
		currentOptions.addAll(newOptions);

		listener.onCurrentOptionsChanged();
	}

	private static String[] ArtistPick = {"song_hotttnesss-desc", "song_hotttnesss-asc", "tempo-desc", "tempo-asc", "duration-desc", "duration-asc", "loudness-desc", "loudness-asc", "mode-desc", "mode-asc", "key-desc", "key-asc"};
	private static Random random = new Random();
	protected ArrayList<Song> retrieveMoreOptions(Song seed, ArrayList<Song> excludes, int numOptions) throws IOException
	{
		String commonParams = "&distribution=focused" + "&variety=1" + "&results="+(excludes.size()+1+numOptions) + "&bucket=audio_summary";
		JSONObject staticPlaylist = null;
		try {
			switch (sessionType)
			{
				case SONG_RADIO:
					staticPlaylist = EchoNestQuery.get("playlist", "static", "type=song-radio" + "&song_id="+seed.getEchoNestId() + commonParams);
					break;
				case ARTIST:
					staticPlaylist = EchoNestQuery.get("playlist", "static", "type=artist" + artistConstraintsString + "&artist_pick="+ArtistPick[random.nextInt(12)] + commonParams);
					break;
				case ARTIST_DESCRIPTION:
					staticPlaylist = EchoNestQuery.get("playlist", "static", "type=artist-description" + styleMoodConstraintsString + commonParams);
					break;
			}
		} catch (EchoNestException e) {}
		
		JSONArray songs = staticPlaylist.optJSONArray("songs");
		ArrayList<Song> newOptions = new ArrayList<Song>();
		
		for (int i = 0; newOptions.size() < numOptions; i++)
		{
			JSONObject song = songs.optJSONObject(i);
			int duration = song.optJSONObject("audio_summary").optInt("duration");
			Song songObj = new Song(song.optString("artist_name"), song.optString("artist_id"), song.optString("title"), song.optString("id"), duration);
			
			if (!songObj.equals(seed) && !excludes.contains(songObj) && !newOptions.contains(songObj))
				newOptions.add(songObj);
		}
		return newOptions;
	}

	// playlist control methods extended to EchoNest

	public void jumpToOption(int optionIndex) throws IOException
	{
		if (optionIndex < 0 || optionIndex >= currentOptions.size())
			return;

		Song unfinishedSong = currentSong;
		changeCurrentSong(currentOptions.get(optionIndex));
		
		try {
			if (!currentSong.equals(firstOption))
				EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId +
						"&unplay_song="+firstOption.getEchoNestId() + "&unplay_song="+unfinishedSong.getEchoNestId() + "&play_song="+currentSong.getEchoNestId());
			else
				EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId +	"&unplay_song="+unfinishedSong.getEchoNestId());
		} catch (EchoNestException e) {}

		makeCurrentPrevious();
		retrieveNewOptions();
	}
	
	public void skipCurrent() throws IOException
	{
		optionsAfterSkip.remove(currentSong);

		if (optionsAfterSkip.isEmpty())
		{
			ArrayList<Song> moreOptions = retrieveMoreOptions(previousSong, previousOptions, numOptions);
			optionsAfterSkip.addAll(moreOptions);
			previousOptions.addAll(moreOptions);
		}

		Song skippedSong = currentSong;
		changeCurrentSong(optionsAfterSkip.get(0));
		
		try {
			EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId +
					"&unplay_song="+firstOption.getEchoNestId() + "&skip_song="+skippedSong.getEchoNestId() + "&play_song="+currentSong.getEchoNestId());
		} catch (EchoNestException e) {}
		
		retrieveNewOptions();
	}

	public void banCurrent() throws IOException
	{
		optionsAfterSkip.remove(currentSong);
		
		if (optionsAfterSkip.isEmpty())
		{
			ArrayList<Song> moreOptions = retrieveMoreOptions(previousSong, previousOptions, numOptions);
			optionsAfterSkip.addAll(moreOptions);
			previousOptions.addAll(moreOptions);
		}

		Song bannedSong = currentSong;
		changeCurrentSong(optionsAfterSkip.get(0));
		
		try {
			EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId +
					"&unplay_song="+firstOption.getEchoNestId() + "&ban_song="+bannedSong.getEchoNestId() + "&play_song="+currentSong.getEchoNestId());
		} catch (EchoNestException e) {}
		
		retrieveNewOptions();
	}

	public void favoriteCurrent() throws IOException
	{
		try {
			EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId +	"&favorite_song="+currentSong.getEchoNestId());
		} catch (EchoNestException e) {}
	}
	
	public void rateCurrent(int rating) throws IOException
	{
		try {
			EchoNestQuery.get("playlist", "dynamic/feedback", "session_id="+sessionId +	"&rate_song="+currentSong.getEchoNestId()+"^"+rating);
		} catch (EchoNestException e) {}
	}
	
	public SessionType getSessionType()
	{
		return sessionType;
	}
	
	public void deletePlaylist() throws IOException
	{
		try {
			EchoNestQuery.get("playlist", "dynamic/delete", "session_id="+sessionId);
		} catch (EchoNestException e) {}
	}
}