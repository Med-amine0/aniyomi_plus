package eu.kanade.tachiyomi.ui.main.dashboard

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
import org.json.JSONArray
import org.json.JSONObject
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.manga.interactor.GetLibraryManga
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class DiscoveredAnime(
    val title: String,
    val imageUrl: String,
    val siteUrl: String,
)

@Immutable
data class DashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val animeView: Boolean = true,
    val showAllExpanded: Boolean = false,
    val allAnime: List<LibraryAnime> = emptyList(),
    val allManga: List<LibraryManga> = emptyList(),
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val selectedGenre: String? = null,
    val discoveredAnime: List<DiscoveredAnime> = emptyList(),
    val discoveredMovies: List<DiscoveredAnime> = emptyList(),
    val animePage: Int = 1,
    val moviesPage: Int = 1,
    val isLoadingAnime: Boolean = false,
    val isLoadingMovies: Boolean = false,
    val animeHasMore: Boolean = true,
    val moviesHasMore: Boolean = true,
    val animeError: String? = null,
    val moviesError: String? = null,
    val genresError: String? = null,
) {
    val recentAnime: List<LibraryAnime>
        get() = allAnime
            .filter { it.hasStarted }
            .sortedByDescending { it.lastSeen }
            .take(3)

    val recentManga: List<LibraryManga>
        get() = allManga
            .filter { it.hasStarted }
            .sortedByDescending { it.lastRead }
            .take(3)

    val inProgressAnime: List<LibraryAnime>
        get() = allAnime
            .filter { it.unseenCount > 0L }
            .sortedByDescending { getProgressPercent(it) }

    val inProgressManga: List<LibraryManga>
        get() = allManga
            .filter { it.unreadCount > 0L }
            .sortedByDescending { getProgressPercent(it) }

    val completedAnime: List<LibraryAnime>
        get() = allAnime
            .filter { it.unseenCount == 0L && it.totalCount > 0L }
            .sortedByDescending { it.lastSeen }

    val completedManga: List<LibraryManga>
        get() = allManga
            .filter { it.unreadCount == 0L && it.totalChapters > 0L }
            .sortedByDescending { it.lastRead }

    fun getProgressPercent(libraryAnime: LibraryAnime): Float =
        if (libraryAnime.totalCount > 0L) {
            libraryAnime.seenCount.toFloat() / libraryAnime.totalCount.toFloat()
        } else {
            0f
        }

    fun getProgressPercent(libraryManga: LibraryManga): Float =
        if (libraryManga.totalChapters > 0L) {
            libraryManga.readCount.toFloat() / libraryManga.totalChapters.toFloat()
        } else {
            0f
        }

    fun getOverlayText(libraryAnime: LibraryAnime): String {
        val percent = (getProgressPercent(libraryAnime) * 100).toInt()
        return "$percent% • Ep ${libraryAnime.seenCount}/${libraryAnime.totalCount}"
    }

    fun getOverlayText(libraryManga: LibraryManga): String {
        val percent = (getProgressPercent(libraryManga) * 100).toInt()
        return "$percent% • Ch. ${libraryManga.readCount}"
    }
}

class DashboardScreenModel : ScreenModel {

    private val getLibraryAnime: GetLibraryAnime = Injekt.get()
    private val getLibraryManga: GetLibraryManga = Injekt.get()

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    companion object {
        private const val ANILIST_URL = "https://graphql.anilist.co"
    }

    init {
        loadData()
        screenModelScope.launch {
            fetchGenresAndTags()
            fetchAnime()
            fetchMovies()
        }
    }

    private fun loadData() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val anime = getLibraryAnime.await()
                val manga = getLibraryManga.await()

                _state.update {
                    it.copy(
                        isLoading = false,
                        allAnime = anime,
                        allManga = manga,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleAnimeMangaView() {
        _state.update { it.copy(animeView = !it.animeView) }
    }

    fun toggleShowAll() {
        _state.update { it.copy(showAllExpanded = !it.showAllExpanded) }
    }

    fun refresh() {
        screenModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            try {
                val anime = getLibraryAnime.await()
                val manga = getLibraryManga.await()

                _state.update {
                    it.copy(
                        isRefreshing = false,
                        allAnime = anime,
                        allManga = manga,
                    )
                }

                fetchGenresAndTags()
                fetchAnime()
                fetchMovies()
            } catch (e: Exception) {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun fetchGraphQL(query: String, variables: JSONObject = JSONObject()): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
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
                if (responseCode != 200) {
                    connection.disconnect()
                    return@withContext null
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                JSONObject(response)
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun fetchGenresAndTags() {
        screenModelScope.launch {
            val query = """
                query {
                    GenreCollection
                    MediaTagCollection {
                        name
                    }
                }
            """.trimIndent()

            try {
                val json = fetchGraphQL(query) ?: run {
                    _state.update { it.copy(genresError = "Failed to fetch genres") }
                    return@launch
                }

                val data = json.optJSONObject("data")

                val genreCollection = data?.optJSONArray("GenreCollection")?.let { arr ->
                    (0 until arr.length()).map { arr.getString(it) }.sorted()
                } ?: emptyList()

                val tagCollection = data?.optJSONArray("MediaTagCollection")?.let { arr ->
                    (0 until arr.length()).mapNotNull { i ->
                        arr.optJSONObject(i)?.optString("name")
                    }.sorted()
                } ?: emptyList()

                _state.update {
                    it.copy(
                        genres = genreCollection,
                        tags = tagCollection,
                        genresError = null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(genresError = "Failed to fetch genres") }
            }
        }
    }

    private fun fetchAnime(append: Boolean = false) {
        val currentState = _state.value
        if (currentState.isLoadingAnime && !append) return

        screenModelScope.launch {
            _state.update { it.copy(isLoadingAnime = true, animeError = null) }

            try {
                delay(200)

                val page = if (append) currentState.animePage + 1 else 1
                val genre = currentState.selectedGenre

                val query: String
                val variables = JSONObject()

                if (genre == null) {
                    query = """
                        query(${'$'}page: Int) {
                            Page(page: ${'$'}page, perPage: 12) {
                                pageInfo { hasNextPage }
                                media(type: ANIME, sort: TRENDING_DESC) {
                                    title { romaji }
                                    coverImage { medium }
                                    siteUrl
                                }
                            }
                        }
                    """.trimIndent()
                    variables.put("page", page)
                } else {
                    query = """
                        query(${'$'}page: Int, ${'$'}genre: String) {
                            Page(page: ${'$'}page, perPage: 12) {
                                pageInfo { hasNextPage }
                                media(type: ANIME, genre: ${'$'}genre, sort: SCORE_DESC) {
                                    title { romaji }
                                    coverImage { medium }
                                    siteUrl
                                }
                            }
                        }
                    """.trimIndent()
                    variables.put("page", page)
                    variables.put("genre", genre)
                }

                val json = fetchGraphQL(query, variables) ?: run {
                    _state.update { it.copy(isLoadingAnime = false, animeError = "Failed to fetch anime") }
                    return@launch
                }

                val pageData = json.optJSONObject("data")?.optJSONObject("Page")
                val pageInfo = pageData?.optJSONObject("pageInfo")
                val hasNextPage = pageInfo?.optBoolean("hasNextPage", false) ?: false
                val mediaArray = pageData?.optJSONArray("media") ?: JSONArray()

                val animeList = mutableListOf<DiscoveredAnime>()
                for (i in 0 until mediaArray.length()) {
                    val media = mediaArray.optJSONObject(i) ?: continue
                    val titleObj = media.optJSONObject("title") ?: continue
                    val coverObj = media.optJSONObject("coverImage") ?: continue

                    animeList.add(
                        DiscoveredAnime(
                            title = titleObj.optString("romaji", ""),
                            imageUrl = coverObj.optString("medium", ""),
                            siteUrl = media.optString("siteUrl", ""),
                        ),
                    )
                }

                _state.update {
                    it.copy(
                        discoveredAnime = if (append) it.discoveredAnime + animeList else animeList,
                        animePage = page,
                        animeHasMore = hasNextPage,
                        isLoadingAnime = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingAnime = false, animeError = "Failed to fetch anime") }
            }
        }
    }

    private fun fetchMovies(append: Boolean = false) {
        val currentState = _state.value
        if (currentState.isLoadingMovies && !append) return

        screenModelScope.launch {
            _state.update { it.copy(isLoadingMovies = true, moviesError = null) }

            try {
                delay(200)

                val page = if (append) currentState.moviesPage + 1 else 1
                val genre = currentState.selectedGenre

                val query: String
                val variables = JSONObject()

                if (genre == null) {
                    query = """
                        query(${'$'}page: Int) {
                            Page(page: ${'$'}page, perPage: 12) {
                                pageInfo { hasNextPage }
                                media(type: ANIME, format: MOVIE, sort: TRENDING_DESC) {
                                    title { romaji }
                                    coverImage { medium }
                                    siteUrl
                                }
                            }
                        }
                    """.trimIndent()
                    variables.put("page", page)
                } else {
                    query = """
                        query(${'$'}page: Int, ${'$'}genre: String) {
                            Page(page: ${'$'}page, perPage: 12) {
                                pageInfo { hasNextPage }
                                media(type: ANIME, format: MOVIE, genre: ${'$'}genre, sort: SCORE_DESC) {
                                    title { romaji }
                                    coverImage { medium }
                                    siteUrl
                                }
                            }
                        }
                    """.trimIndent()
                    variables.put("page", page)
                    variables.put("genre", genre)
                }

                val json = fetchGraphQL(query, variables) ?: run {
                    _state.update { it.copy(isLoadingMovies = false, moviesError = "Failed to fetch movies") }
                    return@launch
                }

                val pageData = json.optJSONObject("data")?.optJSONObject("Page")
                val pageInfo = pageData?.optJSONObject("pageInfo")
                val hasNextPage = pageInfo?.optBoolean("hasNextPage", false) ?: false
                val mediaArray = pageData?.optJSONArray("media") ?: JSONArray()

                val moviesList = mutableListOf<DiscoveredAnime>()
                for (i in 0 until mediaArray.length()) {
                    val media = mediaArray.optJSONObject(i) ?: continue
                    val titleObj = media.optJSONObject("title") ?: continue
                    val coverObj = media.optJSONObject("coverImage") ?: continue

                    moviesList.add(
                        DiscoveredAnime(
                            title = titleObj.optString("romaji", ""),
                            imageUrl = coverObj.optString("medium", ""),
                            siteUrl = media.optString("siteUrl", ""),
                        ),
                    )
                }

                _state.update {
                    it.copy(
                        discoveredMovies = if (append) it.discoveredMovies + moviesList else moviesList,
                        moviesPage = page,
                        moviesHasMore = hasNextPage,
                        isLoadingMovies = false,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingMovies = false, moviesError = "Failed to fetch movies") }
            }
        }
    }

    fun selectGenre(genre: String?) {
        val currentGenre = _state.value.selectedGenre
        if (currentGenre == genre) return

        _state.update {
            it.copy(
                selectedGenre = genre,
                discoveredAnime = emptyList(),
                discoveredMovies = emptyList(),
                animePage = 1,
                moviesPage = 1,
                animeHasMore = true,
                moviesHasMore = true,
            )
        }
        fetchAnime()
        fetchMovies()
    }

    fun loadMoreAnime() {
        val state = _state.value
        if (state.isLoadingAnime || !state.animeHasMore) return
        fetchAnime(append = true)
    }

    fun loadMoreMovies() {
        val state = _state.value
        if (state.isLoadingMovies || !state.moviesHasMore) return
        fetchMovies(append = true)
    }

    fun retryAnime() {
        fetchAnime()
    }

    fun retryMovies() {
        fetchMovies()
    }

    fun retryGenres() {
        fetchGenresAndTags()
    }
}
