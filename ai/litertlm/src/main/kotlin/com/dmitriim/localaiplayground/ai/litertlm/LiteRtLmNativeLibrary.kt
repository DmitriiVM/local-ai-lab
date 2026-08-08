package com.dmitriim.localaiplayground.ai.litertlm

/** Loads the packaged LiteRT-LM JNI library before its Kotlin SDK accesses native methods. */
internal object LiteRtLmNativeLibrary {
    fun load() {
        System.loadLibrary(LIBRARY_NAME)
    }

    private const val LIBRARY_NAME = "litertlm_jni"
}
