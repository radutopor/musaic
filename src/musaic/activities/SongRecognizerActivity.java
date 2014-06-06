package musaic.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import musaic.app.R;
import musaic.songRecognizer.SongRecognizer;
import musaic.musicEntities.Song;

public class SongRecognizerActivity extends Activity implements SongRecognizer.SongRecognizerListener
{
	private Button startRecordingButton;
	private Button stopRecordingButton;
	private TextView songRecognizerStateText;
	
	private SongRecognizer songRecognizer = new SongRecognizer(this);
	
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.songrecognizer);
    	
    	startRecordingButton = (Button)findViewById(R.id.startRecording);
    	stopRecordingButton = (Button)findViewById(R.id.stopRecording);
    	songRecognizerStateText = (TextView)findViewById(R.id.songRecognizerState);
    	
		songRecognizerStateText.setText("Press \"Start recording\".");
    	stopRecordingButton.setEnabled(false);
    }
    
    public void onStartRecordingClick(View v)
    {
    	songRecognizer.start(25);
    }
    
    public void onStopRecordingClick(View v)
    {
    	songRecognizer.stopRecording();
    }
    
    public void onStateChange()
    {
    	runOnUiThread(updateUI);
    }
    
	private Runnable updateUI = new Runnable() 
	{
		public void run() 
		{
	    	switch (songRecognizer.getState())
	    	{
	    		case RECORDING_AUDIO:
	    			songRecognizerStateText.setText("Listening...");
	    	    	startRecordingButton.setEnabled(false);
	    	    	stopRecordingButton.setEnabled(true);
	    			break;
	    		case PROCESSING_AUDIO:
	    			songRecognizerStateText.setText("Processing...");
	    	    	stopRecordingButton.setEnabled(false);
	    			break;
	    		case GENERATING_FINGERPRINT:
	    			songRecognizerStateText.setText("Interpreting...");
	    			break;
	    		case QUERYING_FINGERPRINT:
	    			songRecognizerStateText.setText("Looking up song...");
	    			break;
	    		case DONE:
	    			Song identifiedSong = songRecognizer.getIdentifiedSong();
	    			if (identifiedSong != null)
	    			{
	    				Intent returnIntent = new Intent();
	    				returnIntent.putExtra("identifiedSong", identifiedSong);
	    		        setResult(RESULT_OK, returnIntent);
	    		        finish();
	    			}
	    			else
	    			{
	    				songRecognizerStateText.setText(songRecognizer.getErrorMessage());
		    	    	startRecordingButton.setEnabled(true);
		    	    	stopRecordingButton.setEnabled(false);
	    			}
	    	}
		}
	};
	
    public void onBackClick(View v)
    {
    	finish();
    }
}
