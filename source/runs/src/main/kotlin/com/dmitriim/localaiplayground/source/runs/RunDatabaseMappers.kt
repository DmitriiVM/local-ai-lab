package com.dmitriim.localaiplayground.source.runs

import com.dmitriim.localaiplayground.core.model.capability.AiCapability
import com.dmitriim.localaiplayground.core.model.conversation.ConversationKind
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRecord
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRole
import com.dmitriim.localaiplayground.core.model.conversation.ConversationRecord
import com.dmitriim.localaiplayground.core.model.runs.RunModelSnapshot
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.runs.RunKind
import com.dmitriim.localaiplayground.core.model.runs.RunStatus
import com.dmitriim.localaiplayground.source.database.ConversationEntity
import com.dmitriim.localaiplayground.source.database.ConversationMessageEntity
import com.dmitriim.localaiplayground.source.database.RunEntity
import kotlinx.serialization.json.Json

internal fun RunEntity.toDomain(json: Json) = RunRecord(
    id = id,
    kind = RunKind.valueOf(kind),
    benchmarkSessionId = benchmarkSessionId,
    capability = AiCapability.valueOf(capability),
    status = RunStatus.valueOf(status),
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    model = modelId?.let { RunModelSnapshot(it, modelDisplayName.orEmpty(), engineId.orEmpty(), modelRevision) },
    input = input,
    output = output,
    parametersJson = parametersJson,
    metricsJson = metricsJson,
    errorMessage = errorMessage,
    linkedRunIds = json.decodeFromString(linkedRunIdsJson),
)

internal fun RunRecord.toEntity(json: Json) = RunEntity(
    id = id,
    kind = kind.name,
    benchmarkSessionId = benchmarkSessionId,
    capability = capability.name,
    status = status.name,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    modelId = model?.modelId,
    modelDisplayName = model?.displayName,
    engineId = model?.engineId,
    modelRevision = model?.revision,
    input = input,
    output = output,
    parametersJson = parametersJson,
    metricsJson = metricsJson,
    errorMessage = errorMessage,
    linkedRunIdsJson = json.encodeToString(linkedRunIds),
)

internal fun ConversationEntity.toDomain() = ConversationRecord(id, ConversationKind.valueOf(kind), title, createdAtEpochMs, updatedAtEpochMs)

internal fun ConversationRecord.toEntity() = ConversationEntity(id, kind.name, title, createdAtEpochMs, updatedAtEpochMs)

internal fun ConversationMessageEntity.toDomain() = ConversationMessageRecord(
    id,
    conversationId,
    ConversationMessageRole.valueOf(role),
    content,
    createdAtEpochMs,
    incomplete,
)

internal fun ConversationMessageRecord.toEntity() = ConversationMessageEntity(
    id,
    conversationId,
    role.name,
    content,
    createdAtEpochMs,
    incomplete,
)
