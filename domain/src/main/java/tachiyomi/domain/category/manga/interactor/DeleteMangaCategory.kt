package tachiyomi.domain.category.manga.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.manga.repository.MangaCategoryRepository
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entries.manga.repository.MangaRepository
import tachiyomi.domain.entries.manga.model.MangaUpdate
import tachiyomi.domain.library.service.LibraryPreferences

class DeleteMangaCategory(
    private val categoryRepository: MangaCategoryRepository,
    private val mangaRepository: MangaRepository,
    private val libraryPreferences: LibraryPreferences,
    private val downloadPreferences: DownloadPreferences,
) {

    suspend fun await(categoryId: Long) = withNonCancellableContext {
        val allCategories = categoryRepository.getAllMangaCategories()
        
        fun getAllChildIds(parentId: Long): List<Long> {
            val children = allCategories.filter { it.parentId == parentId }
            return children.flatMap { child ->
                listOf(child.id) + getAllChildIds(child.id)
            }
        }
        
        val categoryIdsToDelete = listOf(categoryId) + getAllChildIds(categoryId)
        
        val allManga = mangaRepository.getLibraryManga()
        
        for (categoryIdToDel in categoryIdsToDelete) {
            try {
                categoryRepository.deleteMangaCategory(categoryIdToDel)
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e)
                return@withNonCancellableContext Result.InternalError(e)
            }
        }
        
        for (manga in allManga) {
            val mangaCategories = categoryRepository.getCategoriesByMangaId(manga.id)
            val remainingCategories = mangaCategories.filterNot { it.id in categoryIdsToDelete }
            
            if (remainingCategories.isEmpty()) {
                try {
                    mangaRepository.updateManga(
                        MangaUpdate(
                            id = manga.id,
                            favorite = false,
                        )
                    )
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e)
                }
            }
        }

        val remainingCategories = categoryRepository.getAllMangaCategories()
        val updates = remainingCategories.mapIndexed { index, category ->
            CategoryUpdate(
                id = category.id,
                order = index.toLong(),
            )
        }

        val defaultCategory = libraryPreferences.defaultMangaCategory().get()
        if (categoryIdsToDelete.any { it.toInt() == defaultCategory }) {
            libraryPreferences.defaultMangaCategory().delete()
        }

        val categoryPreferences = listOf(
            libraryPreferences.mangaUpdateCategories(),
            libraryPreferences.mangaUpdateCategories(),
            downloadPreferences.removeExcludeCategories(),
            downloadPreferences.downloadNewChapterCategories(),
            downloadPreferences.downloadNewChapterCategoriesExclude(),
        )
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            val newIds = ids.filterNot { it.toLong() in categoryIdsToDelete }
            if (newIds.size != ids.size) {
                preference.set(newIds)
            }
        }

        try {
            categoryRepository.updatePartialMangaCategories(updates)
            Result.Success
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            Result.InternalError(e)
        }
    }

    sealed interface Result {
        data object Success : Result
        data class InternalError(val error: Throwable) : Result
    }
}
