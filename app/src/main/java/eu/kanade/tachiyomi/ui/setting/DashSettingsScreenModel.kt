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
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class ApiTestResult(
    val name: String,
    val url: String,
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
                append("Tap 'Test AniList API' to start testing...")
            } else {
                log.forEach { appendLine(it) }
                if (results.isNotEmpty()) {
                    appendLine()
                    appendLine("=== API Test Results ===")
                    results.forEach { result ->
                        appendLine()
                        appendLine("📡 ${result.name}")
                        appendLine("   Query: ${result.url}")
                        appendLine("   Result: ${if (result.success) "✓ SUCCESS" else "✗ FAILED"}")
                        appendLine("   Response Time: ${result.responseTime}ms")
                        if (result.message.isNotEmpty()) {
                            appendLine("   ${result.message}")
                        }
                    }
                }
            }
        }
}

class DashSettingsScreenModel : ScreenModel {

    private val _state = MutableStateFlow(DashSettingsState())
    val state: StateFlow<DashSettingsState> = _state.asStateFlow()

    companion object {
        private const val ANILIST_URL = "https://graphql.anilist.co"
    }

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
            addLog("Starting AniList API tests...")
            addLog("")

            val results = mutableListOf<ApiTestResult>()

            // Test Genre Names Query
            val genresQuery = "query { GenreNames }"
            val genresResult = testGraphQL(
                name = "Genre Names",
                query = genresQuery,
            )
            results.add(genresResult)

            delay(500)

            // Test Trending Anime
            val trendingQuery = """
                query(${'$'}page: Int) {
                    Page(page: ${'$'}page, perPage: 3) {
                        pageInfo { hasNextPage }
                        media(type: ANIME, sort: TRENDING_DESC) {
                            title { romaji }
                            coverImage { medium }
                        }
                    }
                }
            """.trimIndent()
            val trendingResult = testGraphQL(
                name = "Trending Anime",
                query = trendingQuery,
                variables = JSONObject().put("page", 1),
            )
            results.add(trendingResult)

            delay(500)

            // Test Genre Filter (Action)
            val actionQuery = """
                query(${'$'}page: Int, ${'$'}genre: String) {
                    Page(page: ${'$'}page, perPage: 3) {
                        pageInfo { hasNextPage }
                        media(type: ANIME, genre: ${'$'}genre, sort: SCORE_DESC) {
                            title { romaji }
                            coverImage { medium }
                        }
                    }
                }
            """.trimIndent()
            val actionResult = testGraphQL(
                name = "Action Genre",
                query = actionQuery,
                variables = JSONObject().put("page", 1).put("genre", "Action"),
            )
            results.add(actionResult)

            delay(500)

            // Test Movies
            val moviesQuery = """
                query(${'$'}page: Int) {
                    Page(page: ${'$'}page, perPage: 3) {
                        pageInfo { hasNextPage }
                        media(type: ANIME, format: MOVIE, sort: TRENDING_DESC) {
                            title { romaji }
                            coverImage { medium }
                        }
                    }
                }
            """.trimIndent()
            val moviesResult = testGraphQL(
                name = "Movies",
                query = moviesQuery,
                variables = JSONObject().put("page", 1),
            )
            results.add(moviesResult)

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

    private suspend fun testGraphQL(
        name: String,
        query: String,
        variables: JSONObject = JSONObject(),
    ): ApiTestResult {
        val startTime = System.currentTimeMillis()
        return withContext(Dispatchers.IO) {
            try {
                addLog("Testing: $name")
                addLog("   Query: $query")

                val url = URL(ANILIST_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("query", query)
                    put("variables", variables)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseTime = System.currentTimeMillis() - startTime

                if (responseCode != 200) {
                    connection.disconnect()
                    return@withContext ApiTestResult(
                        name = name,
                        url = query,
                        success = false,
                        message = "HTTP Error: $responseCode",
                        responseTime = responseTime,
                    )
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val json = JSONObject(response)

                var message = ""
                if (json.has("errors")) {
                    val errors = json.getJSONArray("errors")
                    if (errors.length() > 0) {
                        message = "Error: ${errors.getJSONObject(0).optString("message", "Unknown error")}"
                    }
                } else if (json.has("data")) {
                    val data = json.get("data")
                    message = when {
                        data is JSONObject && data.has("Page") -> {
                            val page = data.getJSONObject("Page")
                            val media = page.optJSONArray("media")
                            if (media != null) {
                                "Got ${media.length()} items"
                            } else {
                                "Got data object"
                            }
                        }
                        data is JSONObject && data.has("GenreNames") -> {
                            val genres = data.getJSONArray("GenreNames")
                            "Got ${genres.length()} genres"
                        }
                        else -> "Got data"
                    }
                }

                ApiTestResult(
                    name = name,
                    url = query,
                    success = message.isNotEmpty() && !message.startsWith("Error:"),
                    message = message,
                    responseTime = responseTime,
                )
            } catch (e: Exception) {
                val responseTime = System.currentTimeMillis() - startTime
                addLog("   ERROR: ${e.javaClass.simpleName}: ${e.message}")

                ApiTestResult(
                    name = name,
                    url = query,
                    success = false,
                    message = "${e.javaClass.simpleName}: ${e.message}",
                    responseTime = responseTime,
                )
            }
        }
    }
}
