# Local native build inputs

This directory is intentionally not a model repository.

`llama.cpp` is a source-only vendored snapshot used by the Android CMake build. Its
upstream URL, exact revision, license, and update procedure are recorded in
[`LLAMA_CPP_VERSION.md`](./LLAMA_CPP_VERSION.md). Do not add generated native build
outputs or `.git` metadata to the snapshot.

The public Stage 0 model downloads are local probe inputs only and remain ignored:
they are defined with immutable URLs and SHA-256 digests in `spec/PLAN_MODELS.md`.
Production curated downloads are implemented by the Stage 2 model installer, not by
committing model bytes to this repository.
