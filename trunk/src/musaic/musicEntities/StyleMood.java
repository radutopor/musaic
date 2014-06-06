package musaic.musicEntities;

import java.util.ArrayList;
import java.io.*;
import java.net.URLEncoder;
import musaic.webAPIs.EchoNestQuery;
import musaic.webAPIs.EchoNestQuery.EchoNestException;
import org.json.*;

public class StyleMood implements MusicEntity
{
	private static final long serialVersionUID = -904353798673599464L;

	public static enum TermType
	{
		STYLE,
		MOOD,
		DESCRIPTION
	}
	
	private static ArrayList<String> stylesCollection =  new ArrayList<String>();
	private static ArrayList<String> moodsCollection =  new ArrayList<String>();
	
	private String term;
	private TermType termType;
	
	public StyleMood(String term, TermType termType)
	{
		this.term = term;
		this.termType = termType;
	}
	
	public StyleMood(String term)
	{
		this.term = term;
	}
	
	public void fillInEchoNestInfo() throws IOException
	{
		if (termType == null)
		{
			if (stylesCollection.isEmpty() || moodsCollection.isEmpty())
				initCollections();
			
			if (stylesCollection.contains(term))
				termType = TermType.STYLE;
			else if (moodsCollection.contains(term))
				termType = TermType.MOOD;
			else
				termType = TermType.DESCRIPTION;
		}
	}

	public static void initCollections() throws IOException
	{
		JSONObject listTermsStyles = null, listTermsMoods = null;
		try {
			listTermsStyles = EchoNestQuery.get("artist", "list_terms", "type=style");
			listTermsMoods = EchoNestQuery.get("artist", "list_terms", "type=mood");
		} catch (EchoNestException e) {}
		
		JSONArray terms = listTermsStyles.optJSONArray("terms");
		for (int i = 0; i < terms.length(); i++)
			stylesCollection.add(terms.optJSONObject(i).optString("name"));
		
		terms = listTermsMoods.optJSONArray("terms");
		for (int i = 0; i < terms.length(); i++)
			moodsCollection.add(terms.optJSONObject(i).optString("name"));
	}
	
	public String getTerm()
	{
		return term;
	}
	
	public TermType getTermType()
	{
		if (termType == null)
			try {
				fillInEchoNestInfo();
			} catch (Exception e) {}
		
		return termType;
	}
	
	public String getTermUrlEnc()
	{
		String termURLEncoded = null;
		try{
			termURLEncoded = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e){}
		return termURLEncoded;
	}

	public String toString()
	{
		String termTypeString = "";
		switch(getTermType())
		{
			case STYLE:
				termTypeString = "(style) ";
				break;
			case MOOD:
				termTypeString = "(mood) ";
				break;
			case DESCRIPTION:
				termTypeString = "(descr) ";
		}
		return termTypeString + getTerm();
	}
	
	public boolean equals(Object styleMoodObj)
	{
		if (!(styleMoodObj instanceof StyleMood))
			return false;

		StyleMood styleMood = (StyleMood)styleMoodObj;
		return (termType == styleMood.getTermType() && term.equals(styleMood.getTerm()));
	}
}