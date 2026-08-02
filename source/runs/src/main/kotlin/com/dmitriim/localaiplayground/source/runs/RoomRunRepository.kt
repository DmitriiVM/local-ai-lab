package com.dmitriim.localaiplayground.source.runs

import android.app.Application
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.conversation.ConversationMessageRecord
import com.dmitriim.localaiplayground.core.model.conversation.ConversationRecord
import com.dmitriim.localaiplayground.core.model.device.StorageUsage
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import com.dmitriim.localaiplayground.core.model.service.RunRepository
import com.dmitriim.localaiplayground.source.database.ModelDatabaseProvider
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RoomRunRepository(
    private val application: Application,
    databaseProvider: ModelDatabaseProvider,
) : RunRepository {
    private val database = databaseProvider.database
    private val runDao = database.runDao()
    private val conversationDao = database.conversationDao()
    private val json = Json { ignoreUnknownKeys = true }

    override val runs: Flow<List<RunRecord>> = runDao.observeAll().map { entities -> entities.map { it.toDomain(json) } }
    override val conversations: Flow<List<ConversationRecord>> =
        conversationDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeRun(id: String): Flow<RunRecord?> = runDao.observe(id).map { it?.toDomain(json) }

    override fun observeMessages(conversationId: String): Flow<List<ConversationMessageRecord>> = conversationDao.observeMessages(conversationId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveRun(record: RunRecord) = runDao.upsert(record.toEntity(json))

    override suspend fun saveConversation(record: ConversationRecord, messages: List<ConversationMessageRecord>) = conversationDao.replaceConversation(record.toEntity(), messages.map { it.toEntity() })

    override suspend fun deleteConversation(id: String) = conversationDao.deleteConversation(id)

    override suspend fun clearRuns() = runDao.clear()

    override suspend fun storageUsage(): StorageUsage = StorageUsage(
        modelsBytes = directoryBytes(File(application.filesDir, "models")),
        recordingsBytes = directoryBytes(File(application.cacheDir, "stt-inputs")),
        generatedAudioBytes = directoryBytes(File(application.filesDir, "generated-audio")),
        historyBytes = application.getDatabasePath(DATABASE_NAME).length() +
            application.getDatabasePath("$DATABASE_NAME-wal").length() +
            application.getDatabasePath("$DATABASE_NAME-shm").length(),
    )

    private fun directoryBytes(directory: File): Long = directory.takeIf(File::exists)?.walkTopDown()?.filter(File::isFile)?.sumOf(File::length) ?: 0

    private companion object {
        const val DATABASE_NAME = "local-ai-playground.db"
    }
}
