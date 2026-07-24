# Stage 0 — Native feasibility results

## Reference device

- Samsung SM-S926B (Galaxy S24+)
- Android API 36, `arm64-v8a`
- 10.94 GiB RAM; 3.11–3.49 GiB available during probes
- Thermal status `0`; app-private external storage had more than 260 GiB free

## CPU baseline

| Engine/profile | Result on device | Lifecycle |
| --- | --- | --- |
| llama.cpp + Qwen3 1.7B Q4_K_M | Model load: 867 ms; short 8-token response: 1,432 ms | Explicit unload followed by load in 971 ms, without app restart; final model unloaded |
| sherpa-onnx + Whisper Base INT8 | 16 kHz bundled WAV file STT: 545 ms | Recognizer and stream explicitly released per probe |
| sherpa-onnx + Supertonic 3 INT8 | 123,390 PCM samples at 44.1 kHz in 845 ms | TTS object explicitly released; PCM was not played or written to disk |
| sherpa-onnx + Silero VAD | Speech detection on the same WAV: 25 ms | VAD object explicitly released |

All probes requested and used CPU. Vulkan and NNAPI were not enabled; they remain
experimental future backends that must expose their fallback reason.

## Native packaging

- llama.cpp source: `third_party/llama.cpp` commit
  `c0bc8591e8815c63cb01dd3f051a8b0df02501c9`.
- NDK: 27.2.12479018; CMake: 3.22.1; ABI: arm64-v8a.
- The static CPU backend is linked into `local_ai_llamacpp`. Android 16 KiB page
  compatibility uses `-Wl,-z,max-page-size=16384` and
  `-Wl,-z,common-page-size=16384`.
- sherpa runtime: `sherpa-onnx-1.13.4.aar`, SHA-256
  `03f9c4df965f21c71269365a7951a7f23b5696fddd093fa318c80d65550ab780`.

## Scope retained for later stages

- These numbers are feasibility observations, not benchmark promises.
- English and Russian STT/TTS quality is deliberately not evaluated here; the
  project owner will assess it separately.
- The production curated downloader, transactional installer, manifest validation,
  cancellation and experimental-backend fallback UI belong to Stage 2.
