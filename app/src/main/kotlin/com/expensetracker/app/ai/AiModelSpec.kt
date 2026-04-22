package com.expensetracker.app.ai

/**
 * Coordinates for the downloadable on-device model. Actual URL, size, and
 * checksum are wired in Phase 3; Phase 1 only needs the version string to
 * migrate cached weights when we bump it.
 */
object AiModelSpec {
    const val VERSION = "gemma-3n-e2b-it-litert-lm-v1"
    const val FILE_NAME = "gemma-3n-e2b-it.litertlm"

    // Filled in during Phase 3 once hosting + SHA-256 are confirmed.
    const val DOWNLOAD_URL = ""
    const val EXPECTED_SHA256 = ""
    const val EXPECTED_SIZE_BYTES = 0L
}
