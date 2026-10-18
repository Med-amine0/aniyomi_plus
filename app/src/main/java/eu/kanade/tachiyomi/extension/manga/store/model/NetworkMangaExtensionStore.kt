package eu.kanade.tachiyomi.extension.manga.store.model

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.extension.manga.model.MangaExtension
import eu.kanade.tachiyomi.extension.manga.store.MangaExtensionStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Protobuf (and JSON) representation of a v2 extension store index, e.g. Keiyoushi's `index.pb`.
 *
 * The field numbers intentionally follow the upstream Mihon schema:
 *  - [Extension.sources] is field 8
 *  - [Source.message] is field 7
 *  - there is no `isTorrent` field.
 *
 * Do not reuse the anime model (`NetworkAnimeExtensionStore`) as it diverges from this schema
 * (`isTorrent@8`, `sources@9`, `message@6`) and would mis-decode Keiyoushi's index.
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkMangaExtensionStore(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val badgeLabel: String,
    @ProtoNumber(3) val signingKey: String,
    @ProtoNumber(4) val contact: Contact,
    @ProtoNumber(101) val extensionList: ExtensionList?,
    @ProtoNumber(102) val extensionListUrl: String?,
) : BaseNetworkMangaExtensionStore {
    @Serializable
    data class Contact(
        @ProtoNumber(1) val website: String,
        @ProtoNumber(2) val discord: String?,
    )

    @Serializable
    data class ExtensionList(@ProtoNumber(1) val extensions: List<Extension>)

    @Serializable
    data class Extension(
        @ProtoNumber(1) val name: String,
        @ProtoNumber(2) val packageName: String,
        @ProtoNumber(3) val resources: Resources,
        @ProtoNumber(4) val extensionLib: String,
        @ProtoNumber(5) val versionCode: Long,
        @ProtoNumber(6) val versionName: String,
        @ProtoNumber(7) val contentWarning: ContentWarning,
        @ProtoNumber(8) val sources: List<Source>,
    )

    @Serializable
    data class Resources(
        @ProtoNumber(1) val apkUrl: String,
        @ProtoNumber(2) val iconUrl: String,
    )

    @Serializable
    data class Source(
        @ProtoNumber(1) val id: Long,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val language: String,
        @ProtoNumber(4) val homeUrl: String = "",
        @ProtoNumber(5) val mirrorUrls: List<String> = emptyList(),
        @ProtoNumber(7) val message: String? = null,
    )

    @Suppress("Unused")
    enum class ContentWarning {
        @ProtoNumber(0)
        @JsonNames("CONTENT_WARNING_UNSPECIFIED")
        UNSPECIFIED,

        @ProtoNumber(1)
        @JsonNames("CONTENT_WARNING_SAFE")
        SAFE,

        @ProtoNumber(2)
        @JsonNames("CONTENT_WARNING_MIXED")
        MIXED,

        @ProtoNumber(3)
        @JsonNames("CONTENT_WARNING_NSFW")
        NSFW,
    }

    override fun toExtensionStore(indexUrl: String): MangaExtensionStore {
        return MangaExtensionStore(
            indexUrl = indexUrl,
            name = name,
            badgeLabel = badgeLabel,
            signingKey = signingKey,
            contact = MangaExtensionStore.Contact(
                website = contact.website,
                discord = contact.discord,
            ),
            isLegacy = false,
            extensionListUrl = extensionListUrl,
        )
    }
}

fun NetworkMangaExtensionStore.ExtensionList.toAvailableExtensions(
    store: MangaExtensionStore,
): List<MangaExtension.Available> {
    return extensions.map { extension ->
        val lang = extension.sources.map { it.language }.toSet()
        MangaExtension.Available(
            name = extension.name,
            pkgName = extension.packageName,
            versionName = extension.versionName,
            versionCode = extension.versionCode,
            libVersion = extension.extensionLib.toDouble(),
            lang = if (lang.size == 1) lang.first() else "all",
            isNsfw = extension.contentWarning >= NetworkMangaExtensionStore.ContentWarning.MIXED,
            sources = extension.sources.map { source ->
                MangaExtension.Available.MangaSource(
                    id = source.id,
                    name = source.name,
                    lang = source.language,
                    baseUrl = source.homeUrl,
                )
            },
            apkName = extension.resources.apkUrl.substringAfterLast('/'),
            iconUrl = extension.resources.iconUrl,
            repoUrl = store.indexUrl,
            apkUrl = extension.resources.apkUrl,
            store = store,
        )
    }
}
