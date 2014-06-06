package musaic.playlist;

import java.io.*;
import java.util.ArrayList;
import musaic.musicEntities.*;
import musaic.musicSearch.SearchResult;

@SuppressWarnings("serial")
public abstract class Playlist implements Serializable
{
	static public interface PlaylistListener
	{
		void onCurrentSongChanged();
		void onCurrentOptionsChanged();
		void onPlaylistConstraintsChanged();
	}
	protected PlaylistListener listener;
	
	protected Song currentSong;
	protected Song previousSong;
	protected int numOptions;
	protected Song firstOption;
	protected ArrayList<Song> currentOptions = new ArrayList<Song>();
	protected ArrayList<Song> previousOptions = new ArrayList<Song>();
	protected ArrayList<Song> optionsAfterSkip;
	protected long lastPlayedSongAt;
	
	protected ArrayList<Artist> artistConstraints = new ArrayList<Artist>();
	protected ArrayList<StyleMood> styleMoodConstraints = new ArrayList<StyleMood>();
	
	@SuppressWarnings("unchecked")
	protected void makeCurrentPrevious()
	{
		previousSong = currentSong;
		
		previousOptions.clear();
		previousOptions.addAll(currentOptions);
		optionsAfterSkip = (ArrayList<Song>)previousOptions.clone();
	}
	
	protected void changeCurrentSong(Song newCurrentSong)
	{
		currentSong = newCurrentSong;
		lastPlayedSongAt = System.currentTimeMillis();
		listener.onCurrentSongChanged();
	}

	public abstract void initPlaylist() throws IOException;
	protected abstract void retrieveNewCurrentSong() throws IOException;
	protected abstract void retrieveNewOptions() throws IOException;
	protected abstract ArrayList<Song> retrieveMoreOptions(Song seed, ArrayList<Song> excludes, int numOptions) throws IOException;
	
	// playlist control methods
	
	public void nextSong() throws IOException
	{
		changeCurrentSong(firstOption);
		
		// implement local music feedback
		makeCurrentPrevious();
		retrieveNewOptions();
	}
	
	public void jumpToOption(int optionIndex) throws IOException
	{
		if (optionIndex < 0 || optionIndex >= currentOptions.size())
			return;
		
		changeCurrentSong(currentOptions.get(optionIndex));
		
		// implement local music feedback
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

		changeCurrentSong(optionsAfterSkip.get(0));
		
		// implement local music feedback
		retrieveNewOptions();
	}
	
	public void banCurrent() throws IOException
	{
		skipCurrent();
		
		// implement local music feedback
	}
	
	public void favoriteCurrent() throws IOException
	{
		// implement local music feedback
	}
	
	public void rateCurrent(int rating) throws IOException
	{
		// implement local music feedback
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
					retrieveNewOptions();
				}
				break;
			case ARTIST_GROUP:
				Artist[] artists = (Artist[])driver.getResult();
				for (Artist artist : artists)
					if (!artistConstraints.contains(artist))
					{
						artistConstraints.add(artist);
						if (!artistConstraints.contains(currentSong.getArtist()))
							retrieveNewCurrentSong();
						retrieveNewOptions();
					}
				break;
			case STYLEMOOD_GROUP:
				StyleMood[] stylesMoods = (StyleMood[])driver.getResult();
				for (StyleMood styleMood : stylesMoods)
					if (!styleMoodConstraints.contains(styleMood))
					{
						styleMoodConstraints.add(styleMood);
						retrieveNewCurrentSong();
						retrieveNewOptions();
					}
		}
		listener.onPlaylistConstraintsChanged();
	}

	public void removePlaylistConstraint(int constraintIndex) throws IOException
	{
		if (constraintIndex < 0 || constraintIndex >= artistConstraints.size() + styleMoodConstraints.size())
			return;

		if (constraintIndex < artistConstraints.size())
		{
			Artist constraint = artistConstraints.get(constraintIndex);
			artistConstraints.remove(constraintIndex);
			if (currentSong.getArtist().equals(constraint) && !artistConstraints.isEmpty())
				retrieveNewCurrentSong();
		}
		else
		{
			styleMoodConstraints.remove(constraintIndex - artistConstraints.size());
			retrieveNewCurrentSong();
		}

		listener.onPlaylistConstraintsChanged();
		retrieveNewOptions();
	}
	
	public void setNumOptions(int numOptions) throws IOException
	{
		if (this.numOptions == numOptions)
			return;
		if (numOptions > this.numOptions)
			currentOptions.addAll(retrieveMoreOptions(currentSong, currentOptions, numOptions-this.numOptions));
		else
			while (currentOptions.size() > numOptions)
				currentOptions.remove(currentOptions.size()-1);
		
		this.numOptions = numOptions;
		listener.onCurrentOptionsChanged();
	}
	
	// playlist data getters
	
	public int getNumOptions()
	{
		return numOptions;
	}
	
	public Song getCurrentSong()
	{
		return currentSong;
	}
	
	public Song[] getCurrentOptions()
	{
		return currentOptions.toArray(new Song[0]);
	}
	
	public MusicEntity[] getPlaylistConstraints()
	{
		MusicEntity[] constraints = new MusicEntity[artistConstraints.size() + styleMoodConstraints.size()];
		int i;
		for (i = 0; i < artistConstraints.size(); i++)
			constraints[i] = artistConstraints.get(i);
		for (int j = 0; j < styleMoodConstraints.size(); i++, j++)
			constraints[i] = styleMoodConstraints.get(j);
		
		return constraints;
	}
	
	public int getMinutesIdled()
	{
		return (int)((System.currentTimeMillis() - lastPlayedSongAt) / 1000) / 60;
	}
	
	// playlist serialization
	
	private static File playlistFile = new File("playlist.obj");
	
	public static Playlist loadPlaylist()
	{
		Playlist playlist = null;
		
		if (playlistFile.exists())
			try {
				ObjectInputStream objectStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(playlistFile)));
				playlist = (Playlist)objectStream.readObject();
				objectStream.close();
			} catch (Exception e) {}

		return playlist;
	}
	
	public void savePlaylist()
	{
		try {
			ObjectOutputStream objectStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(playlistFile)));
			objectStream.writeObject(this);
			objectStream.close();
		} catch (Exception e) {}
	}
}