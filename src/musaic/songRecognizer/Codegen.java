package musaic.songRecognizer;

class Codegen 			// clasa statica
{
	// metoda 'glue' pentru Codegen.cpp::Codegen.getCodeString() din libraria C++ Echoprint 'libcodegen';
	// metoda nativa, scrisa in C++ si inclusa in libraria nativa 'echoprint-codegen' generata cu Android NDK
	public static native String EchoprintCodegen(float javaPcmData[], int numSamples);
	static
    {
		System.loadLibrary("echoprint-codegen");
    }
}