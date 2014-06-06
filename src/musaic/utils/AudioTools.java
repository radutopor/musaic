package musaic.utils;

import android.media.*;

public class AudioTools 			// clasa statica
{
	// metoda 'glue', interfata pentru libraria C++ LAME 'libmp3lame';
	// metoda nativa, scrisa in C++ si inclusa in libraria nativa 'lame-mp3encode' generata cu Android NDK
	static native public byte[] encodeMp3(short[] pcmDataL, short[] pcmDataR, int channels, int numSamples, int sampleRate, int quality);
	static
    {
		System.loadLibrary("lame-mp3encode");
    }
	
	static public short[] normalize(short[] pcmData)
	{
		short[] normalizedPcmData = new short[pcmData.length];
		
		long RMS = 0;
		int numSamples = 1, originalValue;
		for (int i = 0; i < pcmData.length; i++)
		{
			originalValue = Math.abs(pcmData[i]);
			if (originalValue > 150)							// prag pt participare la calculul RMS-ului (ignora zgomotul de fundal)
			{
				RMS += originalValue;
				numSamples++;
			}			
		}
		RMS /= numSamples;

		float gain = (Short.MAX_VALUE / 5) / (float)RMS;			// coeficientul de normalizare a volumului
		
		int normalizedValue;
		for (int i = 0; i < normalizedPcmData.length; i++)
		{
			normalizedValue = (int) (gain * pcmData[i]);
			
			if (normalizedValue > Short.MAX_VALUE)
				normalizedValue = Short.MAX_VALUE;
			else if (normalizedValue < Short.MIN_VALUE)
				normalizedValue = Short.MIN_VALUE;
			
			normalizedPcmData[i] = (short)normalizedValue;
		}
		
		return normalizedPcmData;
	}
	
	static public short[] resample(short[] pcmData, int sampleRate, int resampleRate)
	{
		int numNewSamples = (int) Math.floor(((float) pcmData.length / sampleRate) * resampleRate);
		short[] resampledPcmData = new short[numNewSamples];
		
		float ratio = (float) sampleRate / resampleRate;
		for (int j, i = 0; i < numNewSamples; i++)
		{
			j = Math.round(ratio * i);
			if (j >= pcmData.length)
				j = pcmData.length - 1;
			
			resampledPcmData[i] = pcmData[j];			// toDO: linear interpolation, maybe?
		}
		
		return resampledPcmData;
	}
	
	static public float[] shortToFloat(short[] pcmData)
	{
        float floatPcmData[] = new float[pcmData.length];
        
        for (int i = 0; i < pcmData.length; i++)
        	floatPcmData[i] = (float)pcmData[i] / Short.MAX_VALUE;
        
        return floatPcmData;
	}
	
	static public void playBuffer(short[] pcmData, int sampleRate)
	{
		int bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT); 
		AudioTrack audioPlayer = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
		audioPlayer.play();
	
		int samplesPlayed = 0;
		while (samplesPlayed < pcmData.length)
			samplesPlayed += audioPlayer.write(pcmData, samplesPlayed, pcmData.length-samplesPlayed);
			
		audioPlayer.stop();
		audioPlayer.release();
	}
}