# Local native build inputs

This directory is intentionally not a model repository.

`llama.cpp` is a source-only vendored snapshot used by the Android CMake build. Its
upstream URL, exact revision, license, and update procedure are recorded in
[`LLAMA_CPP_VERSION.md`](./LLAMA_CPP_VERSION.md). Do not add generated native build
outputs or `.git` metadata to the snapshot.

Model weights are not vendored in this repository. The model library installs or
imports them explicitly into app-private storage; do not add model bytes or generated
native build outputs to this directory.
