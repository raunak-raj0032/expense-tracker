package com.expensetracker.app.ai

/**
 * Pure device-capability gate for the on-device AI feature.
 *
 * V1 targets high-end Android phones only: arm64-v8a, Android 12+, non-emulator,
 * not a low-RAM device, at least 8 GB total RAM, and at least 2 GB free storage
 * before the model is downloaded. Anything weaker gets routed to the Unsupported
 * branch and the feature is hidden rather than offered on a CPU-only fallback.
 */
object AiCompatibility {

    const val REQUIRED_ABI = "arm64-v8a"
    const val MIN_SDK_INT = 31
    const val MIN_RAM_BYTES = 8L * 1024 * 1024 * 1024
    const val MIN_FREE_STORAGE_BYTES = 2L * 1024 * 1024 * 1024

    data class DeviceInfo(
        val supportedAbis: List<String>,
        val sdkInt: Int,
        val isEmulator: Boolean,
        val isLowRam: Boolean,
        val totalRamBytes: Long,
        val freeStorageBytes: Long
    )

    enum class Reason {
        UNSUPPORTED_ABI,
        API_TOO_OLD,
        EMULATOR,
        LOW_RAM_DEVICE,
        INSUFFICIENT_RAM,
        INSUFFICIENT_STORAGE
    }

    sealed class Result {
        object Supported : Result()
        data class Unsupported(val reasons: List<Reason>) : Result()
    }

    fun evaluate(info: DeviceInfo): Result {
        val reasons = buildList {
            if (REQUIRED_ABI !in info.supportedAbis) add(Reason.UNSUPPORTED_ABI)
            if (info.sdkInt < MIN_SDK_INT) add(Reason.API_TOO_OLD)
            if (info.isEmulator) add(Reason.EMULATOR)
            if (info.isLowRam) add(Reason.LOW_RAM_DEVICE)
            if (info.totalRamBytes < MIN_RAM_BYTES) add(Reason.INSUFFICIENT_RAM)
            if (info.freeStorageBytes < MIN_FREE_STORAGE_BYTES) add(Reason.INSUFFICIENT_STORAGE)
        }
        return if (reasons.isEmpty()) Result.Supported else Result.Unsupported(reasons)
    }
}
