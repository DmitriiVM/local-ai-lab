# Local AI Playground — Product Plan

> [!WARNING]
> **Historical planning document — not current project documentation.**
>
> This plan was created during the initial AI-assisted development phase and is
> preserved to show the project's evolution. Its scope and assumptions may no longer
> match the application. Refer to the repository [README](../../README.md) and current
> source code for authoritative information.

## 1. Purpose

Local AI Playground is an Android application for running, inspecting, and comparing
AI inference entirely on the device.

The first release focuses on four workflows:

1. Chat with a local language model.
2. Transcribe microphone or file audio with a local speech-to-text model.
3. Synthesize speech with a local text-to-speech model.
4. Combine the three capabilities into a local voice assistant.

The application is a developer and enthusiast tool. It should expose model settings,
runtime behavior, resource usage, and performance metrics instead of hiding them
behind a production-assistant experience.

## 2. Definition of “local”

Inference is local when prompts, recorded audio, generated text, and generated audio
do not leave the device.

Network access is allowed only for explicit model downloads and optional opening of
model documentation or license links. After a model is installed, its supported
playground must work in airplane mode.

System-provided on-device engines, such as Gemini Nano through Android AICore, count
as local inference. Their device, foreground, quota, and system-service restrictions
must be visible to the user.

## 3. Target users

- Android developers evaluating on-device AI libraries.
- ML engineers testing quantized mobile models.
- Product developers comparing latency, memory, and output quality.
- Enthusiasts experimenting with private and offline AI.

The user is expected to understand concepts such as model size and quantization, but
the application must explain unsupported configurations and failures in plain
language.

## 4. Product principles

### 4.1 Local and transparent

- Never silently fall back to cloud inference.
- Identify the active engine and model on every run.
- Display whether a model is installed, loaded, or unsupported.
- Explain why a capability is unavailable.

### 4.2 Measurable

- Capture latency and throughput for every inference run.
- Distinguish cold runs from warm runs.
- Store the parameters needed to reproduce a run.
- Allow results to be inspected and exported.

### 4.3 Modular

- UI and domain logic must not depend on a specific inference library.
- LLM, STT, and TTS engines must be replaceable independently.
- Model management must support multiple formats and engines without pretending
  that every engine can load every model.

### 4.4 Safe for constrained devices

- Check available storage, architecture, and approximate memory requirements before
  loading a model.
- Keep only the models required for the active workflow loaded by default.
- Make cancellation and unloading first-class operations.
- Surface thermal throttling and out-of-memory risk where possible.

### 4.5 Useful before it is comprehensive

The first complete vertical slices should be small and dependable. Additional
engines, model families, RAG, image input, and scientific quality evaluation are
later extensions.

## 5. Core user journeys

### 5.1 First launch

1. The app explains that inference is local but models can consume significant
   storage and memory.
2. Device diagnostics determine supported ABIs, memory, storage, and available
   system engines.
3. The Playground screen shows which capabilities are ready and which need a model.
4. The user imports or downloads a compatible model.
5. The app validates the model and offers to open the relevant playground.

### 5.2 Chat

1. The user selects an installed LLM and creates a conversation.
2. The user optionally changes the system prompt and generation settings.
3. The response streams into the conversation.
4. The user can stop generation.
5. The completed or cancelled run records parameters and performance metrics.

### 5.3 Speech to text

1. The user selects a local STT model and language configuration.
2. The user records speech or imports an audio file.
3. Partial text is shown when the selected engine supports streaming.
4. The final transcript and timing metrics are stored as a run.

### 5.4 Text to speech

1. The user selects a local voice model and enters text.
2. The app synthesizes and plays the audio.
3. The user can stop playback, replay it, or export the generated audio.
4. The synthesis parameters and metrics are stored as a run.

### 5.5 Voice assistant

1. The user selects one STT, LLM, and TTS model.
2. The app listens until explicit stop or voice activity detection completes a turn.
3. The transcript is sent to the local LLM.
4. The response is synthesized and played.
5. The user can interrupt speaking and begin another turn.
6. The app displays the latency contribution of each pipeline stage.

## 6. Scope

### 6.1 MVP scope

- Single-activity Jetpack Compose application.
- Navigation between Playground, Models, Runs, and Device.
- Import, validate, inspect, delete, load, and unload local models.
- Curated model downloads when a distributable model source has been approved.
- English and Russian samples and one validated complete voice path for each
  language.
- Streaming local LLM chat.
- Live and file-based local speech transcription.
- Local text-to-speech synthesis and playback.
- Push-to-talk voice assistant using the three selected engines.
- Per-run settings, results, errors, and performance metrics.
- Device capability and compatibility diagnostics.
- Local conversation and run history.
- JSON and share-sheet export of individual runs.
- Clear offline/privacy behavior and actionable error states.

### 6.2 Explicit non-goals for the MVP

- Cloud inference or automatic cloud fallback.
- Model training, fine-tuning, or conversion on the phone.
- Retrieval-augmented generation or document chat.
- Image generation, vision chat, OCR, or camera features.
- Always-listening wake-word behavior.
- Background inference while the app is not visible.
- Multi-device result synchronization or user accounts.
- A general plugin SDK.
- iOS, desktop, or web clients.
- Claims of scientifically rigorous model-quality benchmarking.

## 7. Information architecture

The primary navigation contains:

- **Playground** — launch Chat, Speech to Text, Text to Speech, or Voice Assistant.
- **Models** — install and manage models.
- **Runs** — inspect, repeat, compare, and export results.
- **Device** — inspect device capability and runtime status.

Settings are opened from a top-level action rather than occupying a primary
navigation destination.

Detailed screen and behavior requirements are in
[PLAN_FEATURES.md](PLAN_FEATURES.md).

## 8. MVP success criteria

The MVP is successful when:

- A compatible arm64 Android device can complete all four core workflows without
  an inference network request.
- The app detects unsupported model, engine, ABI, memory, permission, and system
  service states before or during an operation and presents an actionable error.
- Long-running operations can be cancelled without leaving a corrupted model or a
  stuck runtime.
- Every completed inference shows its model, settings, and core latency metrics.
- A saved run can be repeated with the same compatible configuration.
- Installed models and saved runs survive process death and app restart.
- Removing a model does not corrupt historical run metadata.
- The user can verify offline operation after models have been installed.

## 9. Constraints and assumptions

- The current project targets Android with `minSdk 26` and Jetpack Compose.
- The implementation uses Gradle modules with Metro dependency injection; each
  feature, source, AI, or core module contributes its own bindings, while `:app`
  owns only the root graph and app-specific runtime values.
- Navigation uses Android Navigation 3 with feature-owned type-safe keys and entry
  contributions. `:app` owns navigation state and the root `NavDisplay`, but does
  not construct feature routes or ViewModels.
- The primary inference target for the MVP is `arm64-v8a`.
- Emulators are suitable for UI development but are not representative inference
  targets.
- Model files can range from tens of megabytes to several gigabytes.
- Model loading and native inference may fail despite preflight estimates; the app
  must recover gracefully.
- Exact starter models and their distribution licenses must be approved before
  enabling curated downloads.
- Performance varies significantly with device hardware, temperature, battery
  policy, context length, and model quantization.

## 10. Documentation map

- [PLAN_FEATURES.md](PLAN_FEATURES.md) — screens, behavior, states, and product
  requirements.
- [PLAN_TECH.md](PLAN_TECH.md) — architecture, data, runtimes, and technical
  constraints.
- [PLAN_MODELS.md](PLAN_MODELS.md) — runtime strategy, starter model candidates,
  compatibility profiles, and catalog governance.
- [PLAN_STAGES.md](PLAN_STAGES.md) — implementation order and exit gates.
- [PLAN_DECISIONS.md](PLAN_DECISIONS.md) — accepted decisions and unresolved
  questions.
- [PLAN_REFERENCES.md](./PLAN_REFERENCES.md) — inspected reference implementations,
  pinned snapshots, adopted techniques, and explicitly rejected patterns.

## 11. Change policy

- A product behavior change updates `PLAN_FEATURES.md`.
- A dependency, runtime, persistence, or architecture change updates
  `PLAN_TECH.md`.
- A runtime/model candidate, compatibility profile, or catalog-policy change updates
  `PLAN_MODELS.md`.
- A sequencing or delivery-scope change updates `PLAN_STAGES.md`.
- A decision that affects more than one plan document is recorded first in
  `PLAN_DECISIONS.md`, then reflected in the affected documents.
- A reference implementation used to justify requirements or architecture is pinned
  and evaluated in `PLAN_REFERENCES.md`.
- Feature IDs from `PLAN_FEATURES.md` should be referenced by implementation tasks
  and pull requests.

## 12. Glossary

- **Engine** — runtime implementation capable of executing a compatible model.
- **Model** — model files plus metadata required by an engine.
- **Capability** — LLM, STT, TTS, or supporting functionality such as VAD.
- **Run** — one recorded inference attempt, successful, cancelled, or failed.
- **Cold run** — inference that includes model/runtime initialization.
- **Warm run** — inference performed with an already initialized model.
- **TTFT** — time from submitting a chat request to the first generated token.
- **RTF** — processing time divided by audio duration; lower is faster.
