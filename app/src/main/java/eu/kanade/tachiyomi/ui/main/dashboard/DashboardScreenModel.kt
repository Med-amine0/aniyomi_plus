package eu.kanade.tachiyomi.ui.main.dashboard

import android.util.Log
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
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.manga.interactor.GetLibraryManga
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Immutable
data class Genre(val id: Int, val name: String)

@Immutable
data class DiscoveredAnime(
    val malId: Long,
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
    val genres: List<Genre> = emptyList(),
    val selectedGenreId: Int? = null,
    val discoveredAnime: List<DiscoveredAnime> = emptyList(),
    val discoveredMovies: List<DiscoveredAnime> = emptyList(),
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val discoverError: String? = null,
    val moviesError: String? = null,
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

    init {
        loadData()
        screenModelScope.launch {
            fetchGenres()
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
            } catch (e: Exception) {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private suspend fun fetchUrl(urlString: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val responseCode = connection.responseCode
                if (responseCode != 200) {
                    connection.disconnect()
                    return@withContext null
                }

                connection.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun fetchGenres() {
        try {
            val body = fetchUrl("https://api.jikan.moe/v4/genres/anime") ?: return

            val json = org.json.JSONObject(body)
            val dataArray = json.getJSONArray("data")

            val genreList = mutableListOf<Genre>()
            for (i in 0 until dataArray.length()) {
                val genreObj = dataArray.getJSONObject(i)
                genreList.add(
                    Genre(
                        id = genreObj.getInt("mal_id"),
                        name = genreObj.getString("name"),
                    ),
                )
            }

            _state.update { it.copy(genres = genreList) }
        } catch (e: Exception) {
        }
    }

    private fun fetchAnime(append: Boolean = false) {
        val currentState = _state.value
        if (currentState.isLoadingMore && !append) return

        screenModelScope.launch {
            if (append) {
                _state.update { it.copy(isLoadingMore = true) }
            } else {
                _state.update { it.copy(isLoadingMore = true, discoverError = null) }
            }

            try {
                delay(350)

                val page = if (append) currentState.currentPage + 1 else 1
                val genreId = currentState.selectedGenreId

                val url = if (genreId == null) {
                    "https://api.jikan.moe/v4/top/anime?page=$page&limit=12"
                } else {
                    "https://api.jikan.moe/v4/anime?genres=$genreId&page=$page&limit=12"
                }

                Log.d("Dashboard", "Fetching anime from: $url")

                val body = fetchUrl(url)

                if (body == null) {
                    Log.e("Dashboard", "Failed to fetch anime - body is null")
                    _state.update { it.copy(isLoadingMore = false, discoverError = "Tap to retry") }
                    return@launch
                }

                Log.d("Dashboard", "Got response body length: ${body.length}")

                val json = org.json.JSONObject(body)
                val dataArray = json.getJSONArray("data")
                val pagination = json.getJSONObject("pagination")
                val hasMore = pagination.getBoolean("has_next_page")

                Log.d("Dashboard", "Parsing $dataArray.length items")

                val animeList = mutableListOf<DiscoveredAnime>()
                for (i in 0 until dataArray.length()) {
                    val animeObj = dataArray.getJSONObject(i)
                    val images = animeObj.getJSONObject("images")
                    val jpg = images.getJSONObject("jpg")
                    val title = animeObj.getString("title")
                    val encodedTitle = URLEncoder.encode(title, "UTF-8")

                    animeList.add(
                        DiscoveredAnime(
                            malId = animeObj.getLong("mal_id"),
                            title = title,
                            imageUrl = jpg.getString("image_url"),
                            siteUrl = "https://anilist.co/search/anime?search=$encodedTitle",
                        ),
                    )
                }

                Log.d("Dashboard", "Created ${animeList.size} DiscoveredAnime items")

                _state.update {
                    val newState = it.copy(
                        discoveredAnime = if (append) it.discoveredAnime + animeList else animeList,
                        currentPage = page,
                        hasMorePages = hasMore,
                        isLoadingMore = false,
                        discoverError = null,
                    )
                    Log.d("Dashboard", "State updated - discoveredAnime size: ${newState.discoveredAnime.size}")
                    newState
                }
            } catch (e: Exception) {
                Log.e("Dashboard", "Exception fetching anime", e)
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        discoverError = "Tap to retry",
                    )
                }
            }
        }
    }

    private fun fetchMovies() {
        screenModelScope.launch {
            try {
                delay(350)

                val body = fetchUrl("https://api.jikan.moe/v4/top/anime?type=movie&limit=12")

                if (body == null) {
                    _state.update { it.copy(moviesError = "Failed to load movies") }
                    return@launch
                }

                val json = org.json.JSONObject(body)
                val dataArray = json.getJSONArray("data")

                val moviesList = mutableListOf<DiscoveredAnime>()
                for (i in 0 until dataArray.length()) {
                    val animeObj = dataArray.getJSONObject(i)
                    val images = animeObj.getJSONObject("images")
                    val jpg = images.getJSONObject("jpg")
                    val title = animeObj.getString("title")
                    val encodedTitle = URLEncoder.encode(title, "UTF-8")

                    moviesList.add(
                        DiscoveredAnime(
                            malId = animeObj.getLong("mal_id"),
                            title = title,
                            imageUrl = jpg.getString("image_url"),
                            siteUrl = "https://anilist.co/search/anime?search=$encodedTitle",
                        ),
                    )
                }

                _state.update {
                    it.copy(
                        discoveredMovies = moviesList,
                        moviesError = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(moviesError = "Failed to load movies")
                }
            }
        }
    }

    fun selectGenre(genreId: Int?) {
        if (_state.value.selectedGenreId == genreId) return
        _state.update {
            it.copy(
                selectedGenreId = genreId,
                discoveredAnime = emptyList(),
                currentPage = 1,
                hasMorePages = true,
            )
        }
        fetchAnime()
    }

    fun loadMore() {
        val currentState = _state.value
        if (currentState.isLoadingMore || !currentState.hasMorePages) return
        fetchAnime(append = true)
    }

    fun retryDiscover() {
        fetchAnime()
    }

    fun retryMovies() {
        fetchMovies()
    }
}
