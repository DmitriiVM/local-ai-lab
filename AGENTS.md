# AGENTS.md

Local AI Lab is a native Android app for running AI models locally.

### Application

- `:app` — Android entry points, root Metro graph, app shell, and top-level adaptive navigation.
- `:ai:api` — engine-neutral LLM, STT, TTS, availability, memory, and model contracts.
- `:ai:runtime` — engine-neutral routing, availability aggregation, runtime-profile registry, and runtime-memory implementations.
- `:ai:chatterbox` — Chatterbox TTS adapter, tokenizer, and model validation.
- `:ai:llamacpp` — llama.cpp GGUF chat adapter, JNI/native code, and validation.
- `:ai:litertlm` — LiteRT-LM chat runtime, prompt encoding, and validation.
- `:ai:sherpa` — sherpa-onnx STT/TTS adapters, model profiles, and validation.
- `:ai:system` — Android SpeechRecognizer and TextToSpeech adapters.
- `:ai:vosk` — Vosk STT adapter and model validation.
- `:core:audio` — audio capture/decoding, storage, playback, and effects processing.
- `:core:di` — application Metro scope and ViewModel integration.
- `:core:model` — shared domain models and service contracts for models, engines, conversations, and runs.
- `:core:navigation` — Navigation 3 state, contracts, host, and feature registry.
- `:core:operation` — foreground-operation coordination and interruption handling.
- `:core:ui` — application Compose theme and typography.
- `:source:database` — Room database, entities, DAOs, and database provider.
- `:source:models` — catalog, installed-model library, resolution, validation, downloads, and transfers.
- `:source:runs` — run persistence, mapping, export, and replay storage.
- `:source:settings` — app and assistant preferences backed by DataStore.
- `:feature:assistant` — local chat and voice-assistant workflow and UI.
- `:feature:device` — device and engine diagnostics and UI.
- `:feature:models` — model catalog/library browsing, details, and management UI.
- `:feature:playground` — capability dashboard, readiness, and workflow entry points.
- `:feature:runs` — run-history navigation and UI.
- `:feature:settings` — application settings navigation and UI.
- `:feature:stt` — transcription workflow, persistence, navigation, and UI.
- `:feature:tts` — synthesis, preview/playback, persistence, navigation, and UI.

## Code organization

Write modular code with one clear responsibility per file. Prefer one primary
top-level declaration; small private helpers and closely related models may stay
together. Separate contracts from concrete implementations at architectural
boundaries.
