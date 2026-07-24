package com.dmitriim.localaiplayground.feature.device.presentation

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.StatFs
import java.util.Locale

internal fun readDeviceSnapshot(application: Application): DeviceSnapshot {
    val memory = ActivityManager.MemoryInfo().also { info ->
        application.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
    }
    val storage = StatFs(application.filesDir.absolutePath)
    return DeviceSnapshot(
        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
        androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        abis = Build.SUPPORTED_ABIS.joinToString(),
        totalMemory = memory.totalMem.toGiB(),
        availableMemory = memory.availMem.toGiB(),
        availableStorage = storage.availableBytes.toGiB(),
    )
}

private fun Long.toGiB(): String =
    String.format(Locale.US, "%.2f GiB", toDouble() / 1024 / 1024 / 1024)
