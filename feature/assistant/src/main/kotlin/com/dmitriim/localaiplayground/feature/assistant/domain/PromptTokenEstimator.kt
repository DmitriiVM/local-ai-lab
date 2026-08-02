package com.dmitriim.localaiplayground.feature.assistant.domain

/** Estimates prompt tokens when a runtime requires caller budgeting without exposing tokenization. */
internal fun interface PromptTokenEstimator {
    fun estimate(prompt: String): Int
}

/** Treats every UTF-8 byte as a token and reserves additional template/special-token overhead. */
internal object ConservativeUtf8PromptTokenEstimator : PromptTokenEstimator {
    override fun estimate(prompt: String): Int = (prompt.toByteArray(Charsets.UTF_8).size.toLong() + SPECIAL_TOKEN_ALLOWANCE)
        .coerceAtMost(Int.MAX_VALUE.toLong())
        .toInt()

    private const val SPECIAL_TOKEN_ALLOWANCE = 64
}
