package com.dmitriim.localailab.ai.api.chat

import com.dmitriim.localailab.ai.api.engine.EngineId

/**
 * Application-facing local-chat facade that routes work to the runtime selected by a model.
 *
 * Call [load] before any active-runtime operation inherited from [ChatExecution]. The selected
 * runtime may change on each load. [capabilitiesFor] is safe before loading; the formatter and
 * token counter accessors are only valid after a compatible runtime has been loaded.
 */
interface ChatEngine : ChatExecution {
    /** Returns the static capabilities of a packaged runtime, or null when it is not bundled. */
    fun capabilitiesFor(engineId: EngineId): LlmEngineCapabilities?

    /** Returns the active runtime's formatter when it declares engine-side chat formatting. */
    fun activeChatFormatter(): LlmChatFormatter?

    /** Returns the active runtime's exact token counter when it declares token counting. */
    fun activeTokenCounter(): LlmTokenCounter?
}
