package com.dmitriim.localaiplayground

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.StatFs
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dmitriim.localaiplayground.ai.llamacpp.NativeLlama
import com.dmitriim.localaiplayground.ai.api.LlmGenerationRequest
import com.dmitriim.localaiplayground.ai.api.LlmLoadRequest
import com.dmitriim.localaiplayground.ai.sherpa.StageZeroSherpa
import com.dmitriim.localaiplayground.ui.theme.LocalAiPlaygroundTheme
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalAiPlaygroundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    StageZeroScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun StageZeroScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val diagnostics = remember(context) { DeviceDiagnostics.read(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val worker = remember { Executors.newSingleThreadExecutor() }
    val modelsDirectory = remember(context) { context.getExternalFilesDir("models")?.also(File::mkdirs) }
    val modelFile = remember(modelsDirectory) { modelsDirectory?.resolve("Qwen3-1.7B-Q4_K_M.gguf") }
    val whisperDirectory = remember(modelsDirectory) {
        modelsDirectory?.resolve("stage0-whisper-base")?.also(File::mkdirs)
    }
    val whisperWav = remember(whisperDirectory) {
        whisperDirectory?.resolve("test_wavs")?.also(File::mkdirs)?.resolve("0.wav")
    }
    val supertonicDirectory = remember(modelsDirectory) {
        modelsDirectory?.resolve("stage0-supertonic-3")?.also(File::mkdirs)
    }
    val sileroVad = remember(modelsDirectory) { modelsDirectory?.resolve("stage0-silero_vad.onnx") }
    var llamaStatus by remember { mutableStateOf("Not initialized") }
    var modelStatus by remember(modelFile) {
        mutableStateOf(if (modelFile?.isFile == true) "Model file is ready to load." else "No Stage 0 model found")
    }
    var sherpaStatus by remember { mutableStateOf("Model bundles have not been probed.") }
    var engine by remember { mutableStateOf<NativeLlama?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            engine?.unload()
            worker.shutdownNow()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Local AI Playground — Stage 0")
        Text("Stage 0 diagnostic only: it reads local app model files and runs short on-device probes when you tap a button. It never changes system settings or uses the microphone.")
        HorizontalDivider()
        Text("Device: ${diagnostics.device}")
        Text("Android: API ${diagnostics.apiLevel}")
        Text("ABIs: ${diagnostics.abis}")
        Text("Memory: ${diagnostics.totalMemoryGiB} GiB total, ${diagnostics.availableMemoryGiB} GiB currently available")
        Text("App storage: ${diagnostics.availableStorageGiB} GiB available")
        Text("Thermal state: ${diagnostics.thermalState}")
        HorizontalDivider()
        Text("llama.cpp CPU baseline: $llamaStatus")
        Button(
            onClick = {
                llamaStatus = runCatching {
                    NativeLlama(context).let { engine ->
                        "Native runtime loaded. ${engine.javaClass.simpleName} is ready for a GGUF model."
                    }
                }.getOrElse { error -> "Unavailable: ${error.message ?: error.javaClass.simpleName}" }
            },
        ) {
            Text("Check llama.cpp runtime")
        }
        Text("Stage 0 GGUF: ${modelFile?.name ?: "external app storage unavailable"}")
        Text(modelStatus)
        Button(
            enabled = modelFile?.isFile == true,
            onClick = {
                val selectedModel = checkNotNull(modelFile)
                modelStatus = "Loading model on a worker thread…"
                worker.execute {
                    val activeEngine = engine ?: NativeLlama(context)
                    val result = runCatching {
                        activeEngine.load(
                            LlmLoadRequest(
                                modelPath = selectedModel.absolutePath,
                                contextSize = 512,
                            ),
                        )
                    }
                    mainHandler.post {
                        if (engine == null) engine = activeEngine
                        modelStatus = result.fold(
                            onSuccess = { loaded ->
                                "Loaded on CPU in ${loaded.loadDurationMs} ms with ${loaded.effectiveThreadCount} threads."
                            },
                            onFailure = { error -> "Load failed: ${error.message ?: error.javaClass.simpleName}" },
                        )
                    }
                }
            },
        ) {
            Text("Load Stage 0 GGUF")
        }
        Button(
            enabled = engine?.isLoaded == true,
            onClick = {
                modelStatus = "Generating a short CPU response…"
                worker.execute {
                    val result = runCatching {
                        checkNotNull(engine).generate(
                            LlmGenerationRequest(
                                prompt = "<|im_start|>system\\nYou are concise.<|im_end|>\\n<|im_start|>user\\nReply with OK.<|im_end|>\\n<|im_start|>assistant\\n",
                                maxTokens = 8,
                            ),
                        )
                    }
                    mainHandler.post {
                        modelStatus = result.fold(
                            onSuccess = { generated ->
                                "Generated in ${generated.totalDurationMs} ms: ${generated.text.take(120)}"
                            },
                            onFailure = { error -> "Generation failed: ${error.message ?: error.javaClass.simpleName}" },
                        )
                    }
                }
            },
        ) {
            Text("Run short CPU generation")
        }
        Button(
            enabled = engine?.isLoaded == true,
            onClick = {
                modelStatus = "Unloading the CPU model…"
                worker.execute {
                    runCatching { checkNotNull(engine).unload() }
                    mainHandler.post {
                        modelStatus = "CPU model unloaded. It can be loaded again without restarting the app."
                    }
                }
            },
        ) {
            Text("Unload Stage 0 GGUF")
        }
        HorizontalDivider()
        Text("sherpa-onnx CPU baseline: $sherpaStatus")
        Text("Whisper: ${whisperDirectory?.name ?: "external app storage unavailable"}")
        Text("Supertonic: ${supertonicDirectory?.name ?: "external app storage unavailable"}")
        Button(
            enabled = whisperDirectory?.isDirectory == true && whisperWav?.isFile == true,
            onClick = {
                val selectedDirectory = checkNotNull(whisperDirectory)
                val selectedWav = checkNotNull(whisperWav)
                sherpaStatus = "Running a short Whisper file-STT probe on CPU…"
                worker.execute {
                    val result = runCatching { StageZeroSherpa.transcribeWhisper(selectedDirectory, selectedWav) }
                    mainHandler.post {
                        sherpaStatus = result.fold(
                            onSuccess = { stt ->
                                "Whisper completed in ${stt.durationMs} ms: ${stt.text.take(140)}"
                            },
                            onFailure = { error -> "Whisper failed: ${error.message ?: error.javaClass.simpleName}" },
                        )
                    }
                }
            },
        ) {
            Text("Run Whisper file STT")
        }
        Button(
            enabled = supertonicDirectory?.isDirectory == true,
            onClick = {
                val selectedDirectory = checkNotNull(supertonicDirectory)
                sherpaStatus = "Running a short Supertonic PCM synthesis probe on CPU…"
                worker.execute {
                    val result = runCatching {
                        StageZeroSherpa.synthesizeSupertonic(selectedDirectory, "Stage zero local speech synthesis.")
                    }
                    mainHandler.post {
                        sherpaStatus = result.fold(
                            onSuccess = { tts ->
                                "Supertonic produced ${tts.sampleCount} PCM samples at ${tts.sampleRate} Hz in ${tts.durationMs} ms."
                            },
                            onFailure = { error -> "Supertonic failed: ${error.message ?: error.javaClass.simpleName}" },
                        )
                    }
                }
            },
        ) {
            Text("Run Supertonic PCM synthesis")
        }
        Button(
            enabled = sileroVad?.isFile == true && whisperWav?.isFile == true,
            onClick = {
                val selectedVad = checkNotNull(sileroVad)
                val selectedWav = checkNotNull(whisperWav)
                sherpaStatus = "Running the Silero VAD file probe on CPU…"
                worker.execute {
                    val result = runCatching { StageZeroSherpa.probeSileroVad(selectedVad, selectedWav) }
                    mainHandler.post {
                        sherpaStatus = result.fold(
                            onSuccess = { vad ->
                                "Silero VAD completed in ${vad.durationMs} ms; speech detected: ${vad.detectedSpeech}."
                            },
                            onFailure = { error -> "Silero VAD failed: ${error.message ?: error.javaClass.simpleName}" },
                        )
                    }
                }
            },
        ) {
            Text("Run Silero VAD file probe")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StageZeroPreview() {
    LocalAiPlaygroundTheme {
        StageZeroScreen()
    }
}

private data class DeviceDiagnostics(
    val device: String,
    val apiLevel: Int,
    val abis: String,
    val totalMemoryGiB: String,
    val availableMemoryGiB: String,
    val availableStorageGiB: String,
    val thermalState: String,
) {
    companion object {
        fun read(context: Context): DeviceDiagnostics {
            val memory = context.getSystemService(ActivityManager::class.java).let { manager ->
                ActivityManager.MemoryInfo().also(manager::getMemoryInfo)
            }
            val storage = StatFs(context.filesDir.absolutePath)
            val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.getSystemService(PowerManager::class.java).currentThermalStatus.toString()
            } else {
                "Unavailable before API 29"
            }
            return DeviceDiagnostics(
                device = "${Build.MANUFACTURER} ${Build.MODEL}",
                apiLevel = Build.VERSION.SDK_INT,
                abis = Build.SUPPORTED_ABIS.joinToString(),
                totalMemoryGiB = memory.totalMem.toGiB(),
                availableMemoryGiB = memory.availMem.toGiB(),
                availableStorageGiB = storage.availableBytes.toGiB(),
                thermalState = thermal,
            )
        }

        private fun Long.toGiB(): String = String.format(Locale.US, "%.2f", this / 1024.0 / 1024.0 / 1024.0)
    }
}
