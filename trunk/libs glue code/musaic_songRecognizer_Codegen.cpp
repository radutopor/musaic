#include <android/log.h>
#include <string.h>
#include <jni.h>
#include "musaic_songRecognizer_Codegen.h"
#include "codegen/src/Codegen.h"
 
JNIEXPORT jstring JNICALL Java_musaic_songRecognizer_Codegen_EchoprintCodegen
  (JNIEnv *env, jobject thiz, jfloatArray javaPcmData, jint numSamples)
{
    // cast de la jfloatArray la float*
    float *pcmData = (float *)env->GetFloatArrayElements(javaPcmData, 0);

    // utilizeaza libraria Echoprint pentru a obtine hashcode-ul
    Codegen c = Codegen(pcmData, (unsigned int)numSamples, 0);
    const char *code = c.getCodeString().c_str();
 
    // elibereaza resursele folosite
    env->ReleaseFloatArrayElements(javaPcmData, pcmData , 0); 
 
    // returneaza hashcode-ul
    return env->NewStringUTF(code);
}