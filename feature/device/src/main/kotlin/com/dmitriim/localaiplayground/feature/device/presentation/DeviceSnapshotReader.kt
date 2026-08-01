package com.dmitriim.localaiplayground.feature.device.presentation

import android.app.ActivityManager
import android.app.Application
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import java.util.Locale

internal fun readDeviceSnapshot(application: Application): DeviceSnapshot {
    val memory = ActivityManager.MemoryInfo().also { info ->
        application.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
    }
    val storage = StatFs(application.filesDir.absolutePath)
    val battery = application.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val batteryState = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> "Charging"
        else -> "Not charging"
    }
    val thermalState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        "Thermal status ${application.getSystemService(PowerManager::class.java).currentThermalStatus}"
    } else "Thermal status unavailable before Android 10"
    return DeviceSnapshot(
        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
        androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        abis = Build.SUPPORTED_ABIS.joinToString(),
        totalMemory = memory.totalMem.toGiB(),
        availableMemory = memory.availMem.toGiB(),
        availableStorage = storage.availableBytes.toGiB(),
        batteryState = "$batteryState${if (plugged != 0) " (plugged in)" else ""}",
        thermalState = thermalState,
        cpuInfo = "${Runtime.getRuntime().availableProcessors()} available processor(s)",
    )
}

private fun Long.toGiB(): String =
    String.format(Locale.US, "%.2f GiB", toDouble() / 1024 / 1024 / 1024)
