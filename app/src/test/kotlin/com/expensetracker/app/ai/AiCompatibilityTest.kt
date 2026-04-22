package com.expensetracker.app.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCompatibilityTest {

    private fun info(
        abis: List<String> = listOf("arm64-v8a"),
        sdkInt: Int = 34,
        isEmulator: Boolean = false,
        isLowRam: Boolean = false,
        totalRamBytes: Long = 12L * 1024 * 1024 * 1024,
        freeStorageBytes: Long = 6L * 1024 * 1024 * 1024
    ) = AiCompatibility.DeviceInfo(abis, sdkInt, isEmulator, isLowRam, totalRamBytes, freeStorageBytes)

    @Test
    fun supportedOnFlagshipProfile() {
        assertEquals(AiCompatibility.Result.Supported, AiCompatibility.evaluate(info()))
    }

    @Test
    fun rejectsThirtyTwoBitAbiOnly() {
        val result = AiCompatibility.evaluate(info(abis = listOf("armeabi-v7a")))
        assertTrue(result is AiCompatibility.Result.Unsupported)
        assertTrue((result as AiCompatibility.Result.Unsupported).reasons
            .contains(AiCompatibility.Reason.UNSUPPORTED_ABI))
    }

    @Test
    fun rejectsApi30AndBelow() {
        val result = AiCompatibility.evaluate(info(sdkInt = 30))
        assertTrue(result is AiCompatibility.Result.Unsupported)
        assertTrue((result as AiCompatibility.Result.Unsupported).reasons
            .contains(AiCompatibility.Reason.API_TOO_OLD))
    }

    @Test
    fun rejectsEmulator() {
        val result = AiCompatibility.evaluate(info(isEmulator = true))
        assertTrue(result is AiCompatibility.Result.Unsupported)
        assertTrue((result as AiCompatibility.Result.Unsupported).reasons
            .contains(AiCompatibility.Reason.EMULATOR))
    }

    @Test
    fun rejectsLowRamDeviceFlag() {
        val result = AiCompatibility.evaluate(info(isLowRam = true))
        assertTrue(result is AiCompatibility.Result.Unsupported)
        assertTrue((result as AiCompatibility.Result.Unsupported).reasons
            .contains(AiCompatibility.Reason.LOW_RAM_DEVICE))
    }

    @Test
    fun rejectsBelowEightGigRam() {
        val result = AiCompatibility.evaluate(info(totalRamBytes = 6L * 1024 * 1024 * 1024))
        assertTrue(result is AiCompatibility.Result.Unsupported)
        assertTrue((result as AiCompatibility.Result.Unsupported).reasons
            .contains(AiCompatibility.Reason.INSUFFICIENT_RAM))
    }

    @Test
    fun rejectsInsufficientStorage() {
        val result = AiCompatibility.evaluate(info(freeStorageBytes = 512L * 1024 * 1024))
        assertTrue(result is AiCompatibility.Result.Unsupported)
        assertTrue((result as AiCompatibility.Result.Unsupported).reasons
            .contains(AiCompatibility.Reason.INSUFFICIENT_STORAGE))
    }

    @Test
    fun collectsAllFailureReasonsAtOnce() {
        val result = AiCompatibility.evaluate(
            info(
                abis = listOf("armeabi-v7a"),
                sdkInt = 29,
                isEmulator = true,
                isLowRam = true,
                totalRamBytes = 2L * 1024 * 1024 * 1024,
                freeStorageBytes = 100L * 1024 * 1024
            )
        )
        assertTrue(result is AiCompatibility.Result.Unsupported)
        assertEquals(
            setOf(
                AiCompatibility.Reason.UNSUPPORTED_ABI,
                AiCompatibility.Reason.API_TOO_OLD,
                AiCompatibility.Reason.EMULATOR,
                AiCompatibility.Reason.LOW_RAM_DEVICE,
                AiCompatibility.Reason.INSUFFICIENT_RAM,
                AiCompatibility.Reason.INSUFFICIENT_STORAGE
            ),
            (result as AiCompatibility.Result.Unsupported).reasons.toSet()
        )
    }

    @Test
    fun acceptsDeviceWithMultipleAbisIncludingArm64() {
        val result = AiCompatibility.evaluate(info(abis = listOf("arm64-v8a", "armeabi-v7a", "armeabi")))
        assertEquals(AiCompatibility.Result.Supported, result)
    }

    @Test
    fun boundaryAtExactMinimums() {
        val result = AiCompatibility.evaluate(
            info(
                sdkInt = AiCompatibility.MIN_SDK_INT,
                totalRamBytes = AiCompatibility.MIN_RAM_BYTES,
                freeStorageBytes = AiCompatibility.MIN_FREE_STORAGE_BYTES
            )
        )
        assertEquals(AiCompatibility.Result.Supported, result)
    }
}
