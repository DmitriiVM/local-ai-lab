cmake_minimum_required(VERSION 3.22.1)

project(local_ai_llamacpp LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# NDK r27 needs explicit ELF segment alignment for Android 15+ devices that use
# 16 KiB memory pages.  Apply it before importing llama.cpp so its shared
# libraries, not just this JNI facade, receive the flags.
if(ANDROID)
    add_link_options(
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384")
endif()

set(LLAMA_SRC "${CMAKE_CURRENT_LIST_DIR}/../../../../../third_party/llama.cpp")

if(NOT EXISTS "${LLAMA_SRC}/CMakeLists.txt")
    message(FATAL_ERROR "Pinned llama.cpp source is missing at ${LLAMA_SRC}")
endif()

add_subdirectory("${LLAMA_SRC}" llama-build)

add_library(local_ai_llamacpp SHARED native_llama.cpp)

target_include_directories(local_ai_llamacpp PRIVATE
    "${LLAMA_SRC}/include"
    "${LLAMA_SRC}/ggml/include"
    "${LLAMA_SRC}/ggml/src")

target_link_libraries(local_ai_llamacpp
    llama
    android
    log)
