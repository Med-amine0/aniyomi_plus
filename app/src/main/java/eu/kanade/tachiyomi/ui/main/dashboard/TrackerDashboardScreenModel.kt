package eu.kanade.tachiyomi.ui.main.dashboard

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.entries.anime.interactor.GetLibraryAnime
import tachiyomi.domain.entries.manga.interactor.GetLibraryManga
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Immutable
data class TrackerDashboardState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val animeView: Boolean = true,
    val showAllExpanded: Boolean = false,
    val allAnime: List<LibraryAnime> = emptyList(),
    val allManga: List<LibraryManga> = emptyList(),
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

    fun getProgressText(libraryAnime: LibraryAnime): String =
        "Ep ${libraryAnime.seenCount}/${libraryAnime.totalCount}"

    fun getProgressText(libraryManga: LibraryManga): String =
        "Ch. ${libraryManga.readCount}"

    fun getOverlayText(libraryAnime: LibraryAnime): String {
        val percent = (getProgressPercent(libraryAnime) * 100).toInt()
        return "$percent% • ${getProgressText(libraryAnime)}"
    }

    fun getOverlayText(libraryManga: LibraryManga): String {
        val percent = (getProgressPercent(libraryManga) * 100).toInt()
        return "$percent% • ${getProgressText(libraryManga)}"
    }
}

class TrackerDashboardScreenModel : ScreenModel {

    private val getLibraryAnime: GetLibraryAnime = Injekt.get()
    private val getLibraryManga: GetLibraryManga = Injekt.get()

    private val _state = MutableStateFlow(TrackerDashboardState())
    val state: StateFlow<TrackerDashboardState> = _state.asStateFlow()

    init {
        loadData()
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
}
