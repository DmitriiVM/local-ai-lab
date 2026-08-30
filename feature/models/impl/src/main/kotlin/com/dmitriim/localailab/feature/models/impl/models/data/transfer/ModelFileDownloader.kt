package com.dmitriim.localailab.feature.models.impl.models.data.transfer

import android.util.Log
import com.dmitriim.localailab.ai.api.model.manifest.ModelId
import com.dmitriim.localailab.feature.models.impl.models.data.persistence.ModelTransferFileEntity
import com.dmitriim.localailab.feature.models.impl.models.data.validation.sha256
import com.dmitriim.localailab.feature.models.impl.models.domain.transfer.ModelDownloadFailure
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Streams one catalog file with verified HTTP range-resume and bounded transient retries. */
internal class ModelFileDownloader(
    private val transferState: ModelTransferStateStore,
    private val ensureRunning: suspend (ModelId, Long) -> Unit,
    private val publishProgress: suspend (StoredModelTransfer) -> Unit,
) {
    suspend fun download(request: ModelFileDownloadRequest): StoredModelTransfer {
        var transfer = request.transfer
        var failure: Throwable? = null
        for (attempt in RETRY_DELAYS_MS.indices) {
            if (attempt > 0) {
                val retryAfterMillis = (failure as? ModelDownloadFailure)?.retryAfterMillis ?: 0L
                val retryDelay = maxOf(RETRY_DELAYS_MS[attempt], retryAfterMillis)
                delay(retryDelay)
            }
            try {
                transfer = downloadOnce(request.copy(transfer = transfer))
                require(request.destination.length() == request.expectedBytes) {
                    "Downloaded size for ${request.relativePath} does not match the catalog."
                }
                require(request.destination.sha256().equals(request.expectedSha256, ignoreCase = true)) {
                    "Downloaded checksum for ${request.relativePath} does not match the catalog."
                }
                transferState.markFileVerified(transfer.modelIdAsModelId(), request.relativePath)
                return transfer
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                failure = error
                if (!error.isRetryableDownloadFailure() || attempt == RETRY_DELAYS_MS.lastIndex) break
            }
        }
        throw requireNotNull(failure)
    }

    @Suppress("LongMethod") // Resume negotiation and streaming progress form one transport operation.
    private suspend fun downloadOnce(request: ModelFileDownloadRequest): StoredModelTransfer {
        request.destination.parentFile?.let { require(it.isDirectory || it.mkdirs()) }
        var offset = request.destination.length().coerceAtMost(request.expectedBytes)
        if (request.destination.length() > request.expectedBytes) request.destination.delete()
        offset = request.destination.length()
        var resetUsed = false
        while (true) {
            ensureRunning(request.transfer.modelIdAsModelId(), request.transfer.executionGeneration)
            val validators = transferState.fileValidators(request.transfer.modelIdAsModelId())
            val validator = validators[request.relativePath] ?: request.existingValidator
            val response = openResponse(
                request.url,
                offset,
                validator,
                request.authorizationToken,
                request.transfer.modelIdAsModelId(),
            )
            response.connection.useResponse { connection ->
                val status = connection.responseCode
                if (status == HTTP_RANGE_NOT_SATISFIABLE) {
                    if (offset == request.expectedBytes) return request.transfer
                    if (resetUsed) {
                        failDownload(
                            ModelDownloadFailure(
                                "Server rejected the download range.",
                                retryable = false,
                            ),
                        )
                    }
                    request.destination.delete()
                    offset = 0L
                    resetUsed = true
                    return@useResponse
                }
                if (status !in 200..299) failDownload(httpFailure(status, connection))
                val append = when (status) {
                    HttpURLConnection.HTTP_OK -> {
                        if (offset > 0) {
                            if (resetUsed) {
                                failDownload(
                                    ModelDownloadFailure(
                                        "Server does not support resuming this file.",
                                        retryable = false,
                                    ),
                                )
                            }
                            request.destination.delete()
                            offset = 0L
                            resetUsed = true
                        }
                        false
                    }
                    HttpURLConnection.HTTP_PARTIAL -> {
                        validateContentRange(connection.getHeaderField("Content-Range"), offset, request.expectedBytes)
                        true
                    }
                    else -> failDownload(ModelDownloadFailure("Unexpected HTTP $status response.", retryable = false))
                }
                val announced = connection.getHeaderFieldLong("Content-Length", -1)
                if (announced >= 0 && announced != request.expectedBytes - if (append) offset else 0L) {
                    failDownload(
                        ModelDownloadFailure(
                            "Server response length does not match the catalog.",
                            retryable = false,
                        ),
                    )
                }
                val eTag = connection.getHeaderField("ETag")
                val lastModified = connection.getHeaderField("Last-Modified")
                if (append && validator?.eTag != null && eTag != null && validator.eTag != eTag) {
                    if (resetUsed) {
                        failDownload(
                            ModelDownloadFailure(
                                "Server changed the download while it was paused.",
                                retryable = false,
                            ),
                        )
                    }
                    request.destination.delete()
                    offset = 0L
                    resetUsed = true
                    return@useResponse
                }
                transferState.updateFileValidator(
                    request.transfer.modelIdAsModelId(),
                    request.relativePath,
                    eTag,
                    lastModified,
                    verified = false,
                )
                val progress = copyResponse(request, connection, append, offset)
                if (progress.written != request.expectedBytes) {
                    failDownload(ModelDownloadFailure("Download ended before the expected size.", retryable = true))
                }
                val updated = transferState.updateWhileRunning(
                    progress.transfer,
                    completedBytes = request.completedBeforeFile + progress.written,
                    currentRelativePath = request.relativePath,
                    message = null,
                ) ?: failDownload(CancellationException("The download was paused."))
                publishProgress(updated)
                return updated
            }
        }
    }

    private suspend fun copyResponse(
        request: ModelFileDownloadRequest,
        connection: HttpURLConnection,
        append: Boolean,
        initialOffset: Long,
    ): DownloadProgress {
        var transfer = request.transfer
        var written = initialOffset
        var lastReportedBytes = written
        var lastReportedAt = System.currentTimeMillis()
        connection.inputStream.use { input ->
            FileOutputStream(request.destination, append).buffered(FILE_BUFFER_BYTES).use { output ->
                val buffer = ByteArray(FILE_BUFFER_BYTES)
                while (true) {
                    ensureRunning(transfer.modelIdAsModelId(), transfer.executionGeneration)
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    written += read
                    val now = System.currentTimeMillis()
                    if (written - lastReportedBytes >= PROGRESS_BYTES || now - lastReportedAt >= PROGRESS_INTERVAL_MS) {
                        transfer = transferState.updateWhileRunning(
                            transfer,
                            completedBytes = request.completedBeforeFile + written,
                            currentRelativePath = request.relativePath,
                            message = null,
                        ) ?: failDownload(CancellationException("The download was paused."))
                        publishProgress(transfer)
                        lastReportedBytes = written
                        lastReportedAt = now
                    }
                }
            }
        }
        return DownloadProgress(transfer, written)
    }

    private fun openResponse(
        url: String,
        offset: Long,
        validator: ModelTransferFileEntity?,
        authorizationToken: String?,
        modelId: ModelId,
    ): DownloadResponse {
        var currentUrl = url
        repeat(MAX_REDIRECTS) { redirect ->
            val uri = URI(currentUrl)
            require(uri.scheme == HTTPS_SCHEME) { "Model downloads must use HTTPS." }
            val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept-Encoding", "identity")
                if (offset > 0) {
                    setRequestProperty("Range", "bytes=$offset-")
                    validator?.eTag?.takeIf { it.startsWith('"') && !it.startsWith("W/", ignoreCase = true) }
                        ?.let { setRequestProperty("If-Range", it) }
                        ?: validator?.lastModified?.let { setRequestProperty("If-Range", it) }
                }
                if (uri.host == HUGGING_FACE_HOST && authorizationToken != null) {
                    setRequestProperty("Authorization", "Bearer $authorizationToken")
                }
            }
            val status = connection.responseCode
            if (status !in 300..399) return DownloadResponse(connection)
            val location = connection.getHeaderField("Location")
            connection.disconnect()
            val redirectUri = requireNotNull(location) { "Redirect without a target." }.let(uri::resolve)
            require(redirectUri.scheme == HTTPS_SCHEME) { "Model downloads must use HTTPS." }
            currentUrl = redirectUri.toString()
            Log.i(TAG, "Catalog model redirected: modelId=${modelId.value}, redirect=${redirect + 1}")
        }
        throw ModelDownloadFailure("The download redirected too many times.", retryable = false)
    }

    private fun httpFailure(status: Int, connection: HttpURLConnection): ModelDownloadFailure = when (status) {
        HTTP_UNAUTHORIZED -> ModelDownloadFailure("Hugging Face token is invalid or expired.", retryable = false)
        HTTP_FORBIDDEN -> ModelDownloadFailure(
            "Access denied. Accept the model license and verify repository access.",
            retryable = false,
        )
        else -> ModelDownloadFailure(
            message = "Download failed with HTTP $status.",
            retryable = status == 408 || status == 429 || status >= 500,
            retryAfterMillis = connection.getHeaderField("Retry-After")
                ?.toLongOrNull()
                ?.times(1_000L)
                ?.coerceAtMost(MAX_RETRY_AFTER_MS),
        )
    }

    private fun validateContentRange(value: String?, offset: Long, expectedBytes: Long) {
        val match = CONTENT_RANGE.matchEntire(requireNotNull(value) { "Missing Content-Range for resumed download." })
            ?: throw ModelDownloadFailure("Invalid Content-Range for resumed download.", retryable = false)
        require(match.groupValues[1].toLong() == offset && match.groupValues[3].toLong() == expectedBytes) {
            "Server resumed a different byte range."
        }
    }

    private fun Throwable.isRetryableDownloadFailure(): Boolean = when (this) {
        is ModelDownloadFailure -> retryable
        is java.io.IOException -> true
        else -> false
    }

    private fun StoredModelTransfer.modelIdAsModelId() = ModelId(modelId)

    private fun failDownload(error: Throwable): Nothing = throw error

    private data class DownloadProgress(val transfer: StoredModelTransfer, val written: Long)

    private class DownloadResponse(val connection: HttpURLConnection)

    private inline fun <T> HttpURLConnection.useResponse(block: (HttpURLConnection) -> T): T = try {
        block(this)
    } finally {
        disconnect()
    }

    private companion object {
        const val TAG = "AiP123Models"
        const val HUGGING_FACE_HOST = "huggingface.co"
        const val HTTPS_SCHEME = "https"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_RANGE_NOT_SATISFIABLE = 416
        const val MAX_REDIRECTS = 5
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val FILE_BUFFER_BYTES = 256 * 1024
        const val PROGRESS_BYTES = 1L * 1024 * 1024
        const val PROGRESS_INTERVAL_MS = 500L
        const val MAX_RETRY_AFTER_MS = 60_000L
        val RETRY_DELAYS_MS = longArrayOf(0L, 2_000L, 5_000L, 15_000L)
        val CONTENT_RANGE = Regex("bytes (\\d+)-(\\d+)/(\\d+)")
    }
}

/** All immutable inputs needed to retrieve and verify one catalog file. */
internal data class ModelFileDownloadRequest(
    val transfer: StoredModelTransfer,
    val url: String,
    val destination: File,
    val relativePath: String,
    val expectedBytes: Long,
    val expectedSha256: String,
    val completedBeforeFile: Long,
    val authorizationToken: String?,
    val existingValidator: ModelTransferFileEntity?,
)
