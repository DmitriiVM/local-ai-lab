# Local native build inputs

This directory is intentionally not a model repository.

For the Stage 0 llama.cpp native build, clone the exact source revision recorded
in `spec/STAGE0_RESULTS.md` into `third_party/llama.cpp` before running Gradle.
The public Stage 0 model downloads are local probe inputs only and remain ignored:
they are defined with immutable URLs and SHA-256 digests in `spec/PLAN_MODELS.md`.

Production curated downloads are implemented by the Stage 2 model installer, not
by committing model bytes to this repository.
