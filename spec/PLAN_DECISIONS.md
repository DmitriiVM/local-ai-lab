# Local AI Playground — Decisions and Open Questions

## 1. Purpose

This document is the authoritative log for choices that affect more than one plan
file. It prevents product, technical, and staging documents from silently making
different assumptions.

Decision states:

- **Accepted** — implementation should follow the decision.
- **Provisional** — current direction, to be confirmed by a named stage.
- **Open** — implementation is blocked at the stated point.
- **Resolved** — an earlier open question answered by an accepted decision.
- **Superseded** — retained for history and linked to its replacement.

When a decision changes, do not rewrite its history. Mark it Superseded, add the
replacement, and update the affected plan files.

## 2. Accepted decisions

### D-001 — Android-first application

- **State:** Accepted
- **Decision:** Build a native Android application using Kotlin and Jetpack Compose.
- **Reason:** The existing project is an Android Compose shell and the target
  runtimes provide Android/native integration paths.

### D-002 — Meaning of local inference

- **State:** Accepted
- **Decision:** Prompts, audio, and outputs remain on-device. Network is permitted
  only for explicit model downloads and opening documentation/license links.
- **Consequence:** There is no automatic cloud fallback. Installed workflows must
  be manually verifiable in airplane mode.

### D-003 — Primary navigation

- **State:** Accepted
- **Decision:** Use Playground, Models, Runs, and Device as primary destinations;
  Settings is a top-level secondary destination.
- **Reason:** Capabilities are launched from one place while model and diagnostic
  concerns remain shared across capabilities.

### D-004 — Engine abstraction

- **State:** Accepted
- **Decision:** UI/domain layers depend on engine-neutral contracts. Each engine
  advertises supported capabilities and parameters.
- **Consequence:** Unsupported controls are not emulated or assumed.

### D-005 — Initial engine direction

- **State:** Accepted
- **Decision:** Use llama.cpp for GGUF LLMs and sherpa-onnx for STT, TTS, and VAD.
  Treat Gemini Nano and Android system speech engines as later comparison adapters.
- **Reason:** The core MVP must not depend on limited system-model availability, and
  one speech runtime reduces integration surface.

### D-006 — No large bundled model

- **State:** Accepted
- **Decision:** Do not embed large model files in the base APK.
- **Consequence:** Users import models or explicitly download from an approved
  catalog. The app can launch without an installed model.

### D-007 — Model storage

- **State:** Accepted
- **Decision:** Copy imported/downloaded model bundles into app-private storage and
  give native engines stable file paths.
- **Reason:** Document-provider URI access and permissions may be transient, and
  native runtimes often require paths or random access.

### D-008 — Primary MVP ABI

- **State:** Accepted
- **Decision:** Target `arm64-v8a` for MVP native inference. Do not promise 32-bit
  support.
- **Consequence:** ABI incompatibility is reported as an unsupported capability,
  not as a load failure.

### D-009 — Dependency injection

- **State:** Superseded by D-014
- **Previous decision:** Begin with an application-scoped manual `AppContainer`.
- **Reason superseded:** Module-owned Metro contributions are now a project
  requirement.

### D-010 — Voice pipeline streaming

- **State:** Accepted
- **Decision:** MVP waits for final STT, complete LLM response, then performs TTS.
- **Reason:** Sentence-level streaming adds segmentation, ordering, playback, and
  cancellation complexity. It is a post-MVP latency optimization.

### D-011 — No silent fallback

- **State:** Accepted
- **Decision:** Failure or unavailability of one engine never causes automatic use
  of another engine. A lower-level provider/backend fallback is permitted only when
  declared by that engine’s policy.
- **Consequence:** The user explicitly chooses a replacement engine. Any permitted
  provider fallback exposes and stores the requested backend, effective backend,
  effective thread count, and fallback reason.

### D-012 — Run snapshots

- **State:** Accepted
- **Decision:** Runs store descriptive model, engine, parameter, and device
  snapshots rather than relying only on live foreign keys.
- **Reason:** Historical results remain meaningful after model deletion or engine
  upgrades.

### D-013 — Foreground-only playground operations

- **State:** Accepted
- **Decision:** Microphone capture, native inference, synthesis, and playground audio
  are foreground operations in the MVP. Backgrounding requests safe cancellation;
  user-started WorkManager model downloads may continue.
- **Reason:** Background inference is outside MVP scope and should not consume
  substantial resources without visible controls.

### D-014 — Metro and module-owned bindings

- **State:** Accepted
- **Decision:** Use Metro for dependency injection. Each feature, source, AI, or core
  module creates and contributes bindings for its own implementations with
  `@BindingContainer` plus `@ContributesTo(AppScope::class)`, or
  `@ContributesBinding(AppScope::class)` for a directly injectable implementation.
- **Application ownership:** `:app` owns only the root
  `@DependencyGraph(AppScope::class)`, graph creation, and app-specific runtime
  values such as `Application`, BuildConfig-derived configuration, and the
  application coroutine scope.
- **ViewModel ownership:** A feature owns its concrete ViewModels and obtains them in
  its route with `metroViewModel()`. `:app` does not import, construct, or register
  feature ViewModels.
- **Consequence:** There is no central implementation registry or parallel service
  locator in `:app`; Metro aggregates module contributions into the root graph.

### D-015 — Multi-module ownership from the foundation stage

- **State:** Accepted
- **Decision:** Establish core, AI, source, and feature Gradle module boundaries in
  Stage 1. Native spikes created in Stage 0 already live in their owning AI modules.
- **Reason:** Metro contribution ownership and the prohibition on implementation
  registration in `:app` require real module boundaries, not package conventions.

### D-016 — Android Navigation 3

- **State:** Accepted
- **Decision:** Use Android Navigation 3 for application navigation.
- **Application ownership:** `:app` owns root navigation state, separate top-level
  back stacks, the top-level navigator, `NavDisplay`, system back handling, and
  external-intent/deep-link conversion into validated keys.
- **Feature ownership:** Each feature owns its serializable `NavKey` types, route,
  navigation-entry definitions, and concrete ViewModels. Feature entries are exposed
  through narrow contracts or Metro contributions rather than constructed in
  `:app`.
- **ViewModel scoping:** Root `NavDisplay` uses
  `rememberSaveableStateHolderNavEntryDecorator()` and
  `rememberViewModelStoreNavEntryDecorator()`. A feature route resolves its
  entry-scoped ViewModel with `metroViewModel()`.
- **Reason:** The application is Compose-only and greenfield, requires independent
  top-level stacks, type-safe arguments, process-restorable state, adaptive layouts,
  and clean multi-module ownership.

### D-017 — Reference implementations are inputs, not application architecture

- **State:** Accepted
- **Decision:** Use `speech-android`/`speech-core` as voice-pipeline and Android
  lifecycle references, sherpa-onnx as the primary STT/TTS/VAD runtime reference,
  and VoxSherpa-TTS as a product reference only.
- **Boundary:** Direct sherpa-onnx adapters remain the MVP direction.
  `speech-core` becomes a production dependency only after a measured spike proves a
  distinct benefit. VoxSherpa source is not copied without an explicitly compatible
  licensing decision.
- **Consequence:** Exact inspected snapshots, adopted techniques, and rejected
  patterns are maintained in [PLAN_REFERENCES.md](./PLAN_REFERENCES.md).

### D-018 — Typed capability-specific model profiles

- **State:** Accepted
- **Decision:** A model set is described by typed runtime profiles such as STT-only,
  TTS-only, LLM-only, or complete voice-pipeline. Profiles declare semantic file
  roles, frontend/runtime requirements, audio formats, capabilities, and quirks.
- **Consequence:** Profile readiness is independent. Shared file reuse is allowed
  only with persisted transactional ownership, and remote catalog changes never
  delete installed local data.

### D-019 — Synthesis and playback latency are separate measurements

- **State:** Accepted
- **Decision:** Streamed TTS records first synthesized chunk, first successful
  `AudioTrack` write, and first Android audio presentation as distinct timestamps.
- **Consequence:** If the active audio route cannot provide a reliable presentation
  timestamp, that metric is unavailable. It is not replaced with callback time.

### D-020 — Initial languages are English and Russian

- **State:** Accepted
- **Decision:** The first release supports English and Russian model samples and
  validates at least one complete STT → LLM → TTS path in each language.
- **Starter direction:** Use multilingual Whisper plus Silero VAD for the bilingual
  STT profile, Supertonic 3 for the shared English/Russian TTS profile, and Qwen3
  1.7B Q4_K_M for chat.
- **Optional profiles:** Keep English Zipformer as a genuine-streaming STT
  comparison and approved English/Russian Piper voices as lower-footprint TTS
  alternatives.
- **Consequence:** A model advertised as multilingual is not accepted until both
  English and Russian behavior is exercised on a reference device.

### D-021 — Runtime code is bundled with the application

- **State:** Accepted
- **Decision:** Build llama.cpp from a pinned upstream revision. Build sherpa-onnx
  from a pinned revision or consume a checksum-pinned reproducible AAR. Package all
  runtime native libraries in the APK/AAB.
- **Boundary:** The installed app never downloads executable runtime code, native
  libraries, or engine plugins. Runtime upgrades are application updates.
- **Consequence:** Build inputs include upstream revision, compile flags, NDK/CMake
  versions, ABIs, and artifact checksums.

### D-022 — Public anonymous model sources

- **State:** Accepted
- **Decision:** Curated model files use public HTTPS sources that work without
  credentials. Prefer the model/runtime owner’s immutable Hugging Face resolver URL
  or GitHub release asset.
- **Hugging Face:** Pin a commit SHA in `/resolve/<revision>/<path>`; never embed a
  maintainer or application-wide Hugging Face token.
- **Gated/private models:** Exclude them from one-tap curated downloads. They remain
  manual-import-only unless a separate user-authentication and license-acceptance
  decision is made.
- **Integrity:** Every source and approved mirror must produce the exact byte size
  and SHA-256 recorded by the immutable catalog version.

### D-023 — Bundled initial catalog

- **State:** Accepted
- **Decision:** Ship the initial approved model catalog as versioned data in the
  application. Update it through application releases during the MVP.
- **Later option:** A remotely updated catalog requires a separate decision covering
  signature verification, rollback, key rotation, availability, and reconciliation.
- **Consequence:** Remote model hosts supply model bytes; they do not control which
  entries the installed MVP presents as approved.

### D-024 — CPU baseline with declared experimental acceleration

- **State:** Accepted
- **Decision:** CPU is the required baseline for every initial native profile.
  Vulkan and NNAPI remain experimental backends and may be offered only with an
  explicit requested/effective-backend record and a visible fallback reason.
- **Consequence:** No device is rejected merely because an experimental accelerator
  is absent or fails. The initial Stage 0 implementation requests and uses CPU.

### D-025 — Stage 0 language-quality scope

- **State:** Accepted
- **Decision:** Stage 0 proves that the selected STT and TTS engines load and
  infer on the reference device. English and Russian quality assessment is deferred
  to the project owner.
- **Consequence:** Compatibility results must not be described as bilingual quality
  acceptance. Later language validation records its own samples and outcomes.

### D-026 — Curated downloads in the MVP

- **State:** Accepted
- **Decision:** The MVP supports curated model downloads; it is not import-only.
- **Consequence:** Stage 2 implements the bundled catalog, integrity verification,
  transactional multi-file installation, and the API-specific download scheduler
  already described by D-023 and D-104.

### D-027 — Stage 5 generated-audio retention

- **State:** Accepted
- **Decision:** Retain only the latest successful TTS WAV in app-private storage so
  it can be replayed or shared. Replace it after the next successful synthesis.
- **Failure behavior:** Failed and cancelled synthesis never replaces the latest
  successful WAV, and partial files are removed.
- **Export boundary:** A document explicitly exported by the user is independent
  from app-private retention and is not removed when the latest synthesis changes.
- **Later scope:** Stage 7 may add durable generated-audio history and configurable
  retention without changing this Stage 5 bounded default.

## 3. Provisional decisions to validate in Stage 0

### D-101 — Keep API 26 as application minimum

- **State:** Provisional
- **Current direction:** Keep `minSdk 26`; gate individual engines at runtime.
- **Validate:** Native library compatibility, audio APIs, and maintenance cost.
- **Decision deadline:** End of Stage 0.

### D-102 — Single Gradle module initially

- **State:** Superseded by D-014 and D-015
- **Previous direction:** Use package boundaries inside `:app` and extract modules
  later.
- **Reason superseded:** Metro binding ownership is now defined at Gradle-module
  level from the application-foundation stage.

### D-103 — Room and DataStore

- **State:** Provisional
- **Current direction:** Room stores structured model/conversation/run data;
  DataStore stores preferences.
- **Validate:** Actual metric/event shapes from the three native spikes.
- **Decision deadline:** Room schema is finalized in Stage 7.

### D-104 — Model download scheduling

- **State:** Provisional
- **Current direction:** Use an Android user-initiated data-transfer job on API 34+
  for long user-started model downloads. Use a foreground WorkManager worker on API
  26–33. Both schedule the same source-owned streaming downloader and installation
  transaction.
- **Background work:** Continue using regular WorkManager for deferrable cleanup and
  short maintenance work.
- **Validate:** Stop/retry behavior, notification and permission requirements,
  process recreation, partial-file resumption, host range support, and Android
  version behavior.
- **Decision deadline:** Before implementing MOD-003.

### D-105 — Material 3 Adaptive Navigation 3 integration

- **State:** Accepted
- **Decision:** Keep the app’s adaptive navigation layout behind a stable boundary
  and use Navigation 3 core with a locally controlled bottom-bar/navigation-rail
  layout.
- **Reason:** This keeps feature keys and entries independent from the adaptive
  presentation layer and avoids coupling the Stage 1 foundation to the alpha
  Material 3 Adaptive Navigation 3 add-on.
- **Revisit:** The add-on may replace the local layout implementation after a stable
  version is validated without changing feature navigation contracts.

### D-106 — Provisional runtime and model catalog

- **State:** Provisional
- **Current direction:** Maintain the runtime/model shortlist and catalog governance
  in [PLAN_MODELS.md](./PLAN_MODELS.md). Start Stage 0 with Qwen3 1.7B Q4_K_M for
  chat, Whisper Base multilingual INT8 plus Silero VAD for English/Russian STT, and
  Supertonic 3 INT8 for English/Russian TTS.
- **Optional profiles:** Evaluate Qwen3.5 0.8B as the tiny LLM, Qwen3 4B as the
  higher-quality LLM, Gemma 3 1B as the small multilingual comparison, Whisper Tiny
  as the smaller bilingual STT profile, English Zipformer as a native-streaming
  comparison, and English/Russian Piper voices as lower-footprint TTS profiles.
- **Post-MVP direction:** LiteRT-LM is the first accelerated LLM comparison engine.
  Pocket TTS, Phi-4 Mini, Parakeet, SenseVoice, Moonshine, and additional runtimes
  remain conditional candidates.
- **Boundary:** A candidate is not an approved curated download until exact files,
  immutable revision, sizes, checksums, license/attribution, typed runtime profile,
  and physical-device results are recorded.
- **Validate:** Required languages, reference-device memory/latency, runtime
  compatibility, model quality, and distribution rights.
- **Decision deadline:** End of Stage 0 for starter profiles; before MOD-003 for
  download approval.

### D-107 — Stage 0 CPU feasibility result

- **State:** Accepted
- **Decision:** Retain llama.cpp for GGUF and the checksum-pinned sherpa-onnx AAR
  for STT, TTS, and VAD. The initial known-good compatibility profile is CPU-only:
  Qwen3 1.7B Q4_K_M, Whisper Base INT8, Silero VAD, and Supertonic 3 INT8.
- **Reference device:** Samsung SM-S926B (Galaxy S24+), Android API 36,
  `arm64-v8a`, 10.94 GiB RAM; 3.11–3.49 GiB was available during the probes.
- **Observed results:** Qwen loaded in 867 ms and generated a short response in
  1,432 ms. A later explicit unload/reload in the same app process loaded in
  971 ms. Whisper file STT completed in 545 ms. Supertonic generated 123,390 PCM
  samples at 44.1 kHz in 845 ms. Silero VAD detected speech in 25 ms.
- **Native packaging:** The app packages arm64 libraries with 16 KiB-compatible
  linker alignment. The llama.cpp CPU backend is statically linked; it does not
  depend on runtime-downloaded backend plugins.
- **Boundary:** These are feasibility measurements, not cross-device promises or
  EN/RU quality validation. Production catalog entries remain subject to their
  final manifest/license/attribution review in Stage 2.

## 4. Questions

### Q-001 — Reference devices

- **State:** Partially resolved
- **Question:** Which physical devices define low-memory, mid-range, and flagship
  verification?
- **Needed information:** Device model, API level, RAM, SoC/ABI, available storage.
- **Resolution so far:** Samsung SM-S926B (Galaxy S24+), API 36, `arm64-v8a`,
  10.94 GiB RAM is the first physical reference device. A low-memory and a
  mid-range device are still needed for release support claims.
- **Blocks:** Device-matrix performance budgets, not the initial feasibility spike.

### Q-002 — Starter LLM model

- **State:** Open
- **Question:** Which exact GGUF model/revision/quantization is the known-good chat
  reference?
- **Current candidate:** Qwen3 1.7B Q4_K_M. Qwen3.5 0.8B and Qwen3 4B are optional
  tier candidates, not substitutes without measurement.
- **Selection criteria:** License/distribution rights, model size, RAM, chat
  template, language needs, output quality, reference-device speed.
- **Blocks:** Stage 0 exit and curated LLM download.
- **Owner:** Project owner with Stage 0 measurements.

### Q-003 — Starter STT model

- **State:** Open
- **Question:** Which exact sherpa-onnx model bundle is the known-good STT reference?
- **Current candidate:** Whisper Base multilingual INT8 plus Silero VAD for the
  required English/Russian profile. Whisper Tiny is the smaller comparison, English
  Zipformer is the native-streaming English comparison, and Russian-specific
  streaming/quality models remain Stage 0 research candidates.
- **Selection criteria:** Languages, streaming/endpoint support, model size,
  accuracy, reference-device RTF, runtime profile and required files, provider
  support, license.
- **Blocks:** Stage 0 exit and Stage 4.
- **Owner:** Project owner with Stage 0 measurements.

### Q-004 — Starter TTS voice

- **State:** Open
- **Question:** Which exact sherpa-onnx-compatible voice bundle is the known-good TTS
  reference?
- **Current candidate:** Supertonic 3 INT8 as the shared English/Russian voice
  profile. Piper/VITS `en_US-lessac-medium` and `ru_RU-dmitri-medium` are
  lower-footprint single-language comparisons.
- **Selection criteria:** Languages, voice quality, speaker count, model size,
  streaming/cancellation support, time to first presentation, reference-device RTF,
  runtime profile and required files, license.
- **Blocks:** Stage 0 exit and Stage 5.
- **Owner:** Project owner with Stage 0 measurements.

### Q-005 — Required languages

- **State:** Resolved by D-020
- **Question:** Which languages must the first release support for STT, TTS, and
  model samples?
- **Resolution:** English and Russian.
- **Why it matters:** It directly determines model selection, app sample text, and
  acceptance scenarios.
- **Blocks:** Final model catalog and release verification.
- **Owner:** Project owner.

### Q-006 — Curated downloads versus import-only first release

- **State:** Resolved by D-026
- **Question:** Must the MVP download models inside the app, or is document-based
  import sufficient for its intended users?
- **Tradeoff:** Downloads improve onboarding but introduce hosting, integrity,
  resumability, licenses, network permissions, and ongoing catalog maintenance.
- **Resolution:** The MVP supports curated downloads. They are implemented in
  Stage 2 only for individually approved public model versions governed by D-022
  and D-023.

### Q-007 — Minimum supported device memory

- **State:** Open
- **Question:** Is there a minimum RAM threshold for the app, or only per-model
  recommendations?
- **Current recommendation:** Report compatibility per model rather than globally
  rejecting lower-memory devices.
- **Blocks:** Release support statement.
- **Owner:** Project owner after Stage 0 measurements.

### Q-008 — Recording and generated-audio retention

- **State:** Partially resolved by D-027
- **Question:** Are audio artifacts deleted after a run, retained until explicit
  deletion, or removed after a configurable period?
- **Resolution so far:** Stage 5 retains exactly one latest successful generated
  TTS WAV under D-027. Explicit exports are independent.
- **Still open:** Final microphone-audio retention and any Stage 7 configurable or
  multi-item generated-audio history policy.
- **Blocks:** Stage 7 storage/privacy completion.
- **Owner:** Project owner.

### Q-009 — Conversation/run retention

- **State:** Open
- **Question:** Is history unlimited until manual deletion, capped by count, or
  governed by time/storage limits?
- **Current recommendation:** Retain lightweight text/metrics until manual deletion;
  manage audio with a separate policy.
- **Blocks:** Stage 7 settings.
- **Owner:** Project owner.

### Q-010 — App distribution

- **State:** Open
- **Question:** Is this a personal/debug tool, an open-source project, or a Play
  Store application?
- **Why it matters:** This changes model delivery, license review, native binary
  packaging, privacy disclosure, and release hardening.
- **Blocks:** Curated downloads and Stage 8 release tasks.
- **Owner:** Project owner.

### Q-011 — Benchmark intent

- **State:** Open
- **Question:** Are run metrics for informal debugging, or must results be comparable
  and exportable across devices/users?
- **Current recommendation:** MVP provides reproducible diagnostic metrics without
  claiming a standardized benchmark.
- **Blocks:** Only advanced comparison/report design.
- **Owner:** Project owner.

## 5. Decision checklist by milestone

### Before Stage 0 exit

- Q-001 reference device.
- Q-002 starter LLM.
- Q-003 starter STT.
- Q-004 starter TTS.
- Confirm the English/Russian D-020 starter combination.
- Exact pinned native runtime revisions.
- Exact inspected reference snapshots.
- Typed file/profile definitions for the known-good STT and TTS bundles.
- Requested/effective provider diagnostics and initial fallback policy.
- Confirm D-101 minimum API.

### Before MOD-003 curated downloads

- Q-006 download requirement.
- Approved model source URLs, licenses, file sizes, and checksums.
- Approved capability profiles, semantic file roles, and shared-file ownership.
- Confirm D-104 download implementation.

### Before Stage 7 exit

- Q-008 audio retention.
- Q-009 history retention.
- Export privacy defaults.

### Before Stage 8 exit

- Q-001 complete release device matrix.
- Q-007 support statement.
- Q-010 distribution path.
- License and attribution inventory.
- Review of any source adapted from the implementations in `PLAN_REFERENCES.md`.
- Numeric reference performance observations.
