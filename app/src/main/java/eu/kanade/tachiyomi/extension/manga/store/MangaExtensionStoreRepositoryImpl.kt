package eu.kanade.tachiyomi.extension.manga.store

import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.handlers.manga.MangaDatabaseHandler

class MangaExtensionStoreRepositoryImpl(
    private val service: MangaExtensionStoreService,
    private val handler: MangaDatabaseHandler,
) : MangaExtensionStoreRepository {
    override suspend fun insert(indexUrl: String): Result<Unit> {
        return service.fetch(indexUrl).mapCatching { upsert(it) }
    }

    override suspend fun insertFromPreference(indexUrl: String, name: String) {
        handler.await {
            extension_storeQueries.upsert(
                indexUrl = indexUrl,
                name = name,
                badgeLabel = name,
                signingKey = "NO_SIGNING_KEY",
                contactWebsite = indexUrl,
                contactDiscord = null,
                isLegacy = false,
                extensionListUrl = null,
            )
        }
    }

    override suspend fun refreshAll() {
        try {
            handler.awaitList { extension_storeQueries.getAll() }.forEach { store ->
                service.fetch(store.index_url)
                    .mapCatching { upsert(it) }
                    .onFailure {
                        logcat(LogPriority.ERROR, it) {
                            "Failed to refresh extension store '${store.name} (${store.index_url})'"
                        }
                    }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    private suspend fun upsert(store: MangaExtensionStore) {
        handler.await {
            extension_storeQueries.upsert(
                indexUrl = store.indexUrl,
                name = store.name,
                badgeLabel = store.badgeLabel,
                signingKey = store.signingKey,
                contactWebsite = store.contact.website,
                contactDiscord = store.contact.discord,
                isLegacy = store.isLegacy,
                extensionListUrl = store.extensionListUrl,
            )
        }
    }

    override suspend fun fetchExtensions(): List<MangaExtension.Available> {
        return try {
            supervisorScope {
                handler.awaitList { extension_storeQueries.getAll(::extensionStoreMapper) }.map { store ->
                    async {
                        service.getExtensions(store).onFailure {
                            logcat(LogPriority.ERROR, it) {
                                "Failed to fetch extensions for store '${store.name} (${store.indexUrl})'"
                            }
                        }
                    }
                }
                    .awaitAll()
                    .flatMap { it.getOrDefault(emptyList()) }
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            emptyList()
        }
    }

    override suspend fun getAll(): List<MangaExtensionStore> {
        return handler.awaitList { extension_storeQueries.getAll(::extensionStoreMapper) }
    }

    override fun getAllAsFlow(): Flow<List<MangaExtensionStore>> {
        return handler.subscribeToList { extension_storeQueries.getAll(::extensionStoreMapper) }
    }

    override fun getCountAsFlow(): Flow<Long> {
        return handler.subscribeToOne { extension_storeQueries.getCount() }
    }

    override suspend fun remove(indexUrl: String) {
        return handler.await { extension_storeQueries.delete(indexUrl) }
    }

    private fun extensionStoreMapper(
        indexUrl: String,
        name: String,
        badgeLabel: String,
        signingKey: String,
        contactWebsite: String,
        contactDiscord: String?,
        isLegacy: Boolean,
        extensionListUrl: String?,
    ): MangaExtensionStore = MangaExtensionStore(
        indexUrl = indexUrl,
        name = name,
        badgeLabel = badgeLabel,
        signingKey = signingKey,
        contact = MangaExtensionStore.Contact(
            website = contactWebsite,
            discord = contactDiscord,
        ),
        isLegacy = isLegacy,
        extensionListUrl = extensionListUrl,
    )
}
