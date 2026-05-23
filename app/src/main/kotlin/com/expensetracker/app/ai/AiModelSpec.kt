package com.expensetracker.app.ai

/**
 * Coordinates for the downloadable on-device model. Actual URL, size, and
 * checksum are wired in Phase 3; Phase 1 only needs the version string to
 * migrate cached weights when we bump it.
 */
object AiModelSpec {
    const val VERSION = "gemma-3n-e2b-it-int4-litertlm-v1"
    const val DISPLAY_NAME = "Gemma 3n E2B IT INT4"
    const val MODEL_ID = "google/gemma-3n-E2B-it-litert-lm"
    const val FILE_NAME = "gemma-3n-E2B-it-int4.litertlm"

    const val DOWNLOAD_URL =
        "https://huggingface.co/google/gemma-3n-E2B-it-litert-lm/resolve/main/gemma-3n-E2B-it-int4.litertlm"

    // Keep validation opt-in until the exact CDN artifact hash/byte count are fixed.
    const val EXPECTED_SHA256 = ""
    const val EXPECTED_SIZE_BYTES = 0L
    const val DISPLAY_SIZE_BYTES = 3_660_000_000L
}
