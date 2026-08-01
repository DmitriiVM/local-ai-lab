package com.dmitriim.localaiplayground.source.runs

import android.app.Application
import androidx.core.content.FileProvider
import com.dmitriim.localaiplayground.core.di.AppScope
import com.dmitriim.localaiplayground.core.model.runs.RunRecord
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Inject
@SingleIn(AppScope::class)
class RunExporter(private val application: Application) {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    fun export(record: RunRecord): String {
        val directory = File(application.cacheDir, "run-exports").also(File::mkdirs)
        val target = File(directory, "run-${record.id}.json")
        target.writeText(json.encodeToString(VersionedRunExport(run = record)))
        return FileProvider.getUriForFile(application, "${application.packageName}.files", target).toString()
    }
}

@Serializable
private data class VersionedRunExport(
    val schemaVersion: Int = 1,
    val run: RunRecord,
)
