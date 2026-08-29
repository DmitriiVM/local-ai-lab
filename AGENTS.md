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
- `:source:runs` — run persistence, mapping, export, and replay storage.
- `:feature:assistant` — local chat and voice-assistant workflow and UI.
- `:feature:device` — device and engine diagnostics and UI.
- `:feature:models:api` — model-library, transfer, diagnostics, and model-navigation contracts.
- `:feature:models:impl` — catalog, installed-model library, validation, downloads, transfers, and models UI.
- `:feature:playground` — capability dashboard, readiness, and workflow entry points.
- `:feature:runs` — run-history navigation and UI.
- `:feature:settings:api` — settings contracts and navigation destinations.
- `:feature:settings:impl` — DataStore-backed settings and settings UI.
- `:feature:stt` — transcription workflow, persistence, navigation, and UI.
- `:feature:tts` — synthesis, preview/playback, persistence, navigation, and UI.

## Code organization

Write modular code with one clear responsibility per file. Prefer one primary
top-level declaration; small private helpers and closely related models may stay
together. Separate contracts from concrete implementations at architectural
boundaries.

## Feature-oriented modularization

Organize product functionality as vertical slices. A feature owns the UI,
workflow, domain rules, and data adapters required for one user-facing
capability; do not split feature-owned code into global layer modules.

Use an API/implementation pair when another module needs a feature's public
capability:

- `:feature:<name>:api` exposes only stable contracts, shared feature models,
  and navigation destinations required by other modules.
- `:feature:<name>:impl` contains the feature's `presentation`, `domain`, and
  `data` packages and provides the API bindings.
- Features depend on other features' `:api` modules, never their `:impl`
  modules.

Keep domain rules and decision policies in the feature's `domain` package.
Keep filesystem, database, network, Android-framework, and other external
adapters in `data`.
