package com.expensetracker.app.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs

object DeviceInfoProvider {

    fun current(context: Context): AiCompatibility.DeviceInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        return AiCompatibility.DeviceInfo(
            supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty(),
            sdkInt = Build.VERSION.SDK_INT,
            isEmulator = isEmulator(),
            isLowRam = am?.isLowRamDevice == true,
            totalRamBytes = memInfo.totalMem,
            freeStorageBytes = freeStorageBytes()
        )
    }

    private fun freeStorageBytes(): Long = runCatching {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        stat.availableBlocksLong * stat.blockSizeLong
    }.getOrDefault(0L)

    private fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        return fingerprint.startsWith("generic") ||
            fingerprint.startsWith("unknown") ||
            model.contains("google_sdk") ||
            model.contains("Emulator") ||
            model.contains("Android SDK built for") ||
            product.contains("sdk_gphone") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu")
    }
}
