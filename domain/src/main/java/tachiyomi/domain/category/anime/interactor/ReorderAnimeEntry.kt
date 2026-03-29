package tachiyomi.domain.category.anime.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.anime.repository.AnimeCategoryRepository

class ReorderAnimeEntry(
    private val categoryRepository: AnimeCategoryRepository,
) {
    suspend fun moveUp(animeId: Long, categoryId: Long): Boolean {
        return try {
            logcat(LogPriority.DEBUG) { "ReorderAnimeEntry.moveUp: animeId=$animeId, categoryId=$categoryId" }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    suspend fun moveDown(animeId: Long, categoryId: Long): Boolean {
        return try {
            logcat(LogPriority.DEBUG) { "ReorderAnimeEntry.moveDown: animeId=$animeId, categoryId=$categoryId" }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
