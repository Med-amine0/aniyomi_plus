package eu.kanade.domain.extension.manga.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.manga.store.MangaExtensionStoreRepository
import mihon.domain.extensionrepo.manga.repository.MangaExtensionRepoRepository
import tachiyomi.core.common.preference.getAndSet

class TrustMangaExtension(
    private val mangaExtensionRepoRepository: MangaExtensionRepoRepository,
    private val mangaExtensionStoreRepository: MangaExtensionStoreRepository,
    private val preferences: SourcePreferences,
) {

    suspend fun isTrusted(pkgInfo: PackageInfo, fingerprints: List<String>): Boolean {
        // Signing keys are read from both the legacy extension_repos table and the newer
        // extension_store table, so extensions installed through either path stay trusted.
        val trustedFingerprints = (
            mangaExtensionRepoRepository.getAll().map { it.signingKeyFingerprint } +
                mangaExtensionStoreRepository.getAll().map { it.signingKey }
            ).toHashSet()
        val key = "${pkgInfo.packageName}:${PackageInfoCompat.getLongVersionCode(pkgInfo)}:${fingerprints.last()}"
        return trustedFingerprints.any { fingerprints.contains(it) } || key in preferences.trustedExtensions().get()
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions().getAndSet { exts ->
            // Remove previously trusted versions
            val removed = exts.filterNot { it.startsWith("$pkgName:") }.toMutableSet()

            removed.also { it += "$pkgName:$versionCode:$signatureHash" }
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions().delete()
    }
}
