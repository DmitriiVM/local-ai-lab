package com.dmitriim.localailab.core.performance

/** Contract implemented by feature runtimes which execute one benchmark iteration. */
interface BenchmarkWorkloadRunner<in Configuration : BenchmarkWorkloadConfiguration> {
    suspend fun run(
        configuration: Configuration,
        runId: String,
        iteration: Int,
        startupMode: BenchmarkStartupMode,
    ): BenchmarkIterationResult
}
