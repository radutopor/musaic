package musaic.musicSearch;

import musaic.musicEntities.*;

public class SearchResult implements Comparable<SearchResult>
{
	public static enum ResultType
	{
		SONG,
		ARTIST_GROUP,
		STYLEMOOD_GROUP,
	}
	
	private Object result;
	private ResultType resultType;
	private double score;
	
	public SearchResult(Song song, double score)
	{
		result = song;
		resultType = ResultType.SONG;
		this.score = score;
	}
	
	public boolean addToPlaylist = false;

	public SearchResult(Artist[] artistGroup, double score)
	{
		result = artistGroup;
		resultType = ResultType.ARTIST_GROUP;
		this.score = score;
	}
	
	public SearchResult(StyleMood[] styleMoodGroup, double score)
	{
		result = styleMoodGroup;
		resultType = ResultType.STYLEMOOD_GROUP;
		this.score = score;
	}
	
	public Object getResult()
	{
		return result;
	}
	
	public ResultType getResultType()
	{
		return resultType;
	}
	
	public double getScore()
	{
		return score;
	}
	
	public String toString()
	{
		String toString = "";
		switch (resultType)
		{
			case SONG:
				Song song = (Song)result;
				toString = song.toString();
				break;
			case ARTIST_GROUP:
				Artist[] artistGroup = (Artist[])result;
				if (artistGroup.length == 1)
					toString = "(artist) ";
				else
					toString = "(artists) ";
				for (Artist artist : artistGroup)
					toString += artist.toString() + ", ";
				toString = toString.substring(0, toString.length()-2);
				break;
			case STYLEMOOD_GROUP:
				StyleMood[] styleMoodGroup = (StyleMood[])result;
				for (StyleMood styleMood : styleMoodGroup)
					toString += styleMood.toString() + ", ";
				toString = toString.substring(0, toString.length()-2);
		}
		return toString;
	}
	
	public boolean equals(Object searchResultObj)	// numai pentru SearchResult-uri de tip SONG 
	{
		if (!(searchResultObj instanceof SearchResult))
			return false;
		
		SearchResult searchResult = (SearchResult)searchResultObj;
		
		if (resultType != ResultType.SONG || searchResult.getResultType() != ResultType.SONG)
			return false;

		return ((Song)result).equals(searchResult.getResult());
	}

	public int compareTo(SearchResult searchResult) 
	{
		if (score == searchResult.score)	return 0;
		return (score > searchResult.score) ? -1 : 1; 
	}
}