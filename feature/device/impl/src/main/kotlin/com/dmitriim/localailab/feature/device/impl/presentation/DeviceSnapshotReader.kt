package com.dmitriim.localailab.feature.device.impl.presentation

import android.app.ActivityManager
import android.app.Application
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.StatFs
import com.dmitriim.localailab.core.ui.R as CoreUiR
import java.util.Locale

internal fun readDeviceSnapshot(application: Application): DeviceSnapshot {
    val memory = ActivityManager.MemoryInfo().also { info ->
        application.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
    }
    val storage = StatFs(application.filesDir.absolutePath)
    val batteryIntentFilter = android.content.IntentFilter(
        android.content.Intent.ACTION_BATTERY_CHANGED,
    )
    val battery = application.registerReceiver(null, batteryIntentFilter)
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val batteryState = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING,
        BatteryManager.BATTERY_STATUS_FULL,
        -> application.getString(CoreUiR.string.device_battery_charging)
        else -> application.getString(CoreUiR.string.device_battery_not_charging)
    }
    val thermalState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        application.getString(
            CoreUiR.string.device_thermal_status,
            application.getSystemService(PowerManager::class.java).currentThermalStatus,
        )
    } else {
        application.getString(CoreUiR.string.device_thermal_status_unavailable)
    }
    return DeviceSnapshot(
        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
        androidVersion = application.getString(
            CoreUiR.string.device_android_version,
            Build.VERSION.RELEASE,
            Build.VERSION.SDK_INT,
        ),
        abis = Build.SUPPORTED_ABIS.joinToString(),
        totalMemory = memory.totalMem.toGiB(),
        availableMemory = memory.availMem.toGiB(),
        availableStorage = storage.availableBytes.toGiB(),
        batteryState = if (plugged != 0) {
            application.getString(CoreUiR.string.device_battery_plugged, batteryState)
        } else {
            batteryState
        },
        thermalState = thermalState,
        cpuInfo = application.getString(
            CoreUiR.string.device_available_processors,
            Runtime.getRuntime().availableProcessors(),
        ),
    )
}

private fun Long.toGiB(): String = String.format(Locale.US, "%.2f GiB", toDouble() / 1024 / 1024 / 1024)
