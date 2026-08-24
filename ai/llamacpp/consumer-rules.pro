# These types and members are resolved by the native llama.cpp bridge.
-keep class com.dmitriim.localailab.ai.llamacpp.LlamaCppRuntime$NativeBridge { native <methods>; }
-keep class com.dmitriim.localailab.ai.llamacpp.NativeTokenCallback { void onToken(java.lang.String); }
-keep class com.dmitriim.localailab.ai.llamacpp.NativeGenerationResult {
    <init>(java.lang.String, java.lang.String, int, int, long, long, long, long, java.lang.String);
}
