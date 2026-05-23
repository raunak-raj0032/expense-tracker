package com.expensetracker.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalModelDownloader @Inject constructor() {

    suspend fun download(
        target: File,
        hfToken: String = "",
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        if (AiModelSpec.DOWNLOAD_URL.isBlank()) {
            throw IOException("Model CDN URL is not configured.")
        }

        val connection = (URL(AiModelSpec.DOWNLOAD_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            instanceFollowRedirects = true
            if (hfToken.isNotBlank()) setRequestProperty("Authorization", "Bearer $hfToken")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("Model download failed with HTTP $code.")

            val total = connection.contentLengthLong.takeIf { it > 0L } ?: AiModelSpec.DISPLAY_SIZE_BYTES
            var downloaded = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }
            validate(target)
        } finally {
            connection.disconnect()
        }
    }

    private fun validate(file: File) {
        if (!file.exists() || file.length() == 0L) throw IOException("Downloaded model file is empty.")
        if (AiModelSpec.EXPECTED_SIZE_BYTES > 0L && file.length() != AiModelSpec.EXPECTED_SIZE_BYTES) {
            throw IOException("Downloaded model size does not match the expected artifact.")
        }
        val expectedHash = AiModelSpec.EXPECTED_SHA256.trim()
        if (expectedHash.isNotEmpty() && !sha256(file).equals(expectedHash, ignoreCase = true)) {
            throw IOException("Downloaded model checksum does not match the expected artifact.")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
