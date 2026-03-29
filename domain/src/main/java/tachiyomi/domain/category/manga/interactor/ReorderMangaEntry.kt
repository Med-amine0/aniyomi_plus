package tachiyomi.domain.category.manga.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository

class ReorderMangaEntry(
    private val categoryRepository: MangaCategoryRepository,
) {
    suspend fun moveUp(mangaId: Long, categoryId: Long): Boolean {
        return try {
            logcat(LogPriority.DEBUG) { "ReorderMangaEntry.moveUp: mangaId=$mangaId, categoryId=$categoryId" }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    suspend fun moveDown(mangaId: Long, categoryId: Long): Boolean {
        return try {
            logcat(LogPriority.DEBUG) { "ReorderMangaEntry.moveDown: mangaId=$mangaId, categoryId=$categoryId" }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }
}
