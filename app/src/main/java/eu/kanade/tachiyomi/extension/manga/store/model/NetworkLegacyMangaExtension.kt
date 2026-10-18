package eu.kanade.tachiyomi.extension.manga.store.model

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.extension.manga.store.MangaExtensionStore
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkLegacyMangaExtension(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String,
    val code: Long,
    val version: String,
    val nsfw: Int,
    val sources: List<Source>?,
) {
    @Serializable
    data class Source(
        val id: Long,
        val lang: String,
        val name: String,
        val baseUrl: String,
    )

    fun toAvailableExtension(store: MangaExtensionStore, storeBaseUrl: String): MangaExtension.Available {
        return MangaExtension.Available(
            name = name.substringAfter("Tachiyomi: "),
            pkgName = pkg,
            versionName = version,
            versionCode = code,
            libVersion = version.substringBeforeLast('.').toDouble(),
            lang = lang,
            isNsfw = nsfw == 1,
            sources = if (sources.isNullOrEmpty()) {
                listOf(
                    MangaExtension.Available.MangaSource(
                        id = 0,
                        name = name,
                        lang = lang,
                        baseUrl = "",
                    ),
                )
            } else {
                sources.map { source ->
                    MangaExtension.Available.MangaSource(
                        id = source.id,
                        lang = source.lang,
                        name = source.name,
                        baseUrl = source.baseUrl,
                    )
                }
            },
            apkName = apk,
            iconUrl = "$storeBaseUrl/icon/$pkg.png",
            repoUrl = storeBaseUrl,
            apkUrl = "$storeBaseUrl/apk/$apk",
            store = store,
        )
    }
}
