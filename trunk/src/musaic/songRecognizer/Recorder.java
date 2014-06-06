package musaic.songRecognizer;

import android.media.*;

class Recorder 				// clasa statica
{
	private static final int SOURCE = MediaRecorder.AudioSource.MIC;
	private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
	private static int sampleRate;
	private static volatile AudioRecord audioRecord;
	
	static			// gaseste cel mai bun sampleRate pentru recunoasterea melodiei
	{
		int[] sampleRates = {11025, 16000, 22050, 44100, 8000};
		for (int i = 0; i < sampleRates.length; i++)
		{
			try
			{
				int bufferSize = AudioRecord.getMinBufferSize(sampleRates[i], CHANNELS, ENCODING);
				if (bufferSize == AudioRecord.ERROR_BAD_VALUE)
					continue;
				
				AudioRecord testRecord = new AudioRecord(SOURCE, sampleRates[i], CHANNELS, ENCODING, bufferSize);	// arunca IllegalArgumentException daca sampleRates[i] nu e bun

				if (testRecord.getState() == AudioRecord.STATE_INITIALIZED)
				{
					sampleRate = sampleRates[i];
					testRecord.release();
					break;
				}
			}
			catch (IllegalArgumentException e){}
		}
	}
	
	public static short[] startRecording(int secondsToRecord)
	{
		int bufferSize = AudioRecord.getMinBufferSize(sampleRate, CHANNELS, ENCODING);
		int samplesToRecord = sampleRate * secondsToRecord;
		short[] pcmData = new short[samplesToRecord];
		
		audioRecord = new AudioRecord(SOURCE, sampleRate, CHANNELS, ENCODING, bufferSize);
		audioRecord.startRecording();

		int samplesRecorded = 0;
		while (samplesRecorded < samplesToRecord)
		{
			samplesRecorded += audioRecord.read(pcmData, samplesRecorded, samplesToRecord-samplesRecorded);
			
			if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED)
			{
				short[] shortenPcmData = new short[samplesRecorded];
				System.arraycopy(pcmData, 0, shortenPcmData, 0, samplesRecorded);
				pcmData = shortenPcmData;
				
				break;
			}
		}
		audioRecord.stop();
		audioRecord.release();
		audioRecord = null;
		
		return pcmData;
	}
	
	public static boolean isRecording()
	{
		return (audioRecord != null);
	}
	
	public static void abortRecording()
	{
		if (isRecording())
			audioRecord.stop();
	}
	
	public static int getSampleRate()
	{
		return sampleRate;
	}
}