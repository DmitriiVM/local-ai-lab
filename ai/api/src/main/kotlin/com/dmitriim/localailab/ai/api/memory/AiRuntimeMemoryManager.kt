package com.dmitriim.localailab.ai.api.memory

enum class AiRuntimeKind {
    CHAT,
    SPEECH_TO_TEXT,
    TEXT_TO_SPEECH,
}

/** Keeps runtimes warm while a feature owns them and evicts them when ownership ends. */
interface AiRuntimeMemoryManager {
    fun acquire(runtimeKinds: Set<AiRuntimeKind>): AutoCloseable

    fun evictAll()
}
