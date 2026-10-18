package eu.kanade.tachiyomi.extension.manga.store.model

import android.annotation.SuppressLint
import eu.kanade.tachiyomi.extension.manga.store.MangaExtensionStore
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NetworkLegacyMangaExtensionRepo(
    @SerialName("index_v2")
    val indexV2: String? = null,
    val meta: Meta,
) : BaseNetworkMangaExtensionStore {
    @Serializable
    data class Meta(
        val name: String,
        val shortName: String? = null,
        val website: String,
        val signingKeyFingerprint: String,
    )

    override fun toExtensionStore(indexUrl: String): MangaExtensionStore {
        return MangaExtensionStore(
            indexUrl = indexUrl,
            name = meta.name,
            badgeLabel = meta.shortName ?: meta.name,
            signingKey = meta.signingKeyFingerprint,
            contact = MangaExtensionStore.Contact(
                website = meta.website,
                discord = null,
            ),
            isLegacy = true,
            extensionListUrl = null,
        )
    }
}
