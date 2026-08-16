# llama.cpp source snapshot

The Android JNI build vendors a minimal, source-only snapshot of the following
upstream revision directly in [`llama.cpp`](./llama.cpp). A normal repository clone
therefore has all CMake inputs required to build `:ai:llamacpp`, without upstream
documentation, examples, test fixtures, tools, or model vocabulary assets.

| Field | Value |
| --- | --- |
| Upstream | `https://github.com/ggml-org/llama.cpp.git` |
| Revision | `c0bc8591e8815c63cb01dd3f051a8b0df02501c9` |
| Upstream commit date | 2026-07-23 |
| Local patches | None |
| License | MIT; see [`llama.cpp/LICENSE`](./llama.cpp/LICENSE) |

## Updating the snapshot

1. Clone the official upstream repository outside this project and check out the
   intended immutable commit SHA.
2. Validate the Android build and the JNI adapter against that commit.
3. Replace the contents of `third_party/llama.cpp` with the checked-out source,
   excluding its `.git` directory, generated build outputs, and the upstream content
   excluded below.
4. Preserve the root CMake files, `cmake/`, `include/`, `src/`, `ggml/`, `vendor/`,
   `licenses/`, `LICENSE`, and `AUTHORS`. The Android build disables all optional
   llama.cpp components and links only the core `llama` target.
5. Update this document with the new SHA, commit date, and any local patches.
6. Build `:app:assembleDebug` to confirm the trimmed snapshot still supplies every
   Android CMake input.
7. Include the resulting source and documentation changes in one reviewable commit.

## Excluded upstream content

The vendored snapshot deliberately excludes `.github/`, `.devops/`, `app/`,
`benches/`, `ci/`, `common/`, `conversion/`, `docs/`, `examples/`, `gguf-py/`,
`grammars/`, `media/`, `models/`, `pocs/`, `scripts/`, `tests/`, and `tools/`.
None are included by the Android build configuration, which sets
`LLAMA_BUILD_COMMON`, `LLAMA_BUILD_TESTS`, `LLAMA_BUILD_TOOLS`,
`LLAMA_BUILD_EXAMPLES`, and `LLAMA_BUILD_APP` to `OFF`.

Do not replace this snapshot with a moving branch such as `main`.
