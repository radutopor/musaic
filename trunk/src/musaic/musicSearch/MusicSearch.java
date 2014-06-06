package musaic.musicSearch;

import musaic.musicEntities.*;
import musaic.webAPIs.EchoNestQuery;
import musaic.webAPIs.EchoNestQuery.EchoNestException;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

import org.json.*;

public class MusicSearch
{
	private String searchString;
	private SearchResult[] searchResultsArr;

	public void search(String searchString) throws IOException, EchoNestException
	{
		this.searchString = searchString;
		String searchStringEnc = URLEncoder.encode(searchString, "UTF-8");
		String[] searchWords = searchString.toLowerCase().split("\\s");

		ArrayList<SearchResult> searchResults = new ArrayList<SearchResult>();
		
		// cauta dupa stiluri muzicale si/sau stari de spirit
		
		ArrayList<StyleMood> resolvedStylesMoods = new ArrayList<StyleMood>();
		int resolved = 0;
		for (int i = 0; i < searchWords.length; i++)
		{
			String currentTerm = "";
			for (int j = resolved; j <= i; j++)
				currentTerm += searchWords[j] + " ";
			currentTerm = currentTerm.substring(0, currentTerm.length()-1);
			
			StyleMood currentStyleMood = new StyleMood(currentTerm);
			if (currentStyleMood.getTermType() == StyleMood.TermType.STYLE || currentStyleMood.getTermType() == StyleMood.TermType.MOOD)
			{
				resolvedStylesMoods.add(currentStyleMood);
				resolved = i + 1;
			}
		}
		if (resolved == searchWords.length)
			searchResults.add(new SearchResult(resolvedStylesMoods.toArray(new StyleMood[0]), 1));

		// daca exista cel putin doua cuvinte de cautare, cauta dupa artist + titlu
		
		if (searchWords.length > 1)
		{
			JSONObject combinedSearch = null;
			try {
				combinedSearch = EchoNestQuery.get("song", "search", "combined="+searchStringEnc +
						"&results=5" + "&bucket=audio_summary");
			} catch (EchoNestException e) {}
			
			JSONArray songs = combinedSearch.optJSONArray("songs");
			nextSong:
			for (int i=0; i<songs.length(); i++)
			{
				JSONObject song = songs.optJSONObject(i);
				String artist = song.optString("artist_name").toLowerCase();
				String title = song.optString("title").toLowerCase();
				
				boolean artistMatch = false, titleMatch = false;
				for (String searchWord : searchWords)
					if (artist.contains(searchWord))
						artistMatch = true;
					else if (title.contains(searchWord))
						titleMatch = true;
					else
						continue nextSong;

				if (artistMatch && titleMatch)
				{
					int duration = song.optJSONObject("audio_summary").optInt("duration");
					Song songObj = new Song(song.optString("artist_name"), song.optString("artist_id"), song.optString("title"), song.optString("id"), duration);
					
					SearchResult songResult = new SearchResult(songObj, 1);
					if (!searchResults.contains(songResult))
						searchResults.add(songResult);
				}
			}
		}
		
		ArrayList<SearchResult> titlesArtistsResults = new ArrayList<SearchResult>();

		// cauta dupa titlu

		JSONObject titleSearch = null;
		try {
			titleSearch = EchoNestQuery.get("song", "search", "title="+searchStringEnc + 
					"&sort=song_hotttnesss-desc" + "&results=10" + "&bucket=song_hotttnesss" + "&bucket=audio_summary");
		} catch (EchoNestException e) {}
		
		JSONArray songs = titleSearch.optJSONArray("songs");		
		for (int i=0, songsFound=0; (songsFound<5) && (i<songs.length()); i++)
		{
			JSONObject song = songs.optJSONObject(i);
			int duration = song.optJSONObject("audio_summary").optInt("duration");
			Song songObj = new Song(song.optString("artist_name"), song.optString("artist_id"), song.optString("title"), song.optString("id"), duration);

			double score = song.optDouble("song_hotttnesss");
			SearchResult songResult = new SearchResult(songObj, score);

			if (!searchResults.contains(songResult) && !titlesArtistsResults.contains(songResult))
			{
				titlesArtistsResults.add(songResult);
				songsFound++;
			}
		}

		// cauta dupa artist

		boolean perfectArtistMatch = false;
		
		JSONObject artistSearch = null;
		try {
			artistSearch = EchoNestQuery.get("artist", "search", "name="+searchStringEnc +
					"&fuzzy_match=true" + "&sort=hotttnesss-desc" + "&results=5" + "&bucket=hotttnesss");
		} catch (EchoNestException e) {}
		
		JSONArray artists = artistSearch.optJSONArray("artists");
		for (int i = 0; i < artists.length(); i++)
		{
			JSONObject artist = artists.optJSONObject(i);
			Artist artistObj = new Artist(artist.optString("name"), artist.optString("id"));
			Artist[] artistArr = {artistObj};
			
			double score = artist.optDouble("hotttnesss");
			titlesArtistsResults.add(new SearchResult(artistArr, score));
			
			if (artist.optString("name").equalsIgnoreCase(searchString))
				perfectArtistMatch = true;
		}

		// daca nu s-a gasit un potrivire perfecta la pasul precedent, cauta dupa artisti multipli

		if (!perfectArtistMatch)
		{
			String capitalizedWords = "";
			for (String searchWord : searchWords)
				capitalizedWords += Character.toUpperCase(searchWord.charAt(0)) + searchWord.substring(1) + " ";
			capitalizedWords = capitalizedWords.substring(0, capitalizedWords.length()-1);
			capitalizedWords = URLEncoder.encode(capitalizedWords, "UTF-8");
				
			JSONObject artistExtract = null;
			try {
				artistExtract = EchoNestQuery.get("artist", "extract", "text="+capitalizedWords + "&bucket=hotttnesss");
			} catch (EchoNestException e) {}
			
			artists = artistExtract.optJSONArray("artists");
			
			String artistsNamesConcat = "";
			for (int i = 0; i < artists.length(); i++)
				artistsNamesConcat += artists.optJSONObject(i).optString("name").toLowerCase() + " ";
			
			boolean allArtistsFound = true;
			for (String searchWord : searchWords)
				if (!artistsNamesConcat.contains(searchWord))
				{
					allArtistsFound = false;
					break;
				}
	
			if (allArtistsFound)
			{
				Artist[] artistGroup = new Artist[artists.length()];
				double meanScore = 0; 
				
				for (int i = 0; i < artists.length(); i++)
				{
					JSONObject artist = artists.optJSONObject(i);
					artistGroup[i] = new Artist(artist.optString("name"), artist.optString("id"));
	
					meanScore += artist.optDouble("hotttnesss");
				}
				
				meanScore /= (double)artistGroup.length;
				titlesArtistsResults.add(new SearchResult(artistGroup, meanScore));
			}
		}

		Collections.sort(titlesArtistsResults);
		searchResults.addAll(titlesArtistsResults);
		
		if (searchResults.isEmpty())
			throw new EchoNestException(6, "No items found.");

		searchResultsArr = searchResults.toArray(new SearchResult[0]);
	}
	
	public String getSearchString()
	{
		return searchString;
	}
	
	public SearchResult[] getSearchResults()
	{
		return searchResultsArr;
	}
	
	public String[] getResultsDisplay()
	{
		if (searchResultsArr == null)
			return null;
		
		String[] resultsDisplay = new String[searchResultsArr.length];
		
		for (int i = 0; i < resultsDisplay.length; i++)
			resultsDisplay[i] = searchResultsArr[i].toString();
			
		return resultsDisplay;
	}
	
	public SearchResult getSearchResult(int resultIndex)
	{
		if (searchResultsArr == null)
			return null;
		
		return searchResultsArr[resultIndex];
	}
}