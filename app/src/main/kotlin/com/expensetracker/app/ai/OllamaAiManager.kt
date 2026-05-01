package com.expensetracker.app.ai

import com.expensetracker.app.core.prefs.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

@Singleton
class OllamaAiManager @Inject constructor(
    private val userPreferences: UserPreferences
) : OnDeviceAiManager {

    override fun observeAvailability(): Flow<AiAvailability> =
        userPreferences.aiModelState.map { state ->
            when {
                state.lastError.isNotBlank() -> AiAvailability.Error(state.lastError)
                normalizeEndpoint(state.endpoint) == null -> AiAvailability.NeedsDownload
                else -> AiAvailability.Ready
            }
        }

    override suspend fun downloadModel(): Result<Unit> = runCatching {
        val state = userPreferences.aiModelState.first()
        val endpoint = normalizeEndpoint(state.endpoint)
            ?: error("Enter your Ollama host first (e.g. http://192.168.1.5:11434).")
        val model = state.modelName.ifBlank { DEFAULT_MODEL }

        val body = OllamaClient.getJson("$endpoint/api/tags")
        val installed = parseModelNames(body)
        if (installed.isEmpty()) {
            error("Reached Ollama at $endpoint but no models are pulled. Run: ollama pull $model")
        }
        if (installed.none { it.equals(model, ignoreCase = true) || it.startsWith("$model:", ignoreCase = true) }) {
            error("Model \"$model\" not pulled on host. Available: ${installed.joinToString(", ")}. Run: ollama pull $model")
        }

        userPreferences.setAiEndpoint(endpoint)
    }.onFailure {
        userPreferences.setAiLastError(formatError(it))
    }

    override suspend fun deleteModel() {
        userPreferences.clearAiModelState()
    }

    override suspend fun initializeIfNeeded(): Result<Unit> = downloadModel()

    override suspend fun createConversation(): AiConversation {
        val state = userPreferences.aiModelState.first()
        val endpoint = normalizeEndpoint(state.endpoint)
            ?: error("Enter your Ollama host first (e.g. http://192.168.1.5:11434).")
        val model = state.modelName.ifBlank { DEFAULT_MODEL }
        return OllamaConversation(endpoint, model) { userPreferences.setAiLastError(formatError(it)) }
    }

    private class OllamaConversation(
        private val endpoint: String,
        private val model: String,
        private val recordError: suspend (Throwable) -> Unit
    ) : AiConversation {
        override suspend fun respond(prompt: String): String {
            val payload = JSONObject()
                .put("model", model)
                .put("prompt", prompt)
                .put("stream", false)
                .put(
                    "options",
                    JSONObject()
                        .put("temperature", 0.25)
                        .put("num_predict", 360)
                )
            val response = try {
                OllamaClient.postJson(
                    url = "$endpoint/api/generate",
                    body = payload.toString(),
                    readTimeoutMillis = GENERATE_READ_TIMEOUT_MS
                )
            } catch (t: Throwable) {
                recordError(t)
                throw IOException(formatError(t), t)
            }
            return JSONObject(response).optString("response").trim()
        }

        override fun close() = Unit
    }

    companion object {
        const val DEFAULT_MODEL = "llama3.2:3b"
        private const val OLLAMA_DEFAULT_PORT = 11434
        private const val GENERATE_READ_TIMEOUT_MS = 180_000

        fun normalizeEndpoint(raw: String): String? {
            val trimmed = raw.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            val withScheme = if (
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) trimmed else "http://$trimmed"

            val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
            val host = uri.host?.takeIf { it.isNotBlank() } ?: return null
            val scheme = (uri.scheme ?: "http").lowercase()
            val explicitPort = uri.port
            val authority = when {
                explicitPort > 0 -> "$host:$explicitPort"
                scheme == "https" -> host
                else -> "$host:$OLLAMA_DEFAULT_PORT"
            }
            return "$scheme://$authority"
        }

        fun formatError(t: Throwable): String = when (t) {
            is UnknownHostException ->
                "Can't resolve that host. Check the address or your network."
            is ConnectException ->
                "Connection refused. Is Ollama running on that port and the firewall open?"
            is SocketTimeoutException ->
                "Host did not respond in time. Same Wi-Fi, or is the tunnel up?"
            is SSLException ->
                "TLS handshake failed. Use http:// for LAN, https:// for tunnels."
            else -> t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
        }

        private fun parseModelNames(tagsResponseBody: String): List<String> {
            val obj = runCatching { JSONObject(tagsResponseBody) }.getOrNull() ?: return emptyList()
            val arr: JSONArray = obj.optJSONArray("models") ?: return emptyList()
            return buildList {
                for (i in 0 until arr.length()) {
                    val m = arr.optJSONObject(i) ?: continue
                    val name = m.optString("name")
                    if (name.isNotEmpty()) add(name)
                }
            }
        }
    }
}

private object OllamaClient {
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val DEFAULT_READ_TIMEOUT_MS = 15_000
    private const val MAX_ATTEMPTS = 2
    private const val RETRY_BACKOFF_MS = 400L

    suspend fun getJson(url: String, readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MS): String =
        request(url, method = "GET", body = null, readTimeoutMillis = readTimeoutMillis, retry = true)

    suspend fun postJson(url: String, body: String, readTimeoutMillis: Int = DEFAULT_READ_TIMEOUT_MS): String =
        request(url, method = "POST", body = body, readTimeoutMillis = readTimeoutMillis, retry = false)

    private suspend fun request(
        url: String,
        method: String,
        body: String?,
        readTimeoutMillis: Int,
        retry: Boolean
    ): String = withContext(Dispatchers.IO) {
        val target = URL(url)
        var lastError: Throwable? = null
        val attempts = if (retry) MAX_ATTEMPTS else 1
        repeat(attempts) { attempt ->
            try {
                return@withContext doRequest(target, method, body, readTimeoutMillis)
            } catch (t: Throwable) {
                lastError = t
                if (!retry || !isRetriable(t) || attempt == attempts - 1) throw t
                delay(RETRY_BACKOFF_MS)
            }
        }
        throw lastError ?: IOException("Unknown network error")
    }

    private fun doRequest(
        target: URL,
        method: String,
        body: String?,
        readTimeoutMillis: Int
    ): String {
        val connection = (target.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = readTimeoutMillis
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            instanceFollowRedirects = true
            if (body != null) doOutput = true
        }
        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            if (code !in 200..299) throw IOException(describeHttpError(code, text))
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun describeHttpError(code: Int, body: String): String {
        val errorMsg = runCatching { JSONObject(body).optString("error") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
        return when {
            errorMsg != null -> "Ollama HTTP $code: $errorMsg"
            else -> "HTTP $code: ${body.take(200)}"
        }
    }

    private fun isRetriable(t: Throwable): Boolean = when (t) {
        is SocketTimeoutException, is ConnectException -> true
        else -> false
    }
}
