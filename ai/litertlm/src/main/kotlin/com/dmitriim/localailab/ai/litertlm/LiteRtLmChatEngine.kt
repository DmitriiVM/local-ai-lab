package com.dmitriim.localailab.ai.litertlm

import android.app.Application
import com.dmitriim.localailab.ai.api.llm.LlmChatFormatter
import com.dmitriim.localailab.ai.api.llm.LlmRuntime
import com.dmitriim.localailab.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

@SingleIn(AppScope::class)
@ContributesIntoSet(AppScope::class, binding = binding<LlmRuntime>())
class LiteRtLmChatEngine private constructor(delegate: LiteRtLmRuntime) :
    LlmRuntime by delegate,
    LlmChatFormatter by delegate {
    @Inject
    constructor(application: Application) : this(LiteRtLmRuntime(application))
}
