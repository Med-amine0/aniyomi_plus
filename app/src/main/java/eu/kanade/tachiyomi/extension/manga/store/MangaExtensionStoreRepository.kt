package eu.kanade.tachiyomi.extension.manga.store

import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import kotlinx.coroutines.flow.Flow

interface MangaExtensionStoreRepository {
    suspend fun insert(indexUrl: String): Result<Unit>

    suspend fun insertFromPreference(indexUrl: String, name: String)

    suspend fun refreshAll()

    suspend fun fetchExtensions(): List<MangaExtension.Available>

    suspend fun getAll(): List<MangaExtensionStore>

    fun getAllAsFlow(): Flow<List<MangaExtensionStore>>

    fun getCountAsFlow(): Flow<Long>

    suspend fun remove(indexUrl: String)
}
