package com.dmitriim.localaiplayground.ai.llamacpp

import android.app.Application
import com.dmitriim.localaiplayground.ai.api.llm.ChatEngine
import com.dmitriim.localaiplayground.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class LlamaCppChatEngine(application: Application) : ChatEngine by NativeLlama(application)
