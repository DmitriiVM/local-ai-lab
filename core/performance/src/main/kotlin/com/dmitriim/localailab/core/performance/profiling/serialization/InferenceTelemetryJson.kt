package com.dmitriim.localailab.core.performance.profiling.serialization

import com.dmitriim.localailab.core.performance.profiling.InferenceTelemetry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder

fun JsonObjectBuilder.putInferenceTelemetry(telemetry: InferenceTelemetry?) {
    telemetry?.let { put("telemetry", Json.encodeToJsonElement(InferenceTelemetry.serializer(), it)) }
}
