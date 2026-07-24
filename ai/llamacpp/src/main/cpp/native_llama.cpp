#include <android/log.h>
#include <jni.h>

#include <algorithm>
#include <atomic>
#include <mutex>
#include <string>
#include <unistd.h>
#include <vector>

#include "llama.h"
#include "ggml-backend.h"

namespace {

constexpr char kLogTag[] = "LocalAiLlama";

std::mutex engine_mutex;
std::atomic_bool cancelled = false;
bool backend_initialized = false;
llama_model *model = nullptr;
llama_context *context = nullptr;
llama_sampler *sampler = nullptr;
int effective_threads = 0;

void log_callback(enum ggml_log_level level, const char *text, void *) {
    int priority = ANDROID_LOG_INFO;
    if (level == GGML_LOG_LEVEL_ERROR) priority = ANDROID_LOG_ERROR;
    if (level == GGML_LOG_LEVEL_WARN) priority = ANDROID_LOG_WARN;
    if (level == GGML_LOG_LEVEL_DEBUG) priority = ANDROID_LOG_DEBUG;
    __android_log_write(priority, kLogTag, text);
}

void release_model_locked() {
    if (sampler != nullptr) {
        llama_sampler_free(sampler);
        sampler = nullptr;
    }
    if (context != nullptr) {
        llama_free(context);
        context = nullptr;
    }
    if (model != nullptr) {
        llama_model_free(model);
        model = nullptr;
    }
    effective_threads = 0;
}

jstring string_result(JNIEnv *env, const std::string &value) {
    return env->NewStringUTF(value.c_str());
}

std::string token_piece(const llama_vocab *vocab, llama_token token) {
    std::vector<char> buffer(256);
    int size = llama_token_to_piece(vocab, token, buffer.data(), buffer.size(), 0, true);
    if (size < 0) {
        buffer.resize(static_cast<size_t>(-size));
        size = llama_token_to_piece(vocab, token, buffer.data(), buffer.size(), 0, true);
    }
    return size > 0 ? std::string(buffer.data(), static_cast<size_t>(size)) : std::string();
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeInitialize(
        JNIEnv *env,
        jobject,
        jstring native_lib_dir) {
    std::lock_guard lock(engine_mutex);
    if (backend_initialized) return string_result(env, "");

    const char *directory = env->GetStringUTFChars(native_lib_dir, nullptr);
    llama_log_set(log_callback, nullptr);
    // Stage 0 deliberately links one generic CPU backend into the library. Android
    // may load APK libraries directly from the APK rather than a directory, so a
    // dlopen-based backend scan is not a reliable CPU baseline.
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "Initializing static CPU backend from %s", directory);
    env->ReleaseStringUTFChars(native_lib_dir, directory);
    llama_backend_init();
    if (ggml_backend_reg_count() == 0) {
        return string_result(env, "llama.cpp did not register a static CPU backend");
    }
    backend_initialized = true;
    return string_result(env, "");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeLoad(
        JNIEnv *env,
        jobject,
        jstring model_path,
        jint context_size,
        jint thread_count) {
    std::lock_guard lock(engine_mutex);
    if (!backend_initialized) return string_result(env, "Native backend is not initialized");
    release_model_locked();
    cancelled = false;

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0;
    model = llama_model_load_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);
    if (model == nullptr) return string_result(env, "llama.cpp could not load this GGUF model");

    const int available_cores = std::max(1, static_cast<int>(sysconf(_SC_NPROCESSORS_ONLN)));
    effective_threads = thread_count > 0 ? thread_count : std::max(1, available_cores - 2);
    llama_context_params context_params = llama_context_default_params();
    context_params.n_ctx = std::max(128, static_cast<int>(context_size));
    context_params.n_batch = std::min(context_params.n_ctx, static_cast<uint32_t>(512));
    context_params.n_ubatch = context_params.n_batch;
    context_params.n_threads = effective_threads;
    context_params.n_threads_batch = effective_threads;
    context_params.no_perf = false;
    context = llama_init_from_model(model, context_params);
    if (context == nullptr) {
        release_model_locked();
        return string_result(env, "llama.cpp could not allocate the requested context");
    }

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_greedy());
    return string_result(env, "");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeGenerate(
        JNIEnv *env,
        jobject,
        jstring prompt,
        jint max_tokens) {
    std::lock_guard lock(engine_mutex);
    if (model == nullptr || context == nullptr || sampler == nullptr) {
        return string_result(env, "ERROR:No model is loaded");
    }
    cancelled = false;
    const char *prompt_chars = env->GetStringUTFChars(prompt, nullptr);
    const std::string prompt_text(prompt_chars);
    env->ReleaseStringUTFChars(prompt, prompt_chars);
    const llama_vocab *vocab = llama_model_get_vocab(model);
    const int prompt_count = -llama_tokenize(vocab, prompt_text.c_str(), prompt_text.size(), nullptr, 0, true, true);
    if (prompt_count <= 0 || prompt_count >= static_cast<int>(llama_n_ctx(context))) {
        return string_result(env, "ERROR:Prompt does not fit in the active context");
    }
    std::vector<llama_token> tokens(static_cast<size_t>(prompt_count));
    if (llama_tokenize(vocab, prompt_text.c_str(), prompt_text.size(), tokens.data(), tokens.size(), true, true) < 0) {
        return string_result(env, "ERROR:Could not tokenize prompt");
    }
    llama_memory_clear(llama_get_memory(context), false);
    llama_sampler_reset(sampler);
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(context, batch) != 0) return string_result(env, "ERROR:Prompt evaluation failed");

    std::string output;
    const int limit = std::max(1, static_cast<int>(max_tokens));
    for (int index = 0; index < limit && !cancelled; ++index) {
        llama_token token = llama_sampler_sample(sampler, context, -1);
        if (llama_vocab_is_eog(vocab, token)) break;
        llama_sampler_accept(sampler, token);
        output += token_piece(vocab, token);
        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(context, batch) != 0) return string_result(env, "ERROR:Token generation failed");
    }
    return string_result(env, (cancelled ? "CANCELLED:" : "OK:") + output);
}

extern "C" JNIEXPORT void JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeCancel(JNIEnv *, jobject) {
    cancelled = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeUnload(JNIEnv *, jobject) {
    std::lock_guard lock(engine_mutex);
    cancelled = true;
    release_model_locked();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeSystemInfo(JNIEnv *env, jobject) {
    return string_result(env, llama_print_system_info());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeEffectiveThreads(JNIEnv *, jobject) {
    return effective_threads;
}
