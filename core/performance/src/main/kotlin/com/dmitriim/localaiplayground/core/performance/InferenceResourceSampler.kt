package com.dmitriim.localaiplayground.core.performance

internal interface InferenceResourceSampler {
    fun snapshot(): InferenceResourceSnapshot
    fun deviceSnapshot(): InferenceDeviceSnapshot
}
