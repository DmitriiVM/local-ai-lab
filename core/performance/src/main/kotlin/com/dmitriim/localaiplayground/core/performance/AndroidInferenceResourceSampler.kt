package com.dmitriim.localaiplayground.core.performance

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import dev.zacsweers.metro.Inject

@Inject
class AndroidInferenceResourceSampler(
    private val application: Application,
) : InferenceResourceSampler {
    private val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val batteryManager = application.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = application.getSystemService(Context.POWER_SERVICE) as PowerManager

    override fun snapshot(): InferenceResourceSnapshot = InferenceResourceSnapshot(
        elapsedRealtimeMs = SystemClock.elapsedRealtime(),
        processCpuTimeMs = Process.getElapsedCpuTime(),
        pssBytes = processPssBytes(),
        availableMemoryBytes = availableMemoryBytes(),
        batteryEnergyNwh = batteryManager.longPropertyOrNull(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER),
        batteryChargeUah = batteryManager.intPropertyOrNull(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)?.toLong(),
        batteryCurrentUa = batteryManager.intPropertyOrNull(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
        powerSaveMode = powerManager.isPowerSaveMode,
        thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) powerManager.currentThermalStatus else null,
        thermalHeadroom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            powerManager.getThermalHeadroom(0).takeUnless { it.isNaN() }
        } else {
            null
        },
    )

    override fun deviceSnapshot() = InferenceDeviceSnapshot(
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        hardware = Build.HARDWARE,
        socManufacturer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER else null,
        socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null,
        sdkInt = Build.VERSION.SDK_INT,
        abis = Build.SUPPORTED_ABIS.toList(),
        availableProcessors = Runtime.getRuntime().availableProcessors(),
    )

    private fun processPssBytes(): Long? = runCatching {
        activityManager.getProcessMemoryInfo(intArrayOf(Process.myPid()))
            .firstOrNull()
            ?.totalPss
            ?.toLong()
            ?.times(1_024L)
    }.getOrNull()

    private fun availableMemoryBytes(): Long? = runCatching {
        ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo).availMem
    }.getOrNull()

    private fun BatteryManager.longPropertyOrNull(property: Int): Long? = getLongProperty(property).takeUnless { it == Long.MIN_VALUE }

    private fun BatteryManager.intPropertyOrNull(property: Int): Int? = getIntProperty(property).takeUnless { it == Int.MIN_VALUE }
}
