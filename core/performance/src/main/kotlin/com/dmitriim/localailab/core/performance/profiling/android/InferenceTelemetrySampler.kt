package com.dmitriim.localailab.core.performance.profiling.android

import com.dmitriim.localailab.core.performance.profiling.InferenceDeviceSnapshot
import com.dmitriim.localailab.core.performance.profiling.InferenceResourceSnapshot

internal interface InferenceTelemetrySampler {
    fun snapshot(): InferenceResourceSnapshot
    fun deviceSnapshot(): InferenceDeviceSnapshot
}
