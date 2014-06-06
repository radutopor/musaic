package musaic.songRecognizer;

import org.json.*;

import musaic.musicEntities.*;

import java.io.IOException;
import musaic.utils.*;
import musaic.webAPIs.EchoNestQuery;
import musaic.webAPIs.EchoNestQuery.EchoNestException;

public class SongRecognizer implements Runnable
{
	public static enum State 			// posibilile stari ale unui SongRecognizer
	{
		DONE,
		RECORDING_AUDIO,
		PROCESSING_AUDIO,
		GENERATING_FINGERPRINT,
		QUERYING_FINGERPRINT
	}

	static public interface SongRecognizerListener
	{
		void onStateChange();		// metoda apelata de catre un obiect SongRecognizer la schimbarea starii acestuia
	}

	private SongRecognizerListener listener;	// SongRecognizerListener-ul a carei metoda callback 'onStateChange' va fi apelata la schimbarea starii acestui SongRecognizer
	private volatile State state = State.DONE;

	private Song identifiedSong;
	private String errorMessage;
	
	private int secondsToRecord;
	
	public SongRecognizer(SongRecognizerListener listener) 
	{
		this.listener = listener;
	}
	
	public void start(int secondsToRecord)
	{
		if (state == State.DONE && !Recorder.isRecording())
		{
			this.secondsToRecord = secondsToRecord;
			new Thread(this).start();				// ruleaza this.run() intr-un thread separat
		}
	}
	
	public void run()
	{
		identifiedSong = null;
		errorMessage = null;
		
		changeState(State.RECORDING_AUDIO);
		short[] pcmData = Recorder.startRecording(secondsToRecord);

		if (pcmData.length / Recorder.getSampleRate() < 5)
		{
			errorMessage = "Not enough audio data.\nPlease record at least 5 sec of audio.";
			changeState(State.DONE);
			return;
		}

		changeState(State.PROCESSING_AUDIO);
		short[] normalizedPcmData = AudioTools.normalize(pcmData);
		pcmData = null;
		short[] resampledPcmData = (Recorder.getSampleRate() != 11025) ? AudioTools.resample(normalizedPcmData, Recorder.getSampleRate(), 11025) : normalizedPcmData;
		normalizedPcmData = null;
		float[] floatPcmData = AudioTools.shortToFloat(resampledPcmData);
		resampledPcmData = null;
		
		changeState(State.GENERATING_FINGERPRINT);
		String EchoprintCode = Codegen.EchoprintCodegen(floatPcmData, floatPcmData.length);
		floatPcmData = null;

		changeState(State.QUERYING_FINGERPRINT);
		JSONObject response;
		try
		{
			response = EchoNestQuery.get("song", "identify", "code="+EchoprintCode + "&version=4.12" + "&bucket=audio_summary");
		} 
		catch (IOException e)
		{
			errorMessage = "Problem contacting server: " + e.getMessage();
			changeState(State.DONE);
			return;
		}
		catch (EchoNestException e)
		{
			errorMessage = e.getMessage();
			changeState(State.DONE);
			return;
		}

		JSONArray songs = response.optJSONArray("songs");
		if (songs.length() == 0)
		{
			errorMessage = "Could not recognize song. Please try again.\nRemember to hold the mic close to the source.";
			changeState(State.DONE);
			return;
		}
		
		JSONObject song = songs.optJSONObject(0);
		int duration = song.optJSONObject("audio_summary").optInt("duration");
		identifiedSong = new Song(song.optString("artist_name"), song.optString("artist_id"), song.optString("title"), song.optString("id"), duration);
		
		changeState(State.DONE);
	}
	
	private void changeState(State state)
	{
		this.state = state;
		listener.onStateChange();
	}
	
	public void stopRecording()
	{
		Recorder.abortRecording();
	}

	public State getState()
	{
		return state;
	}
	
	public String getErrorMessage()
	{
		return errorMessage;
	}
	
	public Song getIdentifiedSong()
	{
		return identifiedSong;
	}
}