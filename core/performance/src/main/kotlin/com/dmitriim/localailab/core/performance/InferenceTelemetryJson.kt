package com.dmitriim.localailab.core.performance

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectBuilder

fun JsonObjectBuilder.putInferenceTelemetry(telemetry: InferenceTelemetry?) {
    telemetry?.let { put("telemetry", Json.encodeToJsonElement(InferenceTelemetry.serializer(), it)) }
}
