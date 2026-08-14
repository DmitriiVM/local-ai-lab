# Local AI Lab — Runtime and Model Catalog Plan

> [!WARNING]
> **Historical planning document — not current project documentation.**
>
> This runtime and model catalog plan captures early AI-assisted development work.
> Model availability, runtime support, and technical assumptions may have changed.
> Refer to the repository [README](../../README.md) and current source code for
> authoritative information.

## 1. Purpose

This document is the authoritative plan for inference runtimes, starter model
candidates, compatibility profiles, and catalog governance.

It separates relatively stable application architecture from model information that
will change as upstream runtimes and model releases evolve. A model listed here is
not automatically approved for in-app download.

The first release should keep the catalog deliberately small:

- One primary runtime for chat and one primary runtime for speech.
- One known-good default model per capability.
- At most one lightweight and one quality alternative where they provide a measured
  difference.
- Additional models remain import-only or research candidates until their value,
  compatibility, and distribution rights are demonstrated.

## 2. Catalog states

Every runtime and model entry has one of these states:

- **Accepted runtime** — part of the MVP architecture.
- **Provisional starter** — the first Stage 0 candidate; it becomes known-good only
  after physical-device validation and license review.
- **Optional profile** — supported after the starter profile is stable.
- **Comparison engine/model** — used to compare another execution path and never
  selected through silent fallback.
- **Research only** — not exposed by the curated downloader; manual import may be
  supported if the runtime profile can validate it.
- **Rejected/retired** — retained with a reason when removal affects existing
  manifests or recorded runs.

Moving an entry to an approved curated download requires an exact upstream
repository and revision, filenames, byte sizes, checksums, license and attribution,
runtime revision, typed file roles, and a completed compatibility record.

## 3. Runtime engine strategy

| Runtime | Capabilities | Stage | State | Role |
| --- | --- | --- | --- | --- |
| `llama.cpp` | LLM | MVP | Accepted runtime | Primary GGUF chat runtime |
| `sherpa-onnx` | STT, TTS, VAD | MVP | Accepted runtime | Primary speech runtime |
| LiteRT-LM | LLM | Post-MVP | Comparison engine | Android CPU/GPU/NPU comparison using `.litertlm` models |
| Android `SpeechRecognizer` and `TextToSpeech` | STT, TTS | Post-MVP | Comparison engine | Platform baseline, not an offline guarantee |
| `speech-core` | Voice orchestration | Optional spike | Research only | Adopt only if it provides a measured benefit over direct adapters |
| ExecuTorch | LLM and other PyTorch exports | Deferred | Research only | Add only for a required `.pte` model or measured backend advantage |

MLC and additional inference frameworks are outside the initial plan. Each added
runtime multiplies native packaging, lifecycle, compatibility, metrics, and support
work, so a new engine requires a decision-log entry and a concrete capability that
the accepted engines cannot provide.

Runtime sources:

- [llama.cpp Android documentation](https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md)
- [sherpa-onnx Android documentation](https://k2-fsa.github.io/sherpa/onnx/android/build-sherpa-onnx.html)
- [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM)
- [ExecuTorch Android LLM documentation](https://docs.pytorch.org/executorch/stable/llm/run-on-android.html)

## 4. Initial catalog

The model names and approximate sizes below are planning inputs, not download
manifest data. The install UI uses only exact values from an approved, versioned
manifest.

### 4.0 Stage 0 pinned compatibility inputs

The following inputs completed a CPU-only inference on the first reference device.
They are the starting data for the bundled curated catalog, not yet its production
installer manifest. The Stage 2 downloader must verify every listed digest before
installation and preserve the stated file roles.

| Capability | Immutable public source | Bytes / SHA-256 | File roles / observed result |
| --- | --- | --- | --- |
| LLM | `https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF/resolve/daeb8e2d528a760970442092f6bf1e55c3b659eb/Qwen3-1.7B-Q4_K_M.gguf` | 1,282,439,264 / `d2387ca2dbfee2ffabce7120d3770dadca0b293052bc2f0e138fdc940d9bc7b5` | One GGUF; Apache-2.0 in model metadata. CPU load and short generation succeeded. |
| STT | `https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-whisper-base.tar.bz2` | 207,557,382 / `911b2083efd7c0dca2ac3b358b75222660dc09fb716d64fbfc417ba6c99ff3de` | `base-encoder.int8.onnx`, `base-decoder.int8.onnx`, `base-tokens.txt`; bundled 16 kHz PCM WAV completed file STT. |
| VAD | `https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx` | 643,854 / `9e2449e1087496d8d4caba907f23e0bd3f78d91fa552479bb9c23ac09cbb1fd6` | One `silero_vad.onnx`; speech detection succeeded on the bundled Whisper WAV. |
| TTS | `https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-supertonic-3-tts-int8-2026-05-11.tar.bz2` | 128,774,318 / `82fa96f91c4ef8abaae3a14a3f4153facf88bed821d1f7331cec2700f432c427` | Predictor, encoder, estimator, vocoder, `tts.json`, `unicode_indexer.bin`, and `voice.bin`; PCM synthesis succeeded. Bundle includes an MIT license. |

The Whisper and Silero distribution-license/attribution text must be copied into
the production catalog record before those entries become user-visible downloads.
The Stage 0 archive supplied no local license file for either asset, so its digest
and source alone are not treated as a completed distribution review.

### 4.1 VAD

| Profile | Candidate | State | Purpose |
| --- | --- | --- | --- |
| Default | Silero VAD ONNX supported by sherpa-onnx | Provisional starter | Endpointing and speech-segment detection |

Only one VAD model is needed initially. VAD-free manual recording remains available
so a VAD failure does not block the STT playground.

### 4.2 Speech to text

| Profile | Candidate | Mode | Languages | State |
| --- | --- | --- | --- | --- |
| English live comparison | `sherpa-onnx-streaming-zipformer-en-20M-2023-02-17`, INT8 files | Native streaming | English | Optional profile |
| Lightweight bilingual/file | Whisper Tiny multilingual INT8 for sherpa-onnx | Non-streaming; file or VAD-segmented audio | Includes English and Russian | Optional profile |
| English/Russian default | Whisper Base multilingual INT8 for sherpa-onnx | Non-streaming; file or VAD-segmented audio | Includes English and Russian | Provisional starter |
| Bilingual live | `sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20`, INT8 files | Native streaming | Chinese and English, including mixed input | Optional profile |
| Russian live | `sherpa-onnx-streaming-t-one-russian-2025-09-08` | Native streaming | Russian | Research candidate |
| Russian quality | GigaAM v2 Russian sherpa-onnx bundle | Offline/file | Russian | Research candidate pending exact profile/license selection |
| English quality | Parakeet TDT-CTC 110M INT8 conversion | Offline/file | English | Research only pending provenance review |
| Five-language | `sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09` | VAD-segmented/simulated streaming | Chinese, English, Japanese, Korean, Cantonese | Research only pending provenance review |
| Language-specific | Moonshine v2 quantized variants | Model-dependent | Includes English, Vietnamese, Spanish, Arabic, Chinese, Japanese, Korean, and Ukrainian variants | Post-MVP candidate |

The Zipformer 20M INT8 runtime files are approximately 44 MB before final packaging
is measured. It is the preferred English streaming comparison because it provides
genuine streaming with a small footprint.

Whisper Tiny and Base must be labelled non-streaming. The app may use them for file
transcription or feed finalized VAD segments to them, but it must not present simulated
streaming as a native streaming capability. Whisper Base is the first
English/Russian Stage 0 candidate; Whisper Tiny remains the smaller comparison.

The bilingual Zipformer is the preferred Chinese/English live alternative because
it provides genuine streaming and explicitly supports mixed Chinese/English input.
Its required INT8 graph files are approximately 205 MB before final packaging and
manifest validation.

The current SenseVoice candidate is approximately 226 MB for its INT8 model plus
tokens. It supports automatic language selection, but its microphone experience is
VAD-based simulated streaming and must be described that way.

The Parakeet and SenseVoice converted repositories are not curated until the plan
records the original model license, conversion provenance, redistributable files,
and required attribution.

Model sources:

- [Zipformer English 20M](https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-en-20M-2023-02-17)
- [Bilingual Chinese/English Zipformer](https://huggingface.co/csukuangfj/sherpa-onnx-streaming-zipformer-bilingual-zh-en-2023-02-20)
- [sherpa-onnx Whisper models](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/whisper/index.html)
- [sherpa-onnx SenseVoice models](https://k2-fsa.github.io/sherpa/onnx/sense-voice/pretrained.html)
- [sherpa-onnx Moonshine models](https://k2-fsa.github.io/sherpa/onnx/moonshine/models.html)
- [sherpa-onnx model index](https://k2-fsa.github.io/sherpa/onnx/pretrained_models/index.html)

### 4.3 Text to speech

The required initial languages are English and Russian under D-020 in
[PLAN_DECISIONS.md](PLAN_DECISIONS.md).

| Profile | Candidate | Languages | State |
| --- | --- | --- | --- |
| English lightweight | Piper/VITS `en_US-lessac-medium` | English | Optional profile |
| Russian lightweight | Piper/VITS `ru_RU-dmitri-medium` | Russian | Optional profile |
| English quality and voice variety | Kokoro INT8 multi-language v1.1 | English and Chinese | Optional profile |
| Bilingual compact | `vits-melo-tts-zh_en` | Chinese and limited English | Optional profile with documented English lexicon limitation |
| Streaming/voice reference | Pocket TTS INT8 | English | Post-MVP candidate |
| English/Russian shared default | `sherpa-onnx-supertonic-3-tts-int8-2026-05-11` | 31 languages including English and Russian | Provisional starter for higher-memory devices |
| Language packs | One approved Piper/VITS voice per required language | More than 40 languages are represented upstream; each voice is normally monolingual | Conditional candidate |

For the initial English/Russian release:

1. Validate Supertonic 3 as the shared bilingual default.
2. Evaluate one English and one Russian Piper/VITS voice as lower-footprint
   single-language alternatives.
3. Keep Kokoro and MeloTTS outside the initial language profile.

Supertonic 3 supports English, Korean, Japanese, Arabic, Bulgarian, Czech, Danish,
German, Greek, Spanish, Estonian, Finnish, French, Hindi, Croatian, Hungarian,
Indonesian, Italian, Lithuanian, Latvian, Dutch, Polish, Portuguese, Romanian,
Russian, Slovak, Slovenian, Swedish, Turkish, Ukrainian, and Vietnamese. Chinese
and Cantonese are not in this model's advertised language list.

`vits-melo-tts-zh_en` is a single-speaker 163 MB bilingual option, but its English
pronunciation is limited to words represented by its lexicon. It is not a general
replacement for Kokoro or an English Piper voice.

Do not launch with Piper, Kitten, Kokoro, Pocket TTS, and Supertonic simultaneously.
The initial catalog should expose one lightweight/default voice and at most one
quality alternative. Pocket TTS voice-reference input requires separate privacy,
storage, validation, and UX requirements before it becomes a product feature.

Every TTS voice receives an independent license review; engine compatibility does
not imply that all voice files share the engine license.

Model sources:

- [sherpa-onnx Piper models](https://k2-fsa.github.io/sherpa/onnx/tts/piper.html)
- [sherpa-onnx VITS and MeloTTS models](https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/vits.html)
- [sherpa-onnx Kokoro models](https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/kokoro.html)
- [Supertonic 3 model and language list](https://supertonic3.github.io/)
- [sherpa-onnx Supertonic 3 support](https://k2-fsa.github.io/sherpa/onnx/c-api/html/tts.html)
- [sherpa-onnx TTS model index](https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/index.html)

### 4.4 Local LLM

| Profile | Candidate | Advertised language coverage | Approximate model file | State |
| --- | --- | --- | --- | --- |
| Tiny | Qwen3.5 0.8B GGUF, Q4_0 | Multilingual | 563 MB | Optional experimental profile |
| Balanced default | Qwen3 1.7B GGUF, Q4_K_M | More than 100 languages | About 1.1 GB | Provisional starter |
| Quality | Qwen3 4B GGUF, Q4_K_M | More than 100 languages | About 2.5 GB | Optional profile for higher-memory devices |
| Small multilingual comparison | Gemma 3 1B Instruct GGUF, Q4_K_M | More than 140 languages | 806 MB | Optional comparison profile; Gemma terms apply |
| Multilingual reasoning comparison | Phi-4 Mini Instruct GGUF, Q4_K_M | 23 explicitly listed supported languages | About 2.49 GB | Research only until an exact GGUF conversion is approved |
| Android acceleration comparison | Gemma 3 1B, 4-bit `.litertlm` | More than 140 languages | 557 MB in the upstream catalog | Post-MVP comparison model |

Qwen3 1.7B Q4_K_M is the preferred initial chat spike because it provides a useful
balance of multilingual capability, size, and model quality. Use its non-thinking
mode by default for interactive mobile chat; thinking mode is explicit and records
the effective mode with the run.

Qwen3.5 0.8B is attractive for low-memory smoke tests but remains experimental until
the selected llama.cpp revision is validated on Android. It does not replace the
balanced default solely because its file is smaller.

Gemma 3 1B is the preferred small cross-family multilingual comparison. It is
available both as GGUF and through the LiteRT-LM catalog, which makes it useful for
separating model-family differences from runtime/backend differences. Its Gemma
license and use policy are reviewed separately from the Apache-licensed Qwen models.

Phi-4 Mini supports Arabic, Chinese, Czech, Danish, Dutch, English, Finnish, French,
German, Hebrew, Hungarian, Italian, Japanese, Korean, Norwegian, Polish, Portuguese,
Russian, Spanish, Swedish, Thai, Turkish, and Ukrainian. The base model uses the MIT
license, but no third-party GGUF is approved until its conversion revision, files,
and checksums are pinned.

The default mobile context limit is 4K tokens. An 8K option may be exposed after
measurement. Larger advertised model contexts are not enabled by default because
KV-cache memory and prefill latency are device-dependent.

Model sources:

- [Qwen3.5 0.8B GGUF](https://huggingface.co/ggml-org/Qwen3.5-0.8B-GGUF)
- [Qwen3 1.7B GGUF](https://huggingface.co/ggml-org/Qwen3-1.7B-GGUF)
- [Qwen3 4B GGUF](https://huggingface.co/ggml-org/Qwen3-4B-GGUF)
- [Gemma 3 1B Instruct GGUF](https://huggingface.co/ggml-org/gemma-3-1b-it-GGUF)
- [Gemma 3 1B model card](https://huggingface.co/google/gemma-3-1b-it)
- [Phi-4 Mini Instruct model card](https://huggingface.co/microsoft/Phi-4-mini-instruct)
- [LiteRT-LM supported models](https://github.com/google-ai-edge/LiteRT-LM#supported-models)

### 4.5 Recommended multilingual combinations

| Goal | STT | TTS | LLM |
| --- | --- | --- | --- |
| Initial English/Russian voice path | Whisper Base multilingual | Supertonic 3 | Qwen3 1.7B |
| Broad language exploration | Whisper Tiny or Base multilingual | Supertonic 3 where its language list applies; otherwise an approved Piper/VITS voice | Qwen3 1.7B |
| Live Chinese/English | Bilingual Zipformer INT8 | Kokoro INT8; MeloTTS only for its constrained bilingual use | Qwen3 1.7B or Gemma 3 1B comparison |
| Chinese/English/Japanese/Korean/Cantonese STT | SenseVoice INT8 after provenance review | Select TTS separately; no proposed single TTS model covers the same five-language set | Qwen3 1.7B |
| Higher-quality multilingual chat | STT selected independently | TTS selected independently | Qwen3 4B or Phi-4 Mini research comparison |

“Multilingual” is not a single compatibility flag. The catalog records advertised
languages, languages verified by this project, automatic-language-detection support,
and known limitations separately. The app never implies that an STT, TTS, and LLM
with different language sets form a complete multilingual voice pipeline.

## 5. User-facing model profiles

The catalog may group compatible models into convenience profiles, but the app still
shows the exact model and engine before installation or execution.

### Lightweight profile

- Silero VAD.
- Whisper Tiny multilingual INT8 for smaller English/Russian STT; English Zipformer
  remains a native-streaming comparison rather than the bilingual default.
- Separate small English and Russian Piper/VITS voices.
- Qwen3.5 0.8B Q4_0 only after the experimental profile passes Stage 0.

### Balanced profile

- Silero VAD.
- Whisper Base multilingual INT8 for English and Russian.
- Supertonic 3 for shared English/Russian TTS.
- Qwen3 1.7B Q4_K_M.

### Multilingual profile

- Whisper Base is the English/Russian default; Whisper Tiny is the smaller offline
  comparison.
- Use bilingual Zipformer when Chinese/English native streaming is more important
  than broad language coverage.
- Supertonic 3 is the shared TTS choice only for its 31 advertised languages;
  otherwise install an approved language-specific Piper/VITS voice.
- Qwen3 1.7B remains the balanced chat default. Gemma 3 1B is the small cross-family
  comparison.
- The profile is offered only when the selected STT, TTS, and LLM all declare the
  requested language.

### Quality profile

- The best measured STT model with verified license provenance for the required
  language.
- Kokoro for English/Chinese or Supertonic 3 for supported multilingual use.
- Qwen3 4B Q4_K_M on a compatible higher-memory device.

Profiles are independent installation selections, not promises that all components
can remain loaded simultaneously. The Voice Assistant screen performs a combined
memory preflight for its selected STT, LLM, and TTS models.

## 6. Required catalog and manifest metadata

An approved model/version records:

- Stable catalog ID, display name, family, capability, and catalog state.
- Engine ID and minimum/maximum tested runtime revision.
- Upstream project, exact immutable revision, source URL, and original model source.
- License identifier, license URL/text, attribution, and redistribution decision.
- Exact filenames, semantic file roles, byte sizes, and SHA-256 checksums.
- Format, architecture, precision/quantization, and required provider libraries.
- Language list and language-selection behavior.
- Streaming, partial-result, endpointing, cancellation, and contextual-biasing
  capabilities where relevant.
- Context and chat-template information for LLMs.
- Sample rate, channels, frontend assets, speaker/voice IDs, and output format for
  speech models.
- Expected installed size, temporary download space, and coarse memory class.
- Known-good ABI, Android version, device observations, and unsupported cases.
- Default settings, their permitted ranges, and any family-specific quirks.

Catalog entries are immutable by version. A changed file, checksum, license,
runtime requirement, or profile produces a new version instead of mutating an
installed definition.

## 7. Compatibility and load policy

- `arm64-v8a` is the initial native inference ABI.
- Compatibility is evaluated per model/profile rather than with one global device
  RAM cutoff.
- File size is not treated as peak RAM usage.
- The app checks ABI, engine revision, file completeness, available storage, coarse
  memory headroom, and model-specific requirements before load.
- The user sees requested and effective backend/provider information.
- The app never silently replaces a model or engine after a failed preflight or
  load.
- Only models needed by the active workflow remain loaded by default.
- The Voice Assistant estimates the combined footprint and validates all three
  selected capability profiles before listening.

Stage 0 records observations on low-memory, mid-range, and flagship reference
devices before numeric support statements are added.

## 8. Distribution policy

### Runtime delivery

- Runtime code and native libraries ship in the APK/AAB.
- llama.cpp is built from a pinned source revision.
- sherpa-onnx is built from a pinned revision or consumed as a checksum-pinned,
  reproducible AAR.
- The installed application never downloads executable engine/runtime code.

### Model sources and authentication

- Prefer public upstream Hugging Face resolver URLs pinned to a commit SHA and
  public upstream GitHub release assets.
- Public curated downloads must work without user credentials.
- Never embed a maintainer or shared Hugging Face token in the application.
- Gated/private models remain manual-import-only.
- Store the stable resolver or release URL, not a temporary redirected CDN URL.
- Mirrors are permitted only when they produce the same expected byte size and
  SHA-256.

### Catalog delivery

- Bundle the initial versioned catalog with the application.
- During MVP, catalog updates arrive with application updates.
- A remote catalog is post-MVP until signature verification, rollback, and key
  rotation are specified.

### Curated download

Enable only after:

- Legal distribution and attribution review is complete.
- Immutable URLs or a controlled versioned catalog are available.
- All required files, sizes, and checksums are recorded.
- Interrupted download, resume/restart, validation, atomic install, and deletion
  behavior are verified.
- The exact model/runtime pair completes inference on a reference physical device.

### Manual import

Manual import may support a broader compatible family, but the importer must:

- Ask for the engine/family profile when it cannot infer it safely.
- Validate every required semantic file role.
- Mark unknown metadata as unknown rather than inventing it.
- Explain that an imported model is not a tested or curated configuration.
- Preserve user-provided source/license notes when available.

Research-only entries do not appear as one-tap downloads.

## 9. Implementation rollout

### Stage 0

- Pin llama.cpp and sherpa-onnx revisions.
- Validate Qwen3 1.7B, Whisper Base multilingual, Silero VAD, and Supertonic 3 as
  the provisional English/Russian combination.
- Validate complete English and Russian STT → LLM → TTS turns independently; do not
  infer pipeline support from three independently multilingual labels.
- Record exact files, licenses, checksums, memory observations, latency, effective
  backends, and cancellation behavior.
- Decide whether Qwen3.5 0.8B, Gemma 3 1B, Whisper Tiny, English Zipformer,
  Russian-specific STT, and English/Russian Piper voices qualify as optional
  profiles.

### MVP

- Ship runtime profiles for the known-good starter models.
- Support validated manual import.
- Enable curated download only for individually approved entries.
- Keep LiteRT-LM, platform STT/TTS baselines, Pocket TTS, Phi-4 Mini, and additional
  research-only STT families outside the critical path.

### Post-MVP

- Compare LiteRT-LM with llama.cpp using a supported `.litertlm` model.
- Add quality or language-specific speech models according to measured demand.
- Add a runtime or model family only through a capability, compatibility, licensing,
  and maintenance decision.

## 10. Unresolved inputs

The provisional catalog does not close these decisions:

- Q-001 reference devices.
- Q-002 exact LLM revision, filenames, checksums, and Stage 0 measurements.
- Q-003 exact STT files, provider configuration, and Stage 0 measurements.
- Q-004 exact TTS voice files, license, frontend assets, and measurements.
- Q-006 curated download versus import-only MVP.
- Q-007 release support statement and per-model memory recommendations.
- Q-010 application distribution path.

The decision log remains authoritative for their status.
