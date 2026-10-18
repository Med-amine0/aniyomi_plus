package eu.kanade.tachiyomi.extension.manga.store.model

import eu.kanade.tachiyomi.extension.manga.store.MangaExtensionStore

interface BaseNetworkMangaExtensionStore {
    fun toExtensionStore(indexUrl: String): MangaExtensionStore
}
