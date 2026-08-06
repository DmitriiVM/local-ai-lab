package com.dmitriim.localaiplayground.core.performance

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put

fun JsonObjectBuilder.putInferenceTelemetry(telemetry: InferenceTelemetry?) {
    telemetry?.let { put("telemetry", Json.encodeToJsonElement(InferenceTelemetry.serializer(), it)) }
}
