# Local AI Playground — Technical Plan

## 1. Current project baseline

- Android application.
- Kotlin.
- Jetpack Compose with Material 3.
- The project currently starts with a single `:app` module.
- `minSdk 26`, `targetSdk 36`, `compileSdk 36.1`.
- Gradle version catalog.
- Java 11 bytecode target.

Stage 1 introduces the planned Gradle modules and Metro graph. Feature, source, AI,
and core implementations must not be placed or registered in `:app`.

## 2. Technical goals

- Fully local inference after model installation.
- Replaceable inference engines behind stable Kotlin contracts.
- Streaming and cancellable operations based on coroutines and `Flow`.
- Safe management of multi-gigabyte model files.
- Explicit ownership and release of native memory, microphone, and playback.
- Reproducible run records with comparable metrics.
- Graceful behavior on unsupported or resource-constrained devices.
- Module-owned implementations and Metro binding contributions.
- A composition root that contains no feature, source, AI, or core registrations.

## 3. Initial runtime decisions

The MVP runtime direction is:

| Capability | Primary engine | Model family/format | Reason |
| --- | --- | --- | --- |
| LLM | `llama.cpp` | GGUF | Broad model ecosystem, quantization support, Android example, streaming tokens |
| STT | `sherpa-onnx` | Engine-compatible ONNX model bundle | Offline streaming, file transcription, VAD, Android support |
| TTS | `sherpa-onnx` | Engine-compatible ONNX voice bundle | Reuses the speech runtime and supports local synthesis |
| VAD | `sherpa-onnx` | Engine-compatible VAD model | Avoids a third speech runtime |

ML Kit GenAI/Gemini Nano and Android system speech/TTS are comparison engines, not
MVP blockers. Gemini Nano is unavailable on many devices and has system-service,
foreground, and quota constraints. The core playground must still operate on a
compatible device without it.

LiteRT-LM is the first planned post-MVP LLM comparison engine. It uses the same
engine-neutral contracts and is added in its own implementation module only after
the llama.cpp vertical slice is stable. ExecuTorch and other native runtimes remain
deferred until a required model or measured capability justifies their additional
packaging and lifecycle surface.

The provisional starter catalog and model-governance rules are defined in
[PLAN_MODELS.md](./PLAN_MODELS.md). Exact revisions, files, licenses, checksums, and
download approval remain governed by [PLAN_DECISIONS.md](./PLAN_DECISIONS.md) and
Stage 0 measurements.

## 4. Supported device baseline

- `arm64-v8a` is the primary MVP inference ABI.
- API 26 remains the application minimum, but individual engines may report a
  stricter capability requirement.
- A device with insufficient RAM may install the app but must be prevented from
  loading incompatible models where the risk can be identified.
- x86_64 emulator support is optional for native inference; UI development must not
  depend on it.
- 32-bit native inference is outside MVP scope.

Runtime capability checks, rather than API level alone, determine whether a
playground is ready.

## 5. Application architecture

Use unidirectional data flow:

```text
Compose UI
    ↓ user action
ViewModel
    ↓ use case
Domain service / repository
    ↓ engine-neutral request
Engine adapter
    ↓
JNI / system API / file and audio runtime

Engine events → repository/use case → ViewModel state → Compose UI
```

### 5.1 Layers

#### UI

- Compose screens, navigation, dialogs, and reusable controls.
- ViewModels own screen state and invoke use cases.
- UI does not import native runtime types.
- Navigation 3 keys and entries are owned by their feature modules.

#### Domain

- Engine-neutral model, request, event, metric, and error types.
- Use cases for model installation, inference, cancellation, history, and
  diagnostics.
- Capability rules and effective-configuration validation.

#### Data

- Model catalog and installed-model repository.
- Room persistence for conversations, messages, and runs.
- DataStore for preferences and lightweight defaults.
- App-private files for models, temporary downloads, recordings, and generated
  audio.

#### Engine adapters

- Llama.cpp LLM implementation.
- Sherpa-onnx STT, TTS, and VAD implementations.
- Later system-engine adapters.
- Mapping between runtime errors/settings and domain types.

### 5.2 Gradle module structure

Use Gradle modules from the application-foundation stage:

```text
:app
├── :core:di
├── :core:model
├── :core:result
├── :core:audio
├── :core:voice
├── :ai:api
├── :ai:llamacpp
├── :ai:sherpa
├── :ai:system
├── :source:database
├── :source:models
├── :source:runs
├── :source:settings
├── :feature:playground
├── :feature:models
├── :feature:chat
├── :feature:stt
├── :feature:tts
├── :feature:voice
├── :feature:runs
├── :feature:device
└── :feature:settings
```

When PLAT-001 or PLAT-002 is scheduled after MVP, add
`:platform:recognition-service` or `:platform:tts-service` respectively. Do not
create empty platform modules during the foundation stage.

The exact split may combine very small API-only core/source modules, but these
ownership rules are fixed:

- `:app` is the composition root and application shell.
- A feature module owns its routes, screen UI, concrete ViewModels, and
  feature-specific use cases.
- An AI implementation module owns its engine adapter, JNI/native integration, and
  corresponding Metro bindings.
- A platform integration module owns an optional exported Android service and its
  Metro bindings; these modules are post-MVP and are not assembled by default.
- A source module owns its repository/data-source implementations and bindings.
- A core implementation is contributed from its own core module.
- Shared interfaces and immutable domain types live in the narrowest API/core module
  needed by their consumers.
- Feature modules do not depend on other feature implementations.
- Implementation modules may be Gradle dependencies of the assembled app so Metro
  can aggregate their contributions, but `:app` Kotlin code must not import their
  concrete implementation types.

### 5.3 Dependency direction

```text
:app
  ├── feature routes / navigation contracts
  ├── implementation modules present for Metro aggregation
  └── :core:di

:feature:* → core/domain APIs, AI APIs, source APIs
:ai:<impl> → :ai:api, required core APIs
:source:<impl> → source/core APIs
:core:<impl> → lower-level core APIs only
```

Avoid circular feature dependencies. Cross-feature workflows such as Voice Assistant
depend on shared AI/source contracts, not on Chat, STT, or TTS ViewModels.

### 5.4 Navigation 3

Use Android Navigation 3 as the application navigation foundation.

Responsibilities:

- `:app` owns the root navigation state, top-level back stacks, top-level navigator,
  `NavDisplay`, system back handling, and initial/deep-link key selection.
- Each feature owns its serializable, type-safe `NavKey` types, route composables,
  navigation-entry definitions, and destination-specific behavior.
- Feature navigation entries are exposed through a narrow navigation contract or
  Metro-contributed entry provider. `:app` aggregates them; it does not manually
  construct feature screens or ViewModels.
- Feature-to-feature navigation targets public keys/contracts rather than concrete
  screen or ViewModel types.

Use one retained `NavBackStack` per top-level destination: Playground, Models, Runs,
and Device. Switching top-level destinations preserves each stack. Settings may be
represented as an overlay/detail entry or a root-level destination without becoming
a fifth primary stack.

Navigation keys:

- Implement `NavKey`.
- Are serializable with Kotlin serialization.
- Contain only the stable identifiers required to restore a destination.
- Do not contain repositories, models, ViewModels, Android contexts, or large
  payloads.
- Use an ID to load model/run/conversation details from a source instead of embedding
  the complete object.

The root `NavDisplay` installs both standard state decorators:

```kotlin
NavDisplay(
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    ),
    // back stack, entries, and back handling
)
```

`rememberSaveableStateHolderNavEntryDecorator()` retains destination saveable state.
`rememberViewModelStoreNavEntryDecorator()` supplies a `ViewModelStoreOwner` per
navigation entry and clears its ViewModels when that entry is removed. This entry
owner is what a feature route’s `metroViewModel()` uses.

Navigation state restoration must cover:

- Selected top-level destination.
- A separate serializable stack for each top-level destination.
- Restorable keys and arguments after configuration change and process recreation.
- Removal or replacement of a key that references deleted/incompatible local data.

Deep links and external intents are parsed at the application boundary into validated
feature-owned keys. Features may contribute parsers/contracts, but untrusted URI
values are validated before a key is added to a back stack.

Add:

- `androidx.navigation3:navigation3-runtime`.
- `androidx.navigation3:navigation3-ui`.
- `androidx.lifecycle:lifecycle-viewmodel-navigation3`.

The Material 3 Adaptive Navigation 3 integration is optional until its selected
version is validated. Keep adaptive bottom-bar/navigation-rail selection behind an
app navigation layout boundary so a beta adaptive integration can be upgraded or
replaced without changing feature keys and routes.

### 5.5 Dependency injection with Metro

Use Metro as the only application dependency-injection framework.

The application graph uses `AppScope`, defined in `:core:di`. Each module creates and
contributes bindings for the implementations it owns:

```kotlin
@BindingContainer
@ContributesTo(AppScope::class)
object ModelsSourceBindings {
    @Provides
    fun provideModelRepository(
        implementation: DefaultModelRepository,
    ): ModelRepository = implementation
}
```

An injectable implementation with one clear binding may contribute itself directly:

```kotlin
@Inject
@ContributesBinding(AppScope::class)
class LlamaChatEngine(
    // Module-owned dependencies
) : ChatEngine
```

Binding rules:

- Use `@BindingContainer` plus `@ContributesTo(AppScope::class)` for a module-owned
  group of provider bindings.
- Use `@ContributesBinding(AppScope::class)` for an injectable implementation that
  directly implements its bound contract.
- A module must not contribute or provide another module’s concrete implementation.
- Do not create a central binding registry in `:app`.
- Do not use service locators or a parallel manual `AppContainer`.
- Scope mutable engine coordinators, repositories, and other application-lifetime
  objects deliberately; do not make every binding application-scoped by accident.

Metro aggregates all module contributions into the application graph. `:app` owns
only:

- The root `@DependencyGraph(AppScope::class)`.
- Graph creation and installation.
- App-specific runtime values such as `Application`, BuildConfig-derived
  configuration, and the application coroutine scope.
- Providing the graph’s `MetroViewModelFactory` at the root Compose hierarchy.

Runtime values passed during graph creation are not re-provided from feature, source,
AI, or core modules.

### 5.6 Feature ViewModels

A feature owns and contributes its concrete ViewModels. A ViewModel is resolved at
the feature route with `metroViewModel()`:

```kotlin
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ChatViewModel(
    private val chatEngine: ChatEngine,
) : ViewModel()

@Composable
fun ChatRoute(
    viewModel: ChatViewModel = metroViewModel(),
) {
    // Route collects state and renders the feature screen.
}
```

`:app` may install or navigate to a feature’s public route/navigation entry, but it
must not import, construct, expose, or register the feature’s concrete ViewModel.
Screen composables below the route receive state and callbacks rather than resolving
the graph themselves.

With Navigation 3, the feature route calls `metroViewModel()` inside its decorated
`NavEntry`. ViewModels are entry-scoped by default. Sharing a ViewModel between
multiple entries requires an explicit parent/owner design and must not happen
accidentally through Activity scope.

## 6. Engine contracts

Contracts must model streaming, lifecycle, and capability differences explicitly.
Illustrative shapes:

```kotlin
interface AiEngine {
    val descriptor: EngineDescriptor
    suspend fun probe(): EngineAvailability
}

interface ChatEngine : AiEngine {
    suspend fun load(model: InstalledModel, config: LoadConfig): LoadResult
    fun generate(request: ChatRequest): Flow<ChatEvent>
    suspend fun cancel()
    suspend fun unload()
}

interface SpeechToTextEngine : AiEngine {
    suspend fun load(model: InstalledModel, config: LoadConfig): LoadResult
    fun transcribe(input: AudioInput, config: SttConfig): Flow<SttEvent>
    suspend fun cancel()
    suspend fun unload()
}

interface TextToSpeechEngine : AiEngine {
    suspend fun load(model: InstalledModel, config: LoadConfig): LoadResult
    fun synthesize(request: TtsRequest): Flow<TtsEvent>
    suspend fun cancel()
    suspend fun unload()
}
```

Events are sealed domain types such as Preparing, Progress, PartialText,
GeneratedToken, AudioChunk, Completed, Cancelled, and Failed.

Do not force every engine to implement unsupported parameters. Each engine publishes
a capability descriptor containing supported settings, ranges, defaults, formats,
languages, streaming modes, and metadata availability.

Engine initialization publishes the requested and effective provider/backend,
effective thread count, runtime revision, and any fallback warning. A backend
fallback may be allowed by an engine-specific policy, but it is never silent and is
stored with the run.

## 7. Concurrency and resource ownership

- Use structured concurrency tied to ViewModel or application-owned services.
- Execute native inference off the main thread.
- Serialize mutable access to each native engine instance.
- Allow only one active generation per loaded LLM instance.
- Audio capture and playback have explicit single owners.
- A pipeline coordinator owns STT → LLM → TTS voice-assistant execution.
- Cancellation waits for native calls to reach a safe boundary and then releases
  resources.
- Audio callbacks use bounded queues or immediate delivery; slow cancellation or
  downstream playback must not block microphone ingestion.
- UI disappearance does not implicitly keep inference alive unless the operation has
  an explicit application-scoped owner.

Engine state follows:

```text
Unavailable | Unloaded → Loading → Loaded → Running
                          ↓          ↓        ↓
                         Error ← Unloading ← Cancelling
```

State transitions are validated rather than inferred from nullable native handles.

## 8. Native integration

### 8.1 llama.cpp

- Integrate through CMake/NDK and a narrow JNI facade.
- Build from a pinned upstream source revision and package the resulting native
  libraries in the APK/AAB.
- Keep C++ pointers behind opaque Kotlin handles.
- Expose metadata parsing, load, tokenization/counting, streaming generation,
  cancellation, and unload.
- Use the model’s chat template when available; record the effective template.
- Pin a known upstream revision rather than building from an unbounded branch.
- Document compile flags and engine revision in the app build.

### 8.2 sherpa-onnx

- Build from a pinned upstream revision or consume a checksum-pinned reproducible
  AAR, and package the resulting native libraries in the APK/AAB.
- Include only required ABIs.
- Wrap STT, TTS, and VAD native objects behind Kotlin adapters.
- Treat model bundles as manifests, not single arbitrary ONNX files.
- Validate required token, vocabulary, acoustic, speaker, frontend, vocoder, and
  config files before constructing native objects.
- Keep family-specific settings and quirks in typed runtime profiles. This includes
  endpoint rules, decoder settings, tail padding, chunk sizes, language frontend
  data, and provider libraries.
- Clamp floating-point audio before conversion to PCM16.
- Pin the runtime and expected model metadata versions.

### 8.3 speech-core evaluation boundary

`speech-android` and `speech-core` are architecture references, not primary MVP
dependencies. Do not introduce their complete voice pipeline alongside the direct
sherpa-onnx adapters without a measured spike demonstrating a distinct capability
or performance benefit.

If added later, a speech-core adapter lives in its own `:ai:<implementation>` module,
implements the same AI contracts, contributes itself with Metro, and does not
replace the Kotlin feature or navigation architecture.

### 8.4 Native safety

- Every native allocation has an idempotent close/unload path.
- No finalizer is relied on for timely release.
- JNI converts native errors into typed domain failures.
- Native callbacks must not update Compose state directly.
- Avoid copying complete model contents through Kotlin/Java memory.
- Log runtime crashes and failures locally without including prompt/audio content by
  default.

## 9. Model lifecycle and storage

### 9.1 Storage layout

Use app-private storage:

```text
files/
├── models/<model-id>/
│   ├── manifest.json
│   └── model files...
├── recordings/
├── generated-audio/
└── exports-staging/

cache/
├── model-downloads/
└── runtime-cache/
```

Imported documents are copied into app-private storage. Native engines must not rely
on a temporary document-provider URI or a permission grant that may later disappear.

### 9.2 Installation transaction

1. Inspect source metadata.
2. Confirm disk requirement and license/source information.
3. Copy/download into a unique temporary directory.
4. Verify expected file sizes and checksums when available.
5. Ask the engine adapter to validate metadata/compatibility.
6. Write the local manifest.
7. Atomically make the directory the installed model location.
8. Persist the installed-model record.

On failure or cancellation, remove only the operation’s validated temporary
directory. Never delete an existing installed model during replacement until the
new version is valid.

### 9.3 Model manifest

The versioned manifest includes:

- Schema version and stable model ID.
- Display name, family, capabilities, engine ID, and runtime-profile type.
- Format, quantization, architecture, and model revision.
- Required files, semantic file roles, relative paths, sizes, and hashes.
- Source and license metadata.
- Languages, speakers, input/output sample rates, PCM formats, context, or other
  capability metadata.
- Required frontend assets, provider libraries, minimum runtime revision, and known
  adapter quirks.
- Supported streaming, partial-result, endpointing, contextual-biasing, cancellation,
  and reference-audio behavior.
- Supported providers, recommended thread policy, and optional warm-up behavior.
- Import/catalog version and installation time.
- Optional approximate RAM, storage, and reference-device RTF observations, clearly
  separated from compatibility guarantees.

### 9.4 Downloads

- Use a user-initiated data-transfer job for long user-started model downloads on
  API 34+.
- Use a foreground WorkManager worker for the equivalent transfer on API 26–33.
- Keep scheduling behind one model-transfer interface; both paths invoke the same
  downloader and transactional installer.
- Use regular WorkManager for deferrable cleanup and short maintenance work.
- Use an HTTP client with streaming file output and cancellation.
- Accept only public credential-free curated URLs. Hugging Face entries use an
  immutable commit in `/resolve/<revision>/<path>`; GitHub entries identify the
  release asset. Never embed a shared service token.
- Store a bundled versioned catalog in the MVP. Do not fetch remote catalog updates
  until signature, rollback, and key-rotation policy is defined.
- Support HTTP range resumption only when the server and local partial metadata make
  it safe.
- Preserve a partial file only with enough source metadata to prove that it belongs
  to the same model revision. If a server ignores a requested range, restart the
  temporary file rather than appending duplicate content.
- Validate TLS, response status, content length when provided, and checksum.
- Handle HTTP 429 and transient server failures with server-directed or bounded
  exponential backoff.
- Follow redirects without persisting a temporary CDN redirect as the catalog
  source.
- Model downloads are the only planned routine network traffic.

### 9.5 Installation profiles and shared files

- One catalog model set may declare STT-only, TTS-only, LLM-only, and voice-pipeline
  profiles.
- A profile lists its required files and explicit shared dependencies.
- Profile readiness is computed independently; installing one profile never marks
  another ready.
- Shared binary reuse is permitted only through persisted ownership/reference data
  that makes deletion atomic and prevents dangling installed profiles.
- Removing or changing a remote catalog entry never removes local installed data.
- Replacement validates the complete new profile before changing active profile
  references.

## 10. Persistence

### 10.1 Room

Persist:

- Installed model records and file-manifest references.
- Conversations and messages.
- Run records.
- Effective parameter snapshots.
- Metrics and error snapshots.
- Links between voice-pipeline component runs.

Large prompts and transcripts can remain in Room. Audio and model binaries remain
files referenced by relative app-private paths.

### 10.2 DataStore

Persist:

- Theme.
- Advanced-control visibility.
- Default engine settings/presets.
- Retention and automatic-unload preferences.
- Onboarding and privacy acknowledgement state.

### 10.3 Schema compatibility

- Room migrations are explicit once a released schema exists.
- Exported JSON has an independent schema version.
- Engine/model snapshots stored with runs remain readable even after an engine or
  model is removed.

## 11. Domain data

Core domain entities:

- `EngineDescriptor`
- `EngineAvailability`
- `ModelDescriptor`
- `InstalledModel`
- `ModelCompatibility`
- `Conversation`
- `ChatMessage`
- `InferenceRun`
- `RunInput`
- `RunOutput`
- `RunParameters`
- `RunMetrics`
- `DeviceSnapshot`
- `InferenceError`

`InferenceRun` stores a snapshot of descriptive model data. It must not depend only
on a foreign key to a currently installed model.

Run statuses are `PREPARING`, `RUNNING`, `SUCCEEDED`, `CANCELLED`, and `FAILED`.
An app/process interruption is resolved to `CANCELLED` with an interruption reason
on the next startup.

## 12. Audio architecture

### 12.1 Capture

- Use `AudioRecord` for predictable PCM capture.
- Preferred STT input is mono PCM 16-bit at the sample rate required by the model,
  commonly 16 kHz.
- Resample in a shared audio layer when source and engine rates differ.
- Feed bounded chunks to streaming STT with backpressure.
- Calculate waveform/level summaries without retaining duplicate full audio buffers.

### 12.2 File input

- Use the document picker.
- Decode supported media to engine-required PCM.
- Report unsupported codecs or corrupt media.
- Preserve original duration and conversion details in run metadata.

### 12.3 TTS output

- Stream PCM through `AudioTrack` when the engine supports incremental audio.
- Retain one correctly configured streaming `AudioTrack` for a synthesis turn;
  recreating it for every native chunk is prohibited.
- Prefill the first safe chunk before playback where this reduces route warm-up
  gaps, while keeping queued audio bounded.
- Clamp floating-point samples before PCM16 conversion.
- Recreate output when sample rate, channel layout, encoding, or Android audio route
  makes the retained track incompatible.
- Track written and presented frames, drain by playback progress, and record
  underruns where the platform exposes them.
- Create a valid WAV file for retained/exported output.
- Use Android audio focus and react to focus loss.
- Keep synthesis state separate from playback state.

### 12.4 Voice assistant coordinator

The coordinator is an explicit state machine and owns temporary outputs. It does not
pass Compose UI objects between engines. The MVP waits for final STT, then complete
LLM output, then starts TTS. Streaming sentence-level LLM → TTS is a later
optimization because it complicates cancellation and response editing.

Post-MVP automatic turn detection extends this coordinator rather than bypassing it.
It uses bounded pre-speech audio, VAD hysteresis, a maximum utterance duration,
recoverable eager STT, complete empty-result reset, sustained-speech barge-in
confirmation, brief-interruption recovery, and a post-playback guard or explicit
echo-cancellation path. The exact thresholds belong to a selected model/runtime
profile and effective run snapshot.

## 13. Metrics

Use a monotonic clock for elapsed durations. Wall-clock time is metadata only.

### LLM

- Model load duration.
- Prompt tokens and prompt evaluation duration.
- TTFT.
- Generated tokens and generation duration.
- Prompt tokens/second and generated tokens/second.
- Total duration and finish reason.

### STT

- Audio duration.
- Time to first partial.
- Time to final result.
- Total processing duration.
- RTF and segment count.

### TTS

- Time to first synthesized audio chunk.
- Time to first successful `AudioTrack` write.
- Time to first Android audio presentation when a reliable monotonic audio timestamp
  is available.
- Total synthesis duration.
- Generated audio duration.
- RTF, sample rate, and streamed playback underruns.

These timestamps are not interchangeable. An unavailable presentation timestamp
remains unavailable rather than being replaced with callback time.

### Device snapshot

- Device/model and Android version.
- Selected engine and requested/effective backend.
- Effective thread count and acceleration fallback reason, if any.
- Available memory before and after, explicitly labeled approximate.
- Thermal state.
- Battery/charging state.
- Cold/warm classification.

Metrics instrumentation must have low overhead. Android process memory is an
estimate and must not be presented as exact native-model peak allocation.

## 14. Technical stack

Planned Android components:

- Compose UI and Material 3.
- Android Navigation 3 runtime and UI.
- Lifecycle ViewModel integration for Navigation 3 entry scoping.
- Optional Material 3 Adaptive Navigation 3 integration after version validation.
- Lifecycle ViewModel and runtime-compose integration.
- Metro compile-time dependency injection.
- MetroX ViewModel and Compose integration for `metroViewModel()`.
- Kotlin coroutines and Flow.
- Kotlin serialization for model manifests and JSON exports.
- Room for structured persistence.
- Preferences DataStore.
- `JobScheduler` user-initiated data-transfer jobs for model downloads on API 34+.
- Foreground WorkManager download fallback on API 26–33 and regular WorkManager for
  cleanup work.
- Android document APIs for import/export.
- `AudioRecord` and `AudioTrack`.
- NDK/CMake and JNI.

Prefer platform APIs over additional dependencies for simple functionality. Pin
native upstream revisions and record all new library versions in
`gradle/libs.versions.toml`.

## 15. Permissions and platform declarations

Expected manifest capabilities:

- `RECORD_AUDIO` for live STT and the voice assistant.
- `INTERNET` only when curated downloads are enabled.
- UIDT `JobService` declarations/permissions on API 34+ when curated downloads are
  enabled.
- Foreground-service declarations/permissions required by the API 26–33
  foreground-WorkManager download fallback. Inference itself remains
  foreground-only and does not use a foreground service.

Post-MVP `RecognitionService` and `TextToSpeechService` integrations are declared
only by their dedicated platform modules. Their exported-service permissions,
metadata, caller attribution, callback limits, cancellation, and destruction
behavior are validated separately from playground Activities.

Use the document picker and app-private storage instead of broad media/storage
permissions. Do not request microphone permission during startup.

## 16. Privacy and security

- No analytics or telemetry SDK in the MVP.
- No prompt, transcript, or audio content in normal logs.
- Redact file paths and content from user-shareable diagnostics where appropriate.
- Imported/downloaded files are untrusted input and must be validated before native
  parsing where practical.
- Catalog downloads require HTTPS and a pinned checksum in catalog metadata.
- Do not store or ship a maintainer/shared model-host token.
- Gated/private model hosts are not curated download sources.
- Remote catalog reconciliation cannot delete installed local model data.
- Export requires explicit user action and warns about sensitive content.
- App backup configuration must be reviewed before release so large models and
  sensitive history are not unintentionally backed up.

## 17. Error model and recovery

Typed categories:

- `PermissionFailure`
- `UnsupportedDevice`
- `UnsupportedModel`
- `MissingModelFile`
- `InsufficientStorage`
- `MemoryRisk`
- `DownloadFailure`
- `IntegrityFailure`
- `EngineUnavailable`
- `EngineInitializationFailure`
- `InferenceFailure`
- `AudioFailure`
- `Cancelled`
- `UnexpectedFailure`

Every error indicates whether retry, settings change, model replacement, or app
restart is appropriate. A native engine failure invalidates the engine handle until
it is safely recreated.

## 18. Quality and verification strategy

No automated test implementation is included unless separately requested. Each
stage nevertheless has a manual verification gate.

The release verification matrix should cover:

- At least one lower-memory, one mid-range, and one flagship arm64 physical device
  when available.
- Minimum API behavior and current target API behavior.
- Fresh install, upgrade, process death, and low-storage recovery.
- Airplane-mode inference after installation.
- Microphone permission denied and later granted.
- Model import/download cancellation and corrupted files.
- Cold and warm inference.
- Background/foreground transitions.
- Audio focus interruption.
- Streaming TTS callback, first write, first presentation, drain, and underrun
  observations on routes that expose the required timing.
- Thermal throttling observation.
- Model unload and repeated engine recreation.

Exact devices and models are recorded in `PLAN_DECISIONS.md` before release
hardening.

## 19. Performance and reliability budgets

Budgets depend on chosen reference devices/models and must be finalized after the
native spikes. Initial non-numeric requirements:

- UI remains responsive during load and inference.
- Streaming updates are batched if token callbacks would cause excessive
  recomposition.
- Cancellation produces visible feedback immediately and reaches a safe idle state.
- No unbounded in-memory buffering of model files or audio.
- Repeating load/run/unload does not show continuing native-memory growth.
- Low-storage and likely OOM conditions fail before destructive work where possible.

Numeric budgets are recorded only with an exact reference device, model,
quantization, input, temperature state, and cold/warm classification.

## 20. Build and dependency policy

- Use reproducible pinned dependency/native revisions.
- Keep native compiler flags visible in build configuration.
- Build release native code with symbols handled separately from distributed
  binaries.
- Do not download runtime code dynamically.
- Model files are data, not executable plugins.
- Review APK/AAB size and native ABI contents before release.

Reference implementations and the exact snapshots used to justify these constraints
are recorded in [PLAN_REFERENCES.md](./PLAN_REFERENCES.md).
