package com.dmitriim.localailab.core.performance.resource

interface ResourceTelemetrySampler {
    fun snapshot(): PerformanceResourceSnapshot
    fun deviceProfile(): DevicePerformanceProfile
}
