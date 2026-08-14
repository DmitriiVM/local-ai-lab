package com.dmitriim.localailab.core.performance

internal interface InferenceResourceSampler {
    fun snapshot(): InferenceResourceSnapshot
    fun deviceSnapshot(): InferenceDeviceSnapshot
}
