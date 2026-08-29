package com.dmitriim.localailab.feature.models.impl.data.transfer

import com.dmitriim.localailab.core.di.AppScope
import com.dmitriim.localailab.core.di.ApplicationCoroutineScope
import com.dmitriim.localailab.feature.models.api.domain.transfer.ModelTransferStartup
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Starts model-transfer recovery and connects background jobs to the transfer service. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<ModelTransferStartup>())
class ModelTransferStartupService(
    private val transferService: ModelTransferService,
    @param:ApplicationCoroutineScope private val applicationScope: CoroutineScope,
) : ModelTransferStartup {
    private val initialized = AtomicBoolean()

    override fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        ModelDownloadRuntime.executor = transferService
        applicationScope.launch(Dispatchers.IO) {
            transferService.recoverPersistedTransfers()
        }
    }
}
