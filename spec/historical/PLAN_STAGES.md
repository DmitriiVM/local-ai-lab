# Local AI Lab — Implementation Stages

> [!WARNING]
> **Historical planning document — not current project documentation.**
>
> This staged delivery plan was created during the initial AI-assisted development
> phase and is preserved to show the project's evolution. Its stage status and exit
> criteria may no longer apply. Refer to the repository [README](../../README.md) and
> current source code for authoritative information.

## 1. Delivery rules

- Build one usable vertical slice at a time.
- A stage is complete only when its exit gate passes on a physical device where
  native inference is involved.
- Later stages may begin only when their required foundation is stable.
- Keep temporary/mock implementations clearly named and replace them before the
  relevant feature gate.
- Do not optimize inference without recorded measurements.
- Do not enable a curated model download until its files, checksum, source, and
  distribution license are documented.
- Automated tests are not part of these stages unless separately requested; the
  listed verification is manual.

## 2. Stage overview

| Stage | Outcome | Main requirements |
| --- | --- | --- |
| 0 | Decisions and native feasibility | DEV-001, DEV-002, decision log |
| 1 | Navigable application foundation | CORE-001–007, CORE-010 |
| 2 | Model lifecycle and diagnostics | MOD-001–002, MOD-004–008, DEV-001–003; MOD-003 conditional |
| 3 | Local LLM chat vertical slice | CHAT-001–005 |
| 4 | Local speech-to-text vertical slice | STT-001–005 |
| 5 | Local text-to-speech vertical slice | TTS-001–005 |
| 6 | Voice assistant pipeline | VOICE-001–005 |
| 7 | Persistence, runs, and export | CORE-011, CHAT-006, RUN-001–004, SET-001–003 |
| 8 | Hardening and MVP release | all P0/P1 requirements |
| 9 | Post-MVP experiments | P2 requirements |

## 3. Stage 0 — Resolve decisions and prove native feasibility

### Goal

Remove the highest-risk unknowns before building significant UI or persistence.

### Work

- Record the initial reference device and its RAM/ABI/API level.
- Select one small legally distributable or user-imported model bundle for each
  capability.
- Begin with the provisional candidates in
  [PLAN_MODELS.md](PLAN_MODELS.md): Qwen3 1.7B Q4_K_M, Whisper Base multilingual
  INT8, Silero VAD, and Supertonic 3 INT8.
- Prove one local STT, LLM, TTS, and VAD inference on the reference device. English
  and Russian quality validation is deferred to the project owner under D-025; do
  not combine models merely because each is described as multilingual.
- Confirm exact llama.cpp and sherpa-onnx revisions.
- Record inspected reference snapshots and adopted/rejected techniques in
  [PLAN_REFERENCES.md](./PLAN_REFERENCES.md).
- Confirm NDK/CMake compatibility with the current Android Gradle Plugin.
- Create the AI API and implementation module boundaries needed by the spikes.
- Build or integrate arm64 native libraries in their owning AI modules, not `:app`.
- Run a minimal non-Compose spike for:
  - GGUF metadata load and short token generation.
  - A sherpa-supported streaming microphone/file STT model.
  - TTS to PCM/WAV with Supertonic 3 for both English and Russian, comparing
    language-specific English and Russian Piper voices as lower-footprint options.
- Measure approximate native load memory, cold/warm load time, cancellation
  behavior, requested/effective backend, and fallback reason.
- For streamed TTS, separately measure first native chunk, first `AudioTrack` write,
  first Android audio presentation when available, and underruns.
- Confirm the exact file roles, frontend assets, audio formats, and adapter quirks
  required by each known-good model profile.
- Do not add `speech-core` as a production dependency unless a separate measured
  spike demonstrates a capability or performance benefit over the direct
  sherpa-onnx adapters.
- Decide whether curated download is enabled for the MVP or import-only initially.
- Confirm each candidate public source works without credentials, pin immutable
  revisions/files, and record sizes, SHA-256 hashes, licenses, and mirrors.
- Update [PLAN_DECISIONS.md](PLAN_DECISIONS.md) with exact results.

### Deliverables

- Reproducible native build/integration instructions in project configuration or
  adjacent developer documentation.
- Pinned runtime revisions.
- Known-good model bundle definitions.
- Updated catalog states in [PLAN_MODELS.md](PLAN_MODELS.md), including why each
  provisional candidate was accepted, replaced, or left research-only.
- Initial compatibility and performance observations.
- Initial STT-only, TTS-only, LLM-only, and voice-pipeline profile definitions where
  the selected model set supports them.
- No production UI is required.

### Exit gate

- Each primary engine completes one inference on the reference physical device.
- Each engine can be unloaded and recreated without restarting the app.
- Native engine implementations do not live in `:app`.
- Model files are loaded without copying their complete contents into JVM memory.
- The selected models have understood file requirements and licenses.
- Requested and effective runtime backend are observable rather than silently
  inferred.
- Any blocker that changes MVP scope is resolved in the decision log.

## 4. Stage 1 — Application foundation

### Goal

Create a stable Compose shell and shared state conventions before feature screens
diverge.

### Work

- Add Navigation 3 runtime, UI, lifecycle ViewModel integration, and Kotlin
  serialization configuration.
- Define serializable feature-owned `NavKey` types and module-owned navigation-entry
  contributions.
- Implement root Navigation 3 state in `:app`, with separate retained back stacks for
  Playground, Models, Runs, and Device.
- Add the root `NavDisplay`, system back behavior, and saveable-state plus
  ViewModel-store NavEntry decorators.
- Implement adaptive bottom navigation/navigation rail behavior behind the app
  navigation layout boundary.
- Add application theme, common dimensions, status components, and error UI.
- Define operation state, domain error, capability, engine, and model primitives.
- Create the initial core, AI, source, and feature Gradle modules and enforce their
  dependency direction.
- Configure Metro consistently in modules that declare or consume graph bindings.
- Define `AppScope` in `:core:di`.
- Add the root `@DependencyGraph(AppScope::class)` in `:app`.
- Create the graph in `:app` with only app-specific runtime values: `Application`,
  BuildConfig-derived configuration, and the application coroutine scope.
- Provide Metro’s ViewModel factory at the root Compose hierarchy.
- Add feature-owned placeholder ViewModels and resolve them in feature routes using
  `metroViewModel()`.
- Contribute each implementation from its owning module using `@BindingContainer`
  with `@ContributesTo` or `@ContributesBinding`.
- Create placeholder capability cards driven by real engine availability probes.
- Establish coroutine/Flow collection and one-shot effect conventions.
- Add privacy/onboarding copy without forcing model installation.
- Define foreground/background interruption behavior for active operations.

### Deliverables

- Navigable application.
- Shared loading, progress, empty, unsupported, and error states.
- Real device/engine availability summary, even before full diagnostics.
- A Metro graph assembled from module-owned contributions.

### Exit gate

- All destinations survive rotation/configuration changes without crashes.
- The selected top-level destination and all restorable stacks survive process
  recreation.
- Switching top-level destinations preserves their independent back stacks.
- Back navigation is predictable.
- Unsupported capability states are distinguishable from missing-model states.
- Backgrounding an active placeholder operation resolves it to a documented state.
- `:app` contains only the root graph, graph creation, app runtime bindings, and app
  shell concerns.
- `:app` does not import or construct a feature ViewModel or register any feature,
  source, AI, or core implementation.
- Each feature route obtains its feature-owned ViewModel with `metroViewModel()`.
- A feature ViewModel is scoped to its Navigation 3 entry and is cleared when that
  entry is removed.
- `:app` aggregates feature navigation contributions without constructing feature
  routes or importing feature ViewModels.
- Removing a required module contribution causes a compile-time graph error rather
  than a runtime service-locator failure.
- No long-running work is performed on the main thread.

## 5. Stage 2 — Model lifecycle and device diagnostics

### Goal

Make model installation, compatibility, and deletion reliable before playgrounds
depend on them.

### Work

- Implement app-private storage layout.
- Define the versioned model manifest.
- Define typed family/runtime profiles, semantic file roles, capability metadata,
  and model-specific audio requirements.
- Implement installed-model repository and Room records.
- Contribute source implementations from their owning source modules; add no source
  registrations to `:app`.
- Implement document-picker import and transactional copy.
- Support multi-file model bundles.
- Support capability-specific installation profiles and transactional ownership of
  any shared files.
- Add engine-specific metadata validation.
- Implement model list and model details.
- Implement compatibility checks for engine, ABI, files, storage, and estimated RAM.
- Implement explicit load/unload coordination.
- Implement safe deletion.
- Add Device profile, engine status, and diagnostics.
- If approved in Stage 0, implement the bundled curated catalog, shared downloader,
  API 34+ user-initiated data-transfer scheduler, and API 26–33 foreground
  WorkManager fallback.

### Deliverables

- Models can be imported, validated, listed, inspected, loaded, unloaded, and deleted.
- Downloads are either working with integrity checks or explicitly deferred.
- Device screen explains why each capability is ready or unavailable.

### Exit gate

- Cancelling an import/download leaves no installed partial model.
- Restarting the app reconstructs installed-model state.
- Missing or manually corrupted model files are detected.
- A profile is ready only when all of its own required files are valid.
- Removing a catalog entry cannot delete locally installed files.
- Deletion reclaims expected storage and preserves run-independent metadata.
- A model incompatible with the current engine/ABI cannot be loaded.

## 6. Stage 3 — Local LLM chat

### Goal

Deliver the first complete, measurable AI playground.

### Work

- Finalize `ChatEngine` and llama.cpp adapter.
- Implement model load, prompt formatting, tokenization/counting, generation,
  cancellation, and unload.
- Build conversation UI with streaming output.
- Add system prompt and supported generation controls.
- Implement safe default values and parameter validation.
- Implement context usage and truncation behavior.
- Capture TTFT, prompt speed, generation speed, counts, finish reason, and cold/warm
  status.
- Store the current run in memory; durable history comes in Stage 7.

### Deliverables

- A user can select a GGUF model and complete multi-turn local chat.
- Generation can be stopped.
- Effective settings and metrics are visible.

### Exit gate

- Chat works in airplane mode with an installed model.
- Cancelling generation returns the engine to a usable state.
- Repeated generation does not duplicate tokens or messages.
- Context overflow is prevented or handled according to the documented strategy.
- The main thread remains responsive during load and generation.
- Load/run/unload/reload succeeds repeatedly on the reference device.

## 7. Stage 4 — Local speech to text

### Goal

Deliver live and file-based offline transcription.

### Work

- Finalize `SpeechToTextEngine` and sherpa-onnx STT adapter.
- Implement microphone permission-at-use flow.
- Implement `AudioRecord` capture, PCM chunking, levels/waveform, and backpressure.
- Implement partial and final transcription events.
- Add language and engine-supported STT controls.
- Add optional VAD/endpoint configuration when supported by the chosen model.
- Implement document-picker audio import and decoding/resampling.
- Capture audio duration, partial/final latency, total duration, and RTF.
- Decide temporary recording retention behavior.

### Deliverables

- Microphone transcription with partial results.
- File transcription for documented supported formats.
- Copy/share transcript and repeat transcription.

### Exit gate

- Live and file transcription work in airplane mode.
- Denied microphone permission produces a recoverable state.
- Stop/cancel releases the microphone promptly.
- Long input uses bounded memory.
- Imported unsupported audio fails with an actionable error.
- RTF and durations are based on a monotonic clock and correct audio duration.

## 8. Stage 5 — Local text to speech

### Goal

Deliver offline speech generation, playback, and export.

### Work

- Finalize `TextToSpeechEngine` and sherpa-onnx TTS adapter.
- Implement model, voice, speaker, and supported parameter selection.
- Generate PCM and valid WAV output.
- Implement audio focus, playback, pause/resume/stop, and progress.
- Separate synthesis lifecycle from playback lifecycle.
- Retain a compatible streaming `AudioTrack` for the turn, keep its queue bounded,
  clamp PCM conversion, drain by presented frames, and track underruns.
- Capture first synthesized chunk, first audio write, first presentation when
  available, synthesis duration, output duration, sample rate, and RTF.
- Implement document-based WAV export and share flow.
- Implement generated-audio retention and cleanup rules.

### Deliverables

- Enter text, synthesize locally, play, stop, replay, and export.
- Metrics and effective settings are visible.

### Exit gate

- Synthesis and playback work in airplane mode.
- Cancellation releases engine and audio resources.
- Exported WAV opens in an external player.
- Audio focus loss pauses or stops according to the documented behavior.
- Repeated synthesis does not leak generated files or native memory.
- Streamed chunks play without gaps caused by recreating `AudioTrack` per chunk.
- Metrics never label the synthesis callback as physical playback start.

## 9. Stage 6 — Voice assistant pipeline

### Goal

Combine independently stable capabilities into an interruptible conversational
pipeline.

### Work

- Implement the pipeline coordinator and explicit turn state machine.
- Validate all selected models before listening.
- Connect microphone/VAD to STT finalization.
- Pass final transcript into a persistent in-memory chat context.
- Pass the completed LLM response into TTS.
- Implement playback interruption and return to Idle.
- Display transcript, assistant response, current phase, and latency timeline.
- Store linked in-memory component-run identifiers for Stage 7 persistence.
- Define recovery behavior for failure at each pipeline stage.

### Deliverables

- Push-to-talk local voice conversation.
- Per-stage progress and metrics.
- Safe cancel/interrupt behavior.

### Exit gate

- A complete STT → LLM → TTS turn works in airplane mode.
- No engine receives incompatible simultaneous work.
- Cancelling from every active state returns to Idle.
- A failure in one stage does not require an app restart to begin a new turn.
- Multiple turns preserve chat context and apply documented truncation.

## 10. Stage 7 — Persistence, runs, settings, and export

### Goal

Turn successful demos into a reproducible playground.

### Work

- Finalize Room schema for conversations, messages, runs, metrics, errors, and
  pipeline links.
- Persist successful, failed, and cancelled runs.
- Recover interrupted active runs after process death.
- Implement run list, filters, and run details.
- Connect recent activity on the Playground screen to persisted runs.
- Implement repeat-run prefill and missing-model handling.
- Implement versioned JSON export and share flow.
- Persist conversations and incomplete/cancelled assistant messages safely.
- Implement DataStore settings.
- Implement storage usage and separate cleanup actions.
- Review backup exclusion for models and sensitive content.

### Deliverables

- Durable conversations and run history.
- Reproducible configuration snapshots.
- Individual run export.
- User-controlled storage and retention.

### Exit gate

- Process death does not leave a run permanently marked running.
- Runs remain readable after their model is deleted.
- Repeat run restores effective settings and reports incompatibilities.
- Exported JSON includes a schema version and contains no audio unless selected.
- Clearing history does not remove models, and clearing temporary media does not
  corrupt run metadata.

## 11. Stage 8 — Hardening and MVP release

### Goal

Validate that the complete application is safe, understandable, and stable across
the supported MVP matrix.

### Work

- Resolve every open decision marked “Before MVP release.”
- Verify all P0 and P1 requirements in
  [PLAN_FEATURES.md](PLAN_FEATURES.md).
- Run the physical-device and lifecycle matrix from
  [PLAN_TECH.md](PLAN_TECH.md).
- Verify fresh install, update, backup policy, and data cleanup.
- Verify low-storage, corrupted-model, probable low-memory, audio interruption,
  cancellation, and engine recreation scenarios.
- Verify airplane-mode behavior for every installed workflow.
- Inspect APK/AAB native libraries and size.
- Review licenses and attribution for engines and curated models.
- Review accessibility with TalkBack and large fonts.
- Establish numeric performance observations for documented reference
  device/model combinations.
- Remove spike-only and debug-only controls from release UI.

### Deliverables

- Release candidate.
- Supported-device/model statement.
- Known limitations.
- License and attribution inventory.
- Recorded manual verification results.

### Exit gate

- All MVP success criteria in [PLAN.md](PLAN.md) pass.
- No known issue can corrupt an installed model or leave microphone/audio/native
  resources stuck across normal recovery.
- Offline claims match observed network behavior.
- Unsupported devices receive actionable explanations.
- Known limitations and supported configurations are documented.

## 12. Stage 9 — Post-MVP work

Candidates are prioritized only after observing MVP use:

- Side-by-side run and model comparison.
- ML Kit Gemini Nano engine.
- Android system STT/TTS baselines.
- Sentence-level streaming LLM → TTS.
- Automatic endpointing with VAD hysteresis, bounded pre-speech audio, forced
  utterance splitting, recoverable eager STT, clean empty-result reset, and
  post-playback suppression.
- Acoustic barge-in with sustained-speech confirmation and brief-interruption
  recovery.
- Optional Android `RecognitionService` and `TextToSpeechService` integrations in
  dedicated modules.
- Reference transcript WER/CER convenience metrics.
- Prompt/configuration presets.
- Batch benchmark runner and report export.
- User-configured model catalogs.
- LiteRT-LM and a supported `.litertlm` model as the first accelerated LLM
  comparison, after the llama.cpp slice is stable.
- RAG and local document workflows.

Each candidate should receive its own requirement update, decision entry, and stage
exit criteria before implementation.

## 13. Cross-stage dependency notes

- Model management precedes all production playgrounds.
- Chat, STT, and TTS must work independently before Voice Assistant integration.
- Persistent run schemas should be finalized only after real engine metrics are
  observed, but in-memory run domain models begin in Stage 1.
- Curated downloads are independent of inference and may be deferred without
  blocking user-imported model workflows.
- System engines must not become silent fallbacks for custom engines.
- Android system-engine baselines and exposing this app as a platform speech service
  are separate features and modules.
