package musaic.activities;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import musaic.app.R;
import musaic.mediaSources.MediaSources;
import musaic.mediaSources.MediaSources.YouTubeException;
import musaic.musicEntities.*;
import musaic.musicSearch.*;
import musaic.playlist.*;
import musaic.webAPIs.EchoNestQuery.EchoNestException;
import java.io.IOException;

public class MainActivity extends Activity implements Playlist.PlaylistListener
{
	volatile private MusicSearch musicSearch = new MusicSearch();
	volatile private EchoNestPlaylist playlist;
	volatile private MediaPlayer mediaPlayer = new MediaPlayer();

	private AlertDialog searchResultsDialog;
	private AlertDialog searchResultAddDialog;
	private Dialog numOptionsDialog;
	private EditText searchBox;
	private InputMethodManager input;
	private ProgressBar progressSpinnerBig;
	private TextView currentSongText;
	private Button playPauseButton;
	private SeekBar seekBar;
	private TextView playbackTime;
	private Button skipButton;
	private ListView optionList;
	private HorizontalScrollView playlistConstraintsScroll;
	private LinearLayout playlistConstraints;
	private Button favorite;
	private Button ban;
	private RatingBar ratingBar;
	
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.mainlayout);

    	input = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
    	searchBox = (EditText)findViewById(R.id.searchBox);
    	progressSpinnerBig = (ProgressBar)findViewById(R.id.progressSpinnerBig);
    	currentSongText = (TextView)findViewById(R.id.currentSongText);
    	playPauseButton = (Button)findViewById(R.id.playPause);
    	seekBar = (SeekBar)findViewById(R.id.seekBar);
    	playbackTime = (TextView)findViewById(R.id.playbackTime);
    	skipButton = (Button)findViewById(R.id.skip);
    	optionList = (ListView)findViewById(R.id.optionList);
    	playlistConstraintsScroll = (HorizontalScrollView)findViewById(R.id.playlistConstraintsScroll);
    	playlistConstraints = (LinearLayout)findViewById(R.id.playlistConstraints);
    	favorite = (Button)findViewById(R.id.favorite);
    	ban = (Button)findViewById(R.id.ban);
    	ratingBar = (RatingBar)findViewById(R.id.ratingBar);

    	mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
    		public void onPrepared(MediaPlayer mp)
    		{
				preparePlaybackTaskCode = 0;
				mediaPlayer.start();
				new UpdateUIForPlaybackTask().execute();
    		}});

    	mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
			public void onCompletion(MediaPlayer mp)
			{
				new NextSongTask().execute();
			}});
    	
    	seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			public void onStartTrackingTouch(SeekBar seekBar)
			{
				refreshPlaybackTimeFlag = false;
			}
    		public void onStopTrackingTouch(SeekBar seekBar)
    		{
    			mediaPlayer.seekTo(seekBar.getProgress());
    			refreshPlaybackTimeFlag = true;
			}
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){}
			});
		
    	optionList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
    		public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
    		{
    			new JumpToOptionTask().execute(position);
    		}});
    	
    	optionList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
			{
				numOptionsDialog.show();
				return true;
			}});
    	
    	ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener(){
			public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) 
			{
				if (playlist != null && fromUser)
					new RateSongTask().execute(rating);
			}});
    	
    	searchResultAddDialog = new AlertDialog.Builder(this)
		.setTitle("Add to current playlist?")
		.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
	       public void onClick(DialogInterface dialog, int id) 
	       {
	    	   pickedResult.addToPlaylist = true;
	    	   new SteerPlaylistTask().execute(pickedResult);
	       }})
	    .setNegativeButton("No", new DialogInterface.OnClickListener(){
	       public void onClick(DialogInterface dialog, int id)
	       {
	    	   pickedResult.addToPlaylist = false;
	    	   new SteerPlaylistTask().execute(pickedResult);
	       }})
	    .create();

    	numOptionsDialog = new Dialog(this);
    	numOptionsDialog.setContentView(R.layout.numoptionsdialog);
    	numOptionsDialog.setCancelable(true);
    	numOptionsDialog.setTitle("Number of songs");
    	final EditText numOptions = (EditText)numOptionsDialog.findViewById(R.id.numOptions);
    	numOptionsDialog.setOnShowListener(new DialogInterface.OnShowListener(){
			public void onShow(DialogInterface dialog) 
			{
				numOptions.setText(playlist.getNumOptions()+"");
			}});
    	((Button)numOptionsDialog.findViewById(R.id.more)).setOnClickListener(new View.OnClickListener(){
    		public void onClick(View v)
    		{
    			int currentNumOptions = Integer.parseInt(numOptions.getText().toString());
    			numOptions.setText(((currentNumOptions<8) ? currentNumOptions+1 : 8) + "");
    		}});
    	((Button)numOptionsDialog.findViewById(R.id.less)).setOnClickListener(new View.OnClickListener(){
    		public void onClick(View v)
    		{
    			int currentNumOptions = Integer.parseInt(numOptions.getText().toString());
    			numOptions.setText(((currentNumOptions>3) ? currentNumOptions-1 : 3) + "");
    		}});
    	((Button)numOptionsDialog.findViewById(R.id.ok)).setOnClickListener(new View.OnClickListener(){
    		public void onClick(View v)
    		{
    			new SetNumOptionsTask().execute(Integer.parseInt(numOptions.getText().toString()));
    			numOptionsDialog.dismiss();
    		}});
    	((Button)numOptionsDialog.findViewById(R.id.cancel)).setOnClickListener(new View.OnClickListener(){
    		public void onClick(View v)
    		{
    			numOptionsDialog.dismiss();
    		}});
    }
    
    private void disablePlaylistInput()
    {
		optionList.setEnabled(false);
		skipButton.setEnabled(false);
		progressSpinnerBig.setVisibility(View.VISIBLE);
    }

    public void onSearchClick(View v)
    {
    	String searchString = searchBox.getText().toString();
    	if (!searchString.equals(""))
    		new MusicSearchTask().execute(searchString);
    }
    private class MusicSearchTask extends AsyncTask<String, Void, Void>
    {
    	private Exception exception;
    	protected void onPreExecute()
    	{
    		input.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
    		progressSpinnerBig.setVisibility(View.VISIBLE);
    	}
    	
        protected Void doInBackground(String... searchString)
        {
        	try {
        		musicSearch.search(searchString[0]);
    		} catch (Exception e) { exception = e; }
    		return null;
        }
        protected void onPostExecute(Void v) 
        {
        	progressSpinnerBig.setVisibility(View.GONE);
        	
            if (exception != null)
	        	{ displayException(exception); return; }
	        
	    	CharSequence[] searchItems = musicSearch.getResultsDisplay();
	
	    	searchResultsDialog = new AlertDialog.Builder(MainActivity.this)
	    		.setTitle("Search results")
	    		.setItems(searchItems, pickSearchResult)
	    		.create();
	    	searchResultsDialog.show();
        }
    }
    private SearchResult pickedResult;
    private DialogInterface.OnClickListener pickSearchResult = new DialogInterface.OnClickListener()
	{
        public void onClick(DialogInterface dialogInterface, int itemIndex)
        {
        	pickedResult = musicSearch.getSearchResult(itemIndex);
        	if (playlist == null)
        	{
        		new SteerPlaylistTask().execute(pickedResult);
        		return;
        	}
        	
        	SearchResult.ResultType pickedResultType = pickedResult.getResultType();
        	EchoNestPlaylist.SessionType playlistSessionType = playlist.getSessionType();
        	if ((pickedResultType == SearchResult.ResultType.ARTIST_GROUP && playlistSessionType == EchoNestPlaylist.SessionType.ARTIST) ||
			(pickedResultType == SearchResult.ResultType.STYLEMOOD_GROUP && playlistSessionType == EchoNestPlaylist.SessionType.ARTIST_DESCRIPTION))
        		searchResultAddDialog.show();
        	else
        		new SteerPlaylistTask().execute(pickedResult);
        }
    };
    private class SteerPlaylistTask extends AsyncTask<SearchResult, Void, Void>
    {
    	private Exception exception;
    	protected void onPreExecute()
    	{
    		searchBox.setText("");
    		disablePlaylistInput();
    	}
    	protected Void doInBackground(SearchResult... driver)
        {
    		try {
        		if (playlist == null)
        		{
        			playlist = new EchoNestPlaylist(driver[0], 6, MainActivity.this);
        			playlist.initPlaylist();
        		}
        		else
        			playlist.steerPlaylist(driver[0]);
    		} catch (Exception e) {	exception = e; }
    		return null;
        }
        protected void onPostExecute(Void v) 
        {
        	progressSpinnerBig.setVisibility(View.GONE);
            if (exception != null)
	        	{ displayException(exception);}
        }
    }
    
	public void onCurrentSongChanged()
	{
		new PreparePlaybackTask().execute(playlist.getCurrentSong().mediaSources);
	}
	Runnable beforePreparePlayback = new Runnable(){
		public void run()
		{
			favorite.setEnabled(false);
			ban.setEnabled(false);
			playPauseButton.setEnabled(false);
			seekBar.setProgress(0);
			seekBar.setEnabled(false);
			playbackTime.setText(" ");
			ratingBar.setRating(0);
			currentSongText.setText("Loading song...");
	    	if (mediaPlayer.isPlaying())
	    		mediaPlayer.stop();
		}};
	volatile private int preparePlaybackTaskCode = 0;
    private class PreparePlaybackTask extends AsyncTask<MediaSources, Void, Void>
    {
    	private Exception exception;
    	protected void onPreExecute()
    	{
    		runOnUiThread(beforePreparePlayback);
    	}
    	protected Void doInBackground(MediaSources... mediaSources)
        {
    		int thisTaskCode = ++preparePlaybackTaskCode;
			String source;
			try {
				while (preparePlaybackTaskCode == thisTaskCode)
				{
					source = mediaSources[0].getNextSource();
					mediaPlayer.reset();
					mediaPlayer.setDataSource(source);
					
					mediaPlayer.prepareAsync();
					Thread.sleep(5000);
				}
			} catch (Exception e) {	exception = e; }
			return null;
        }
    	protected void onPostExecute(Void v) 
        {
            if (exception != null)
	        	{ displayException(exception); return; }
        }
    }
	
    private boolean refreshPlaybackTimeFlag = true;
	private class UpdateUIForPlaybackTask extends AsyncTask<Void, Void, Void>
	{
		protected void onPreExecute()
		{
	    	currentSongText.setText(playlist.getCurrentSong().toString());
			playPauseButton.setEnabled(true);
			playPauseButton.setText("Pause");
			seekBar.setEnabled(true);
			seekBar.setMax(mediaPlayer.getDuration());
			favorite.setEnabled(true);
			ban.setEnabled(true);
		}
    	protected Void doInBackground(Void... voids)
    	{
			while(mediaPlayer.isPlaying())
			{
				if (refreshPlaybackTimeFlag)
					runOnUiThread(refreshPlaybackTime);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
			}
    		return null;
    	}
	}
	private Runnable refreshPlaybackTime = new Runnable()
	{
		public void run()
		{
			int currentPlaybackTime = mediaPlayer.getCurrentPosition();
			seekBar.setProgress(currentPlaybackTime);
			int secsRemaining = (mediaPlayer.getDuration() - currentPlaybackTime) / 1000;
			int secs = secsRemaining % 60;
			String secsString = (secs<10) ? "0"+secs : secs+"";
			playbackTime.setText("-" + secsRemaining/60 + ":" + secsString);
		}
	};

	public void onCurrentOptionsChanged()
	{
		runOnUiThread(refreshOptionList);
	}
	private Runnable refreshOptionList = new Runnable()
	{
		public void run()
		{
			ArrayAdapter<Song> adapter = new ArrayAdapter<Song>
			(MainActivity.this, android.R.layout.test_list_item, android.R.id.text1, playlist.getCurrentOptions());
			optionList.setAdapter(adapter);
			optionList.setEnabled(true);
	    	skipButton.setEnabled(true);
		}
	};

	public void onPlaylistConstraintsChanged()
	{
		runOnUiThread(refreshPlaylistConstraints);
	}
	private Runnable refreshPlaylistConstraints = new Runnable()
	{
		public void run()
		{
			playlistConstraints.removeAllViews();
			MusicEntity[] constraints = playlist.getPlaylistConstraints();

			if (constraints.length == 0)
			{
				playlistConstraintsScroll.setVisibility(View.GONE);
				return;
			}
			if (playlistConstraintsScroll.getVisibility() == View.GONE)
				playlistConstraintsScroll.setVisibility(View.VISIBLE);

			int constraintId = 0;
			for (MusicEntity constraint : constraints)
			{
				TextView constraintText = new TextView(MainActivity.this);
				constraintText.setText("[X]"+constraint.toString());
				constraintText.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Medium);
				constraintText.setPadding(0, 0, 10, 0);
				constraintText.setId(constraintId++);
				constraintText.setOnClickListener(removePlaylistConstraint);

				playlistConstraints.addView(constraintText);
			}
		}
	};
	private View.OnClickListener removePlaylistConstraint = new View.OnClickListener()
	{
		public void onClick(View constraintText)
		{
			new RemovePlaylistConstraintTask().execute(constraintText.getId());
		}
	};
	private class RemovePlaylistConstraintTask extends AsyncTask<Integer, Void, Void>
	{
		private Exception exception;
    	protected void onPreExecute()
    	{
    		disablePlaylistInput();
    	}
    	protected Void doInBackground(Integer... constraintIndex)
    	{
        	try {
        		playlist.removePlaylistConstraint(constraintIndex[0]);
    		} catch (Exception e) { exception = e; }
    		return null;
    	}
        protected void onPostExecute(Void v) 
        {
        	progressSpinnerBig.setVisibility(View.GONE);
            if (exception != null)
	        	{ displayException(exception); }
        }
	}
    
	private class JumpToOptionTask extends AsyncTask<Integer, Void, Void>
	{
		private Exception exception;
    	protected void onPreExecute()
    	{
    		disablePlaylistInput();
    	}
    	protected Void doInBackground(Integer... optionIndex)
    	{
        	try {
        		playlist.jumpToOption(optionIndex[0]);
    		} catch (Exception e) { exception = e; }
    		return null;
    	}
        protected void onPostExecute(Void v) 
        {
        	progressSpinnerBig.setVisibility(View.GONE);
            if (exception != null)
	        	{ displayException(exception); }
        }
	}
	
	public void onSkipClick(View v)
	{
		new SkipSongTask().execute();
	}
	private class SkipSongTask extends AsyncTask<Void, Void, Void>
	{
		private Exception exception;
    	protected void onPreExecute()
    	{
    		disablePlaylistInput();
    	}
    	protected Void doInBackground(Void...v)
    	{
        	try {
        		playlist.skipCurrent();
    		} catch (Exception e) { exception = e; }
    		return null;
    	}
        protected void onPostExecute(Void v) 
        {
        	progressSpinnerBig.setVisibility(View.GONE);
            if (exception != null)
	        	{ displayException(exception);}
        }
	}

	private class NextSongTask extends AsyncTask<Void, Void, Void>
	{
		private Exception exception;
    	protected void onPreExecute()
    	{
    		disablePlaylistInput();
    	}
    	protected Void doInBackground(Void... voids)
    	{
        	try {
        		playlist.nextSong();
    		} catch (Exception e) { exception = e; }
    		return null;
    	}
        protected void onPostExecute(Void v) 
        {
        	progressSpinnerBig.setVisibility(View.GONE);
            if (exception != null)
	        	{ displayException(exception); }
        }
	}
	
	public void onFavoriteClick(View v)
	{
		new FavoriteSongTask().execute();
	}
	private class FavoriteSongTask extends AsyncTask<Void, Void, Void>
	{
		private Exception exception;
    	protected Void doInBackground(Void... voids)
    	{
        	try {
        		playlist.favoriteCurrent();
    		} catch (Exception e) { exception = e; }
    		return null;
    	}
        protected void onPostExecute(Void v) 
        {
        	Toast.makeText(MainActivity.this, "Song favorited", Toast.LENGTH_LONG).show();
            if (exception != null)
	        	{ displayException(exception); }
        }
	}
	
	private class RateSongTask extends AsyncTask<Float, Void, Void>
	{
		private Exception exception;
    	protected Void doInBackground(Float...rating)
    	{
    		int intRating = Math.round((rating[0] * 10) / ratingBar.getNumStars());
        	try {
        		playlist.rateCurrent(intRating);
    		} catch (Exception e) { exception = e; }
    		return null;
    	}
        protected void onPostExecute(Void v) 
        {
        	Toast.makeText(MainActivity.this, "Song rated", Toast.LENGTH_LONG).show();
            if (exception != null)
	        	{ displayException(exception);}
        }
	}
	
	public void onBanClick(View v)
	{
		new BanSongTask().execute();
	}
	private class BanSongTask extends AsyncTask<Void, Void, Void>
	{
		private Exception exception;
    	protected void onPreExecute()
    	{
    		disablePlaylistInput();
    	}
    	protected Void doInBackground(Void...v)
    	{
        	try {
        		playlist.banCurrent();
    		} catch (Exception e) { exception = e; }
    		return null;
    	}
        protected void onPostExecute(Void v) 
        {
        	progressSpinnerBig.setVisibility(View.GONE);
        	Toast.makeText(MainActivity.this, "Song banned", Toast.LENGTH_LONG).show();
            if (exception != null)
	        	{ displayException(exception);}
        }
	}

	private class SetNumOptionsTask extends AsyncTask<Integer, Void, Void>
	{
		private Exception exception;
    	protected void onPreExecute()
    	{
    		disablePlaylistInput();
    	}
    	protected Void doInBackground(Integer... numOptions)
    	{
        	try {
        		playlist.setNumOptions(numOptions[0]);
    		} catch (Exception e) { exception = e; }
    		return null;
    	}
        protected void onPostExecute(Void v) 
        {
        	progressSpinnerBig.setVisibility(View.GONE);
            if (exception != null)
	        	{ displayException(exception);}
        }
	}

	public void onPlayPauseClick(View v)
	{
		try {
			if (mediaPlayer.isPlaying())
			{
				mediaPlayer.pause();
				playPauseButton.setText("Play");
			}
			else
			{
				mediaPlayer.start();
				new UpdateUIForPlaybackTask().execute();
				playPauseButton.setText("Pause");
			}
		} catch (Exception e) { displayException(e); }
	}
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mainmenu, menu);
	    return true;
	}
	public static final int RECOGNIZE_SONG_REQUEST = 0;
	public boolean onOptionsItemSelected(MenuItem item)
	{
	    switch (item.getItemId())
	    {
	        case R.id.songRecognizer:
	        	if (mediaPlayer.isPlaying())
	        		mediaPlayer.pause();
	        	playPauseButton.setText("Play");
        		startActivityForResult(new Intent(this, SongRecognizerActivity.class), RECOGNIZE_SONG_REQUEST);
	        	break;
	        case R.id.setNumOptions:
	        	if (playlist != null)
	        		numOptionsDialog.show();
	        	break;
	    }
        return true;
	}
    protected void onActivityResult(int requestCode, int resultCode, Intent returnIntent)
    {
        switch(requestCode)
        {
	        case RECOGNIZE_SONG_REQUEST: 
	              if (resultCode == RESULT_OK) 
	              {
	            	  Song identifiedSong = (Song)returnIntent.getSerializableExtra("identifiedSong");
	            	  new SteerPlaylistTask().execute(new SearchResult(identifiedSong, 1));
	              }
	              break;
        }
    }

    public void onBackPressed()
    {
    	if (playlist != null)
	    	try {
	    		playlist.deletePlaylist();
	    	} catch (IOException e) { displayException(e); }
    	
    	if (mediaPlayer.isPlaying())
    		mediaPlayer.stop();
    	mediaPlayer.release();
    	finish();
    }
    
    private void displayException(Exception e)
    {
    	if (e instanceof IOException)
    		Toast.makeText(this, "Problem contacting servers.", Toast.LENGTH_LONG).show();
    	else if (e instanceof EchoNestException)
    		Toast.makeText(this, ((EchoNestException)e).getMessage(), Toast.LENGTH_LONG).show();
    	else if (e instanceof YouTubeException)
    		Toast.makeText(this, ((YouTubeException)e).getMessage(), Toast.LENGTH_LONG).show();
    	else
    		Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
    	
    	e.printStackTrace();
    }
}