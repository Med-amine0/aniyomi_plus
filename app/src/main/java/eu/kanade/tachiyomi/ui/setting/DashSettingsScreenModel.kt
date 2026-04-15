package eu.kanade.tachiyomi.ui.setting

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class ApiTestResult(
    val name: String,
    val url: String,
    val status: Int?,
    val success: Boolean,
    val message: String,
    val responseTime: Long,
)

@Immutable
data class DashSettingsState(
    val isTesting: Boolean = false,
    val log: List<String> = emptyList(),
    val results: List<ApiTestResult> = emptyList(),
) {
    val logText: String
        get() = buildString {
            if (results.isEmpty() && log.isEmpty()) {
                append("Tap 'Test Jikan API' to start testing...")
            } else {
                log.forEach { appendLine(it) }
                if (results.isNotEmpty()) {
                    appendLine()
                    appendLine("=== API Test Results ===")
                    results.forEach { result ->
                        appendLine()
                        appendLine("📡 ${result.name}")
                        appendLine("   URL: ${result.url}")
                        appendLine("   Status: ${result.status ?: "ERROR"}")
                        appendLine("   Response Time: ${result.responseTime}ms")
                        appendLine("   Result: ${if (result.success) "✓ SUCCESS" else "✗ FAILED"}")
                        if (result.message.isNotEmpty()) {
                            appendLine("   Message: ${result.message}")
                        }
                    }
                }
            }
        }
}

class DashSettingsScreenModel : ScreenModel {

    private val _state = MutableStateFlow(DashSettingsState())
    val state: StateFlow<DashSettingsState> = _state.asStateFlow()

    fun addLog(message: String) {
        _state.update { it.copy(log = it.log + message) }
    }

    fun clearLog() {
        _state.update { it.copy(log = emptyList(), results = emptyList()) }
    }

    fun testAllApis() {
        if (_state.value.isTesting) return

        screenModelScope.launch {
            _state.update { it.copy(isTesting = true, results = emptyList()) }
            addLog("Starting Jikan API tests...")
            addLog("")

            val results = mutableListOf<ApiTestResult>()

            // Test Genres
            val genresResult = testApi(
                name = "Genres API",
                url = "https://api.jikan.moe/v4/genres/anime",
            )
            results.add(genresResult)

            delay(500)

            // Test Top Anime (Trending)
            val topAnimeResult = testApi(
                name = "Top Anime (Trending)",
                url = "https://api.jikan.moe/v4/top/anime",
            )
            results.add(topAnimeResult)

            delay(500)

            // Test Top Anime Movies
            val moviesResult = testApi(
                name = "Top Anime Movies",
                url = "https://api.jikan.moe/v4/top/anime?type=movie",
            )
            results.add(moviesResult)

            delay(500)

            // Test Anime by Genre (Action = 1)
            val genreAnimeResult = testApi(
                name = "Anime by Genre (Action)",
                url = "https://api.jikan.moe/v4/anime?genres=1",
            )
            results.add(genreAnimeResult)

            addLog("")
            addLog("All tests completed!")

            _state.update {
                it.copy(
                    isTesting = false,
                    results = results,
                )
            }
        }
    }

    private suspend fun testApi(name: String, url: String): ApiTestResult {
        val startTime = System.currentTimeMillis()
        return withContext(Dispatchers.IO) {
            try {
                addLog("Testing: $name")
                addLog("  URL: $url")

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val status = connection.responseCode
                val responseTime = System.currentTimeMillis() - startTime

                val success = status == 200

                var message = ""
                if (success) {
                    try {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = org.json.JSONObject(body)
                        if (json.has("data")) {
                            val data = json.get("data")
                            message = "Got ${when (data) {
                                is org.json.JSONArray -> "${data.length()} items"
                                is org.json.JSONObject -> "data object"
                                else -> "unknown format"
                            }}"
                        }
                        if (json.has("pagination")) {
                            val pagination = json.getJSONObject("pagination")
                            val hasNext = pagination.optBoolean("has_next_page", false)
                            if (hasNext) {
                                message += " (has more pages)"
                            }
                        }
                    } catch (e: Exception) {
                        message = "Response received but failed to parse"
                    }
                } else {
                    message = "HTTP Error: $status"
                }

                connection.disconnect()

                ApiTestResult(
                    name = name,
                    url = url,
                    status = status,
                    success = success,
                    message = message,
                    responseTime = responseTime,
                )
            } catch (e: Exception) {
                val responseTime = System.currentTimeMillis() - startTime
                addLog("  ERROR: ${e.javaClass.simpleName}: ${e.message}")

                ApiTestResult(
                    name = name,
                    url = url,
                    status = null,
                    success = false,
                    message = "${e.javaClass.simpleName}: ${e.message}",
                    responseTime = responseTime,
                )
            }
        }
    }
}
