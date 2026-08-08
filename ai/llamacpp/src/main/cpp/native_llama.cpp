#include <android/log.h>
#include <jni.h>

#include <algorithm>
#include <atomic>
#include <chrono>
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
int effective_threads = 0;

void log_callback(enum ggml_log_level level, const char *text, void *) {
    int priority = ANDROID_LOG_INFO;
    if (level == GGML_LOG_LEVEL_ERROR) priority = ANDROID_LOG_ERROR;
    if (level == GGML_LOG_LEVEL_WARN) priority = ANDROID_LOG_WARN;
    if (level == GGML_LOG_LEVEL_DEBUG) priority = ANDROID_LOG_DEBUG;
    __android_log_write(priority, kLogTag, text);
}

void release_model_locked() {
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

jobjectArray array_result(JNIEnv *env, const std::vector<std::string> &values) {
    jclass string_class = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(static_cast<jsize>(values.size()), string_class, nullptr);
    for (size_t index = 0; index < values.size(); ++index) {
        env->SetObjectArrayElement(result, static_cast<jsize>(index), string_result(env, values[index]));
    }
    return result;
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

long elapsed_ms(const std::chrono::steady_clock::time_point &start) {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - start).count();
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeInitialize(
        JNIEnv *env, jobject, jstring native_lib_dir) {
    std::lock_guard lock(engine_mutex);
    if (backend_initialized) return string_result(env, "");
    const char *directory = env->GetStringUTFChars(native_lib_dir, nullptr);
    llama_log_set(log_callback, nullptr);
    __android_log_print(ANDROID_LOG_INFO, kLogTag, "Initializing static CPU backend from %s", directory);
    env->ReleaseStringUTFChars(native_lib_dir, directory);
    llama_backend_init();
    if (ggml_backend_reg_count() == 0) return string_result(env, "llama.cpp did not register a static CPU backend");
    backend_initialized = true;
    return string_result(env, "");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeLoad(
        JNIEnv *env, jobject, jstring model_path, jint context_size, jint thread_count) {
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
    return string_result(env, "");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeFormatChat(
        JNIEnv *env, jobject, jobjectArray roles, jobjectArray contents) {
    std::lock_guard lock(engine_mutex);
    if (model == nullptr) return string_result(env, "ERROR:No model is loaded");
    const jsize count = env->GetArrayLength(roles);
    if (count == 0 || count != env->GetArrayLength(contents)) return string_result(env, "ERROR:Invalid chat message list");
    std::vector<std::string> role_values;
    std::vector<std::string> content_values;
    role_values.reserve(count);
    content_values.reserve(count);
    size_t character_count = 0;
    for (jsize index = 0; index < count; ++index) {
        auto role = static_cast<jstring>(env->GetObjectArrayElement(roles, index));
        auto content = static_cast<jstring>(env->GetObjectArrayElement(contents, index));
        const char *role_chars = env->GetStringUTFChars(role, nullptr);
        const char *content_chars = env->GetStringUTFChars(content, nullptr);
        role_values.emplace_back(role_chars);
        content_values.emplace_back(content_chars);
        character_count += role_values.back().size() + content_values.back().size();
        env->ReleaseStringUTFChars(role, role_chars);
        env->ReleaseStringUTFChars(content, content_chars);
        env->DeleteLocalRef(role);
        env->DeleteLocalRef(content);
    }
    std::vector<llama_chat_message> messages;
    messages.reserve(count);
    for (jsize index = 0; index < count; ++index) {
        messages.push_back({role_values[index].c_str(), content_values[index].c_str()});
    }
    const char *template_name = llama_model_chat_template(model, nullptr);
    if (template_name == nullptr) {
        std::string fallback;
        for (const auto &message : messages) fallback += std::string(message.role) + ": " + message.content + "\n";
        fallback += "assistant: ";
        return string_result(env, fallback);
    }
    std::vector<char> buffer(std::max<size_t>(1024, character_count * 4 + 512));
    int32_t size = llama_chat_apply_template(template_name, messages.data(), messages.size(), true, buffer.data(), buffer.size());
    if (size < 0) return string_result(env, "ERROR:Could not apply the model chat template");
    if (size >= static_cast<int32_t>(buffer.size())) {
        buffer.resize(static_cast<size_t>(size) + 1);
        size = llama_chat_apply_template(template_name, messages.data(), messages.size(), true, buffer.data(), buffer.size());
    }
    if (size < 0) return string_result(env, "ERROR:Could not apply the model chat template");
    return string_result(env, std::string(buffer.data(), static_cast<size_t>(size)));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeTokenCount(
        JNIEnv *env, jobject, jstring prompt) {
    std::lock_guard lock(engine_mutex);
    if (model == nullptr) return -1;
    const char *chars = env->GetStringUTFChars(prompt, nullptr);
    const std::string text(chars);
    env->ReleaseStringUTFChars(prompt, chars);
    const llama_vocab *vocab = llama_model_get_vocab(model);
    const int count = -llama_tokenize(vocab, text.c_str(), text.size(), nullptr, 0, true, true);
    return count > 0 ? count : -1;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_dmitriim_localaiplayground_ai_llamacpp_NativeLlama_00024NativeBridge_nativeGenerate(
        JNIEnv *env, jobject, jstring prompt, jint max_tokens, jfloat temperature, jint top_k, jfloat top_p,
        jint seed, jobject callback) {
    std::lock_guard lock(engine_mutex);
    if (model == nullptr || context == nullptr) return array_result(env, {"ERROR", "No model is loaded"});
    cancelled = false;
    const auto started = std::chrono::steady_clock::now();
    const char *prompt_chars = env->GetStringUTFChars(prompt, nullptr);
    const std::string prompt_text(prompt_chars);
    env->ReleaseStringUTFChars(prompt, prompt_chars);
    const llama_vocab *vocab = llama_model_get_vocab(model);
    const int prompt_count = -llama_tokenize(vocab, prompt_text.c_str(), prompt_text.size(), nullptr, 0, true, true);
    if (prompt_count <= 0 || prompt_count >= static_cast<int>(llama_n_ctx(context))) {
        return array_result(env, {"ERROR", "Prompt does not fit in the active context"});
    }
    std::vector<llama_token> tokens(static_cast<size_t>(prompt_count));
    if (llama_tokenize(vocab, prompt_text.c_str(), prompt_text.size(), tokens.data(), tokens.size(), true, true) < 0) {
        return array_result(env, {"ERROR", "Could not tokenize prompt"});
    }
    llama_memory_clear(llama_get_memory(context), false);
    llama_batch batch = llama_batch_get_one(tokens.data(), tokens.size());
    if (llama_decode(context, batch) != 0) return array_result(env, {"ERROR", "Prompt evaluation failed"});
    const long prompt_duration = elapsed_ms(started);

    llama_sampler_chain_params sampler_params = llama_sampler_chain_default_params();
    llama_sampler *sampler = llama_sampler_chain_init(sampler_params);
    llama_sampler_chain_add(sampler, llama_sampler_init_top_k(std::max(1, static_cast<int>(top_k))));
    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(std::clamp(static_cast<float>(top_p), 0.05f, 1.0f), 1));
    llama_sampler_chain_add(sampler, llama_sampler_init_temp(std::clamp(static_cast<float>(temperature), 0.0f, 2.0f)));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(seed < 0 ? LLAMA_DEFAULT_SEED : static_cast<uint32_t>(seed)));

    jclass callback_class = env->GetObjectClass(callback);
    if (callback_class == nullptr) {
        return array_result(env, {"ERROR", "Could not resolve the streaming callback class"});
    }
    jmethodID on_token = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;)V");
    if (on_token == nullptr) {
        env->ExceptionClear();
        return array_result(env, {"ERROR", "The streaming callback is unavailable"});
    }
    std::string output;
    int generated_count = 0;
    long first_token_latency = -1;
    std::string finish_reason = "MAX_TOKENS";
    const int limit = std::max(1, static_cast<int>(max_tokens));
    for (int index = 0; index < limit && !cancelled; ++index) {
        llama_token token = llama_sampler_sample(sampler, context, -1);
        if (llama_vocab_is_eog(vocab, token)) {
            finish_reason = "STOP_TOKEN";
            break;
        }
        llama_sampler_accept(sampler, token);
        const std::string piece = token_piece(vocab, token);
        output += piece;
        ++generated_count;
        if (first_token_latency < 0) first_token_latency = elapsed_ms(started);
        if (!piece.empty() && on_token != nullptr) {
            jstring java_piece = string_result(env, piece);
            env->CallVoidMethod(callback, on_token, java_piece);
            env->DeleteLocalRef(java_piece);
            if (env->ExceptionCheck()) {
                llama_sampler_free(sampler);
                return array_result(env, {"ERROR", "The streaming callback failed"});
            }
        }
        batch = llama_batch_get_one(&token, 1);
        if (llama_decode(context, batch) != 0) {
            llama_sampler_free(sampler);
            return array_result(env, {"ERROR", "Token generation failed"});
        }
    }
    if (cancelled) finish_reason = "CANCELLED";
    const long total_duration = elapsed_ms(started);
    llama_sampler_free(sampler);
    return array_result(env, {
        "OK", output, std::to_string(prompt_count), std::to_string(generated_count),
        std::to_string(first_token_latency), std::to_string(prompt_duration),
        std::to_string(std::max(0L, total_duration - prompt_duration)), std::to_string(total_duration), finish_reason,
    });
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
