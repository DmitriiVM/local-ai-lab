# Local AI Playground — Features and Screens

> [!WARNING]
> **Historical planning document — not current project documentation.**
>
> This feature plan was created during the initial AI-assisted development phase and
> is preserved to show the project's evolution. Its requirements and priorities may
> no longer match the application. Refer to the repository [README](../../README.md)
> and current source code for authoritative information.

## 1. Requirement conventions

Requirements use stable identifiers:

- `CORE` — navigation and shared behavior.
- `MOD` — model management.
- `CHAT` — local LLM chat.
- `STT` — speech to text.
- `TTS` — text to speech.
- `VOICE` — combined voice assistant.
- `RUN` — history, comparison, and export.
- `DEV` — device diagnostics.
- `SET` — settings and privacy.

Priorities:

- **P0** — required for the MVP workflow to be usable.
- **P1** — required before the MVP is considered complete.
- **P2** — post-MVP enhancement.

## 2. Navigation map

```text
App
├── Playground
│   ├── Chat
│   │   ├── Conversation
│   │   └── Generation settings
│   ├── Speech to Text
│   ├── Text to Speech
│   └── Voice Assistant
├── Models
│   ├── Model catalog / import
│   └── Model details
├── Runs
│   ├── Run details
│   └── Compare runs
├── Device
└── Settings
```

On compact screens, Playground, Models, Runs, and Device use bottom navigation.
Larger layouts may use a navigation rail without changing destination behavior.

## 3. Shared application behavior

### CORE-001 — App shell (P0)

- Provide the four primary destinations.
- Give each top-level destination its own back stack.
- Preserve the state and back stack of each top-level destination while switching
  between them.
- Support deep navigation to a model, run, or active playground.
- Restore the selected top-level destination and restorable navigation entries after
  configuration change or process recreation.

### CORE-002 — Operation lifecycle (P0)

All long-running operations expose:

- Idle, preparing, running, cancelling, completed, and error states.
- Determinate progress when byte or item totals are known.
- Indeterminate progress otherwise.
- Cancellation where the underlying runtime allows it.
- Protection against starting a conflicting operation.

Leaving a screen must not accidentally orphan microphone capture, audio playback,
model download, or inference.

### CORE-003 — Errors (P0)

Errors contain:

- A plain-language title and explanation.
- The affected engine/model.
- A suggested action when one exists.
- Optional expandable technical details.
- Retry when retrying is safe.

Expected error categories include permission denied, unsupported device, incompatible
model, insufficient storage, probable insufficient memory, corrupt download, engine
initialization, inference failure, cancellation, and system-service unavailable.

### CORE-004 — Offline trust (P1)

- The active engine is visible during inference.
- System and custom engines are visually distinguishable.
- The app never switches to another engine without confirmation.
- The Device screen reports whether installed workflows are ready for airplane-mode
  use.

### CORE-005 — Accessibility and adaptive layout (P1)

- Every control has an accessible label.
- Content remains usable with large font sizes.
- Status is not communicated by color alone.
- TalkBack focus follows streaming and dialog changes predictably.
- Record, stop, and interrupt controls have sufficiently large touch targets.
- Landscape and tablet layouts avoid unnecessarily stretched text.

### CORE-006 — First launch (P1)

- Explain local-inference privacy, optional download network use, and potential model
  storage/memory cost.
- Run lightweight device/engine availability probes.
- Do not require a model installation before the user can inspect the app.
- Allow onboarding to be reopened from Settings.

### CORE-007 — Foreground lifecycle (P1)

- Stop microphone capture when the app leaves the foreground.
- Request cancellation of active inference and synthesis that cannot safely continue
  without visible UI.
- Stop non-background playback according to the documented audio policy.
- Resolve an interrupted operation to a recoverable cancelled state.
- Allow explicitly started WorkManager model downloads to continue.

## 4. Playground screen

### CORE-010 — Capability cards (P0)

Display cards for Chat, Speech to Text, Text to Speech, and Voice Assistant.
Each card shows one readiness state:

- Ready.
- Model required.
- Download/import in progress.
- Unsupported on this device.
- Temporarily unavailable.

Selecting a card opens the playground or a focused model-selection flow.

### CORE-011 — Recent activity (P1)

Show a small list of recent runs with capability, model, result status, and time.
Selecting an item opens Run Details.

## 5. Models

The planned runtime/model shortlist and the distinction between provisional,
approved, optional, and research-only entries are defined in
[PLAN_MODELS.md](PLAN_MODELS.md).

### MOD-001 — Model list (P0)

Group installed and available models by capability. Each row shows:

- Display name and family.
- Capability and compatible engine.
- Installed runtime profile, such as STT-only, TTS-only, or voice-pipeline.
- Format and quantization, when known.
- File size.
- Installation and validation status.
- Loaded state.
- Catalog state: provisional, approved, optional, research/import-only, or retired.

The user can filter by capability, engine, language, and installed state.

### MOD-002 — Import model (P0)

- Use Android’s document picker.
- Inspect candidate files before copying.
- Show required app-private storage.
- Copy the model and required companion files into an app-owned model directory.
- Keep the import recoverable if the source document disappears.
- Validate structure and metadata with the selected engine.
- Do not leave a model marked ready after a partial or failed copy.

For multi-file models, the importer must support selecting a directory or all
required files and explain missing files.

### MOD-003 — Curated download (P2)

- Show only catalog versions that have passed the approval gate in
  [PLAN_MODELS.md](PLAN_MODELS.md).
- Curated sources are public and do not prompt for or require a shared model-host
  token.
- Show source, license, total size, expected files, checksum, languages, and a
  coarse memory recommendation before download.
- Require explicit confirmation.
- Support progress, cancellation, retry, and resumption where the host permits it.
- Verify the checksum before installation.
- Keep temporary files separate from installed models.
- Treat catalog metadata as versioned data.
- Do not offer a research-only or merely provisional candidate as a one-tap
  download.
- Show a persistent system notification and cancellation action while a long
  transfer continues outside the visible screen.
- Install a multi-file/archive bundle only after the complete transaction passes
  size, SHA-256, extraction, and engine-profile validation.

### MOD-004 — Model details (P0)

Display:

- Stable local ID and display name.
- Engine, capability, format, quantization, and architecture.
- File list and total disk usage.
- Context size, vocabulary, sample rate, speakers, or languages where relevant.
- Runtime profile, file roles, frontend assets, and model-family-specific
  requirements.
- Supported streaming, partial-result, endpointing, contextual-biasing, and
  cancellation behavior where applicable.
- Requested/supported providers and recommended thread policy.
- Source URL and license information.
- Compatibility result for the current device.
- Import/download and last-used timestamps.
- Validation errors and raw metadata.

Actions include load, unload, open playground, benchmark, rename local display name,
and delete.

### MOD-005 — Delete model (P0)

- Confirm the exact model and reclaimed space.
- Refuse deletion while the model is actively in use.
- Unload before deletion when safe.
- Preserve historical run metadata while marking the referenced model unavailable.
- Delete model files and model-specific generated cache.

### MOD-006 — Model compatibility (P0)

Before loading, evaluate:

- Engine and format compatibility.
- ABI/runtime availability.
- Required companion files.
- Required runtime revision, provider libraries, frontend assets, and input/output
  audio formats.
- Available storage for installation.
- Approximate RAM requirement against device memory.
- Feature restrictions such as language or context size.

Compatibility is an advisory preflight, not a guarantee.

### MOD-007 — Model load policy (P0)

- Loading is explicit or occurs when entering a playground.
- Only the minimum required heavy engines/models remain loaded by default.
- Show loading progress and allow safe cancellation.
- Unload on user request and under recoverable memory pressure.

### MOD-008 — Capability-specific installation profiles (P1)

- A multi-capability catalog entry may expose STT-only, TTS-only, LLM-only, and
  complete voice-pipeline installation profiles.
- Install only files required by the selected profile plus explicitly declared
  shared dependencies.
- Do not mark an uninstalled capability ready because another profile shares the
  same catalog entry.
- Reuse identical files without duplicating storage only when ownership and
  deletion references remain transactional.
- A remote catalog change must never delete a locally installed profile.

## 6. Chat

### CHAT-001 — Conversation screen (P0)

- Create a new conversation and select an installed compatible LLM.
- Show user, assistant, and optional system messages.
- Stream assistant text as it is generated.
- Keep the latest content visible unless the user scrolls away.
- Support multiline input and keyboard send behavior.

### CHAT-002 — Generation controls (P0)

- Send.
- Stop generation.
- Regenerate the last response.
- Edit and retry a user message.
- Copy message text.
- Clear conversation with confirmation.

Only one generation may mutate a conversation at a time.

### CHAT-003 — Prompt configuration (P0)

Expose engine-supported settings:

- System prompt.
- Temperature.
- Top-K and Top-P.
- Maximum output tokens.
- Seed.
- Context size.
- Thread count where appropriate.

Unsupported settings are hidden or disabled with an explanation. A Reset action
restores engine-safe defaults.

### CHAT-004 — Context handling (P0)

- Show estimated or exact context usage when the engine exposes tokenization.
- Never silently exceed the engine’s context limit.
- Use a documented truncation strategy.
- Warn before old turns are omitted.
- Store the effective prompt configuration with the run.

The MVP truncation strategy is oldest-turn removal while preserving the system
prompt and latest user turn.

### CHAT-005 — Chat metrics (P0)

Show:

- Cold or warm run.
- Model load time when part of the run.
- Prompt token count and prompt tokens per second, when available.
- TTFT.
- Generated token count and tokens per second.
- Total duration.
- Finish reason.

### CHAT-006 — Conversation persistence (P1)

- Persist completed messages and conversation metadata.
- Recover cleanly from process death.
- Mark an interrupted streaming response as cancelled or incomplete.
- Allow conversations to be renamed and deleted.

### CHAT-007 — Compare responses (P2)

Run the same prompt against two compatible models or parameter presets and display
results and metrics side by side.

## 7. Speech to text

### STT-001 — Input modes (P0)

Support:

- Live microphone capture.
- Imported audio file.

Show the selected model, language mode, audio duration, and current recording state.

### STT-002 — Recording (P0)

- Request microphone permission at the point of use.
- Start and stop explicitly.
- Display elapsed time and a level/waveform indicator.
- Prevent multiple simultaneous recording sessions.
- Retain the recorded input long enough to repeat transcription when the user
  chooses.

### STT-003 — Transcription output (P0)

- Display partial results for streaming models.
- Clearly distinguish partial and final text.
- Support copy, clear, repeat, and share.
- Display timestamps or segments when provided by the engine.
- Show detected/configured language and confidence only when meaningful.

### STT-004 — STT configuration (P0)

Expose supported options:

- Language or auto-detect.
- Streaming versus file mode.
- Voice activity detection.
- Endpointing/silence thresholds.
- Number of threads.

### STT-005 — STT metrics (P0)

Show:

- Audio duration.
- Total processing duration.
- Time to first partial result.
- Time to final result.
- RTF.
- Segment count.

### STT-006 — Reference comparison (P2)

Allow a reference transcript and compute normalized word or character error metrics.
Label the result as a convenience measurement rather than a standardized benchmark.

## 8. Text to speech

### TTS-001 — Synthesis input (P0)

- Multiline text input.
- Sample-text presets.
- Character or engine-limit indicator.
- Voice/model and speaker selection.

### TTS-002 — Synthesis and playback (P0)

- Synthesize locally.
- Play, pause, resume, stop, and replay.
- Display playback progress and audio duration.
- Respect Android audio focus.
- Stop and release audio resources when requested.

### TTS-003 — TTS configuration (P0)

Expose supported parameters:

- Speaker.
- Language.
- Speech rate.
- Pitch or model-supported prosody controls.
- Volume.
- Sentence silence.

Do not simulate controls that the selected model cannot honor.

### TTS-004 — TTS output (P0)

- Save generated audio in app-private history.
- Export a WAV file through the system document flow.
- Share generated audio through Android’s share sheet.
- Make generated-file retention behavior visible.

### TTS-005 — TTS metrics (P0)

Show:

- Time to first synthesized audio chunk when synthesis is streamed.
- Time to the first successful `AudioTrack` write.
- Time to first presentation by Android audio when the active route exposes a
  reliable timestamp.
- Total synthesis duration.
- Generated audio duration.
- RTF.
- Output sample rate.
- Playback underrun count for streamed output.

If first presentation cannot be measured, show it as unavailable rather than
substituting synthesis callback time.

### TTS-006 — Android system baseline (P2)

Optionally expose Android `TextToSpeech` as a comparison engine. Identify whether the
chosen system voice reports network requirements and do not label it local when
offline availability cannot be established.

## 9. Voice Assistant

### VOICE-001 — Pipeline configuration (P1)

Require compatible selections for:

- STT model and settings.
- LLM model, system prompt, and generation preset.
- TTS model/voice and settings.

Validate the complete pipeline before recording begins.

### VOICE-002 — Turn state machine (P1)

The UI exposes these states:

```text
Idle → Listening → Finalizing → Thinking → Speaking → Idle
              ↘ Error ←───────────────↗
```

Cancellation can return any active state to Idle after resources are released.

### VOICE-003 — Push-to-talk interaction (P1)

- Tap to start and tap to stop recording.
- Show live partial transcript.
- Show final user text and streaming assistant response.
- Begin speech synthesis after the complete response in the MVP.
- Allow the user to interrupt playback.

### VOICE-004 — Conversation continuity (P1)

- Maintain LLM conversation context across turns.
- Allow a new conversation.
- Make context truncation visible.
- Store each pipeline turn as linked STT, LLM, and TTS run data.

### VOICE-005 — Pipeline metrics (P1)

Show a latency breakdown:

- Listening duration.
- Speech finalization duration.
- STT finalization.
- LLM TTFT and completion.
- TTS first synthesized chunk, first output write, first presentation when available,
  and completion.
- End-to-end time until first audio presentation, or until first output write when
  presentation timing is unavailable.

### VOICE-006 — Automatic endpointing and barge-in (P2)

- Optional VAD-based end-of-turn detection uses configurable onset/offset
  hysteresis and model-required chunk sizes.
- Retain a bounded pre-speech buffer so confirmed utterances do not lose their first
  phonemes.
- Force-split or reject an utterance at a documented maximum duration to bound
  memory.
- Eager STT may start after a short silence, but the result is invalidated when
  speech resumes before the turn is confirmed.
- Empty or low-confidence STT returns the coordinator and VAD to a clean listening
  state.
- Stop TTS only after sustained new speech confirms barge-in; allow recovery from a
  brief false interruption.
- Apply a documented post-playback guard or acoustic echo-cancellation strategy so
  speaker output is not transcribed as user input.
- Expose automatic endpointing, interruption, recovery, and forced-split events in
  run details.

## 10. Runs

### RUN-001 — Run history (P1)

List successful, cancelled, and failed runs with:

- Capability.
- Date and duration.
- Engine/model snapshot.
- Short input/output preview.
- Status.
- Key metric.

Support filters for capability, model, status, and date.

### RUN-002 — Run details (P1)

Display:

- Input and output.
- Engine/model identity and version snapshot.
- Requested/effective backend, effective thread count, and fallback reason.
- Effective parameters.
- Cold/warm state.
- Device and thermal snapshot.
- Metrics.
- Error details.
- Linked pipeline runs.

### RUN-003 — Repeat run (P1)

Open the relevant playground with saved input and parameters. If the original model
is missing or no longer compatible, explain the mismatch and allow model
replacement before running.

### RUN-004 — Export (P1)

- Export an individual run as a versioned JSON document.
- Share through the Android share sheet.
- Exclude source audio and generated audio by default; attach them only after
  explicit user selection.
- Clearly warn that prompts, transcripts, and outputs may be sensitive.

### RUN-005 — Compare runs (P2)

Compare compatible runs side by side. Highlight settings and device-state
differences instead of implying that unlike workloads are directly comparable.

## 11. Device

### DEV-001 — Device profile (P0)

Display:

- Manufacturer and model.
- Android/API version.
- Supported ABIs and relevant CPU features available to the runtime.
- Total and available memory.
- Available app storage.
- Battery and charging state.
- Current thermal status where available.

### DEV-002 — Engine status (P0)

For each compiled or system engine, display:

- Engine name and version.
- Runtime availability.
- Supported capabilities and formats.
- Supported acceleration/backend information.
- Requested and effective backend/provider, thread count, and any fallback reason
  from the most recent initialization.
- Initialization or compatibility errors.

For Gemini Nano/AICore integrations, display support, download, and availability
state rather than assuming presence.

### DEV-003 — Diagnostics (P1)

Run non-destructive checks:

- App-private model directory is writable.
- Sufficient temporary space is available.
- Installed model files still exist.
- Model manifests/checksums remain valid where available.
- Engine libraries load.
- Offline readiness is known.

Diagnostics must not automatically load a multi-gigabyte model.

## 12. Settings

### SET-001 — General settings (P1)

- Theme: system, light, dark.
- Keep screen awake during active inference.
- Confirm before deleting models and runs.
- Default retention for recordings and generated audio.
- Advanced controls visibility.

### SET-002 — Performance defaults (P1)

- Preferred thread-count policy.
- Automatic model unload policy.
- Whether to warm up a selected model.
- Default metric detail level.

Engine-safe defaults take precedence over a global preference.

### SET-003 — Storage and privacy (P1)

- Show disk usage by models, recordings, generated audio, and history.
- Clear temporary files.
- Clear run/conversation history separately from models.
- State that inference data stays on device.
- List the exact features that can use network access.

## 13. Android platform integrations

### PLAT-001 — Provide Android speech recognition service (P2)

- Optionally expose an installed compatible STT model through Android
  `RecognitionService`.
- Report supported languages and model-download state through the relevant platform
  APIs.
- Keep recognition, microphone, cancellation, and caller-attribution lifecycle
  separate from the interactive playground.
- Never silently substitute a system/network recognizer when the selected local
  model is unavailable.

### PLAT-002 — Provide Android text-to-speech service (P2)

- Optionally expose installed compatible voices through Android
  `TextToSpeechService`.
- Use a TTS-only installation profile rather than requiring the complete voice
  pipeline.
- Stream correctly formatted PCM within platform callback buffer limits.
- Map language, voice, speaker, and speech-rate support without advertising
  unsupported values.
- `onStop()` and service destruction cancel synthesis and release owned native
  resources.

Both integrations live in dedicated modules and contribute their dependencies with
Metro. They are distinct from using Android system STT/TTS engines as playground
baselines.

## 14. Post-MVP candidates

- Additional engines, including ML Kit GenAI Prompt API and Android system speech
  baselines.
- Structured LLM output.
- Multimodal prompts.
- Prompt and parameter preset library.
- Batch transcription and benchmark suites.
- Model download sources supplied by the user.
- RAG over local documents.
- Exportable benchmark reports.
- Wake word and hands-free assistant mode.
