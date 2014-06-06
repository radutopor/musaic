#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "musaic_utils_AudioTools.h"
#include "lame.h"
 
JNIEXPORT jbyteArray JNICALL Java_musaic_utils_AudioTools_encodeMp3
  (JNIEnv *env, jobject thiz, jshortArray javaPcmDataL, jshortArray javaPcmDataR, jint javaChannels, jint javaNumSamples, jint javaSampleRate, jint javaQuality)
{
    // declararea si initializarea variabilelor locale native
    short *pcmDataL = (short *)env->GetShortArrayElements(javaPcmDataL, 0);
    short *pcmDataR;
    unsigned int channels = (unsigned int)javaChannels;
	unsigned int numSamples = (unsigned int)javaNumSamples;
	unsigned int sampleRate = (unsigned int)javaSampleRate;
	unsigned int quality = (unsigned int)javaQuality;

    // initializari pentru encodarea LAME
    lame_global_flags *settings;
    settings = lame_init();

	lame_set_num_channels(settings, channels);
	pcmDataR = (channels == 2) ? (short *)env->GetShortArrayElements(javaPcmDataR, 0) : pcmDataL;

    lame_set_num_samples(settings, (unsigned long)numSamples);
	lame_set_in_samplerate(settings, sampleRate);

	lame_set_VBR(settings, vbr_default);
	lame_set_quality(settings, quality);		//algoritmul folosit pentru encodare; calitate vs viteza

	if (lame_init_params(settings) < 0)
		return 0;

	// encodarea propriuzisa
	unsigned char *mp3Buffer = (unsigned char*) malloc(numSamples * sizeof(unsigned char));
	unsigned int numBytesEncoded;
	
	numBytesEncoded = lame_encode_buffer(settings, pcmDataL, pcmDataR, numSamples, mp3Buffer, numSamples);
	lame_encode_flush(settings, mp3Buffer, numSamples);

	lame_close(settings);
	env->ReleaseShortArrayElements(javaPcmDataL, pcmDataL, 0);
	if (channels == 2)
		env->ReleaseShortArrayElements(javaPcmDataR, pcmDataR, 0);

	//construirea si returnarea array-ul de bytes Java
	jbyteArray javaMp3Buffer = env->NewByteArray(numBytesEncoded);
	jbyte *nativeMp3Buffer = env->GetByteArrayElements(javaMp3Buffer, 0);
	
	memcpy(nativeMp3Buffer, mp3Buffer, numBytesEncoded);
	free(mp3Buffer);
	env->ReleaseByteArrayElements(javaMp3Buffer, nativeMp3Buffer, 0);

	return javaMp3Buffer;
}