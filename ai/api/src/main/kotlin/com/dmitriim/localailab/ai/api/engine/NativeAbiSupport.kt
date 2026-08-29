package com.dmitriim.localailab.ai.api.engine

/** Native ABIs for which the app packages its local inference runtimes. */
object NativeAbiSupport {
    const val ARM64_V8A = "arm64-v8a"
    const val X86_64 = "x86_64"

    val supported = setOf(ARM64_V8A, X86_64)

    fun supports(deviceAbis: Collection<String>): Boolean = deviceAbis.any(supported::contains)
}
