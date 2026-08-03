# llama.cpp source snapshot

The Android JNI build vendors the following upstream source snapshot directly in
[`llama.cpp`](./llama.cpp). A normal repository clone therefore has all CMake inputs
required to build `:ai:llamacpp`.

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
   excluding its `.git` directory and generated build outputs.
4. Update this document with the new SHA, commit date, and any local patches.
5. Include the resulting source and documentation changes in one reviewable commit.

Do not replace this snapshot with a moving branch such as `main`.
