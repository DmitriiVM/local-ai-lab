package com.dmitriim.localailab.source.runs

import android.app.Application
import androidx.core.content.FileProvider
import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.model.runs.RunRecord
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Inject
@SingleIn(AppScope::class)
class RunExporter(private val application: Application) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun export(record: RunRecord, linkedRuns: List<RunRecord> = emptyList()): String {
        val directory = File(application.cacheDir, "run-exports").also(File::mkdirs)
        val target = File(directory, "run-${record.id}.json")
        target.writeText(json.encodeToString(VersionedRunExport(run = record, linkedRuns = linkedRuns)))
        return FileProvider.getUriForFile(application, "${application.packageName}.files", target).toString()
    }
}

@Serializable
private data class VersionedRunExport(
    val schemaVersion: Int = 2,
    val run: RunRecord,
    val linkedRuns: List<RunRecord> = emptyList(),
)
