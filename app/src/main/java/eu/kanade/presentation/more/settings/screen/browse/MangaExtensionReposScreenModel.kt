package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import eu.kanade.tachiyomi.extension.manga.store.MangaExtensionStore
import eu.kanade.tachiyomi.extension.manga.store.MangaExtensionStoreRepository
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import mihon.domain.extensionrepo.manga.interactor.CreateMangaExtensionRepo
import mihon.domain.extensionrepo.manga.interactor.DeleteMangaExtensionRepo
import mihon.domain.extensionrepo.manga.interactor.GetMangaExtensionRepo
import mihon.domain.extensionrepo.manga.interactor.ReplaceMangaExtensionRepo
import mihon.domain.extensionrepo.manga.interactor.UpdateMangaExtensionRepo
import mihon.domain.extensionrepo.model.ExtensionRepo
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaExtensionReposScreenModel(
    private val getExtensionRepo: GetMangaExtensionRepo = Injekt.get(),
    private val createExtensionRepo: CreateMangaExtensionRepo = Injekt.get(),
    private val deleteExtensionRepo: DeleteMangaExtensionRepo = Injekt.get(),
    private val replaceExtensionRepo: ReplaceMangaExtensionRepo = Injekt.get(),
    private val updateExtensionRepo: UpdateMangaExtensionRepo = Injekt.get(),
    private val extensionStoreRepository: MangaExtensionStoreRepository = Injekt.get(),
    private val extensionManager: MangaExtensionManager = Injekt.get(),
) : StateScreenModel<RepoScreenState>(RepoScreenState.Loading) {

    private val _events: Channel<RepoEvent> = Channel(Int.MAX_VALUE)
    val events = _events.receiveAsFlow()

    init {
        screenModelScope.launchIO {
            getExtensionRepo.subscribeAll()
                .collectLatest { repos ->
                    mutableState.update { current ->
                        RepoScreenState.Success(
                            repos = repos.toImmutableSet(),
                            stores = (current as? RepoScreenState.Success)?.stores ?: persistentSetOf(),
                        )
                    }
                }
        }
        // Extension stores (new format) share this screen with the legacy repos.
        screenModelScope.launchIO {
            extensionStoreRepository.getAllAsFlow()
                .collectLatest { stores ->
                    mutableState.update { current ->
                        RepoScreenState.Success(
                            repos = (current as? RepoScreenState.Success)?.repos ?: persistentSetOf(),
                            stores = stores.toImmutableSet(),
                        )
                    }
                }
        }
    }

    /**
     * Creates and adds a new repo to the database.
     *
     * @param baseUrl The baseUrl or index url of the repo to create.
     */
    fun createRepo(baseUrl: String) {
        screenModelScope.launchIO {
            val indexUrl = baseUrl.trim().let {
                if (it.endsWith("/index.min.json") || it.endsWith("/index.pb") || it.endsWith("/repo.json")) {
                    it
                } else {
                    "${it.trimEnd('/')}/index.min.json"
                }
            }

            // New-format stores (repo.json / index.pb) are handled by the store pipeline. Legacy repos have no
            // repo.json, so this fails cleanly for them and they keep using the unchanged flow below.
            if (extensionStoreRepository.insert(indexUrl).isSuccess) {
                extensionManager.findAvailableExtensions()
                return@launchIO
            }

            when (val result = createExtensionRepo.await(indexUrl)) {
                CreateMangaExtensionRepo.Result.InvalidUrl -> _events.send(RepoEvent.InvalidUrl)
                CreateMangaExtensionRepo.Result.RepoAlreadyExists -> _events.send(RepoEvent.RepoAlreadyExists)
                is CreateMangaExtensionRepo.Result.DuplicateFingerprint -> {
                    showDialog(RepoDialog.Conflict(result.oldRepo, result.newRepo))
                }
                else -> {}
            }
        }
    }

    /**
     * Inserts a repo to the database, replace a matching repo with the same signing key fingerprint if found.
     *
     * @param newRepo The repo to insert
     */
    fun replaceRepo(newRepo: ExtensionRepo) {
        screenModelScope.launchIO {
            replaceExtensionRepo.await(newRepo)
        }
    }

    /**
     * Refreshes information for each repository.
     */
    fun refreshRepos() {
        val status = state.value

        if (status is RepoScreenState.Success) {
            screenModelScope.launchIO {
                updateExtensionRepo.awaitAll()
            }
        }
    }

    /**
     * Deletes the given repo from the database
     */
    fun deleteRepo(baseUrl: String) {
        screenModelScope.launchIO {
            deleteExtensionRepo.await(baseUrl)
        }
    }

    /**
     * Deletes the given extension store (new format) from the database
     */
    fun deleteStore(indexUrl: String) {
        screenModelScope.launchIO {
            extensionStoreRepository.remove(indexUrl)
            extensionManager.findAvailableExtensions()
        }
    }

    fun showDialog(dialog: RepoDialog) {
        mutableState.update {
            when (it) {
                RepoScreenState.Loading -> it
                is RepoScreenState.Success -> it.copy(dialog = dialog)
            }
        }
    }

    fun dismissDialog() {
        mutableState.update {
            when (it) {
                RepoScreenState.Loading -> it
                is RepoScreenState.Success -> it.copy(dialog = null)
            }
        }
    }
}

sealed class RepoEvent {
    sealed class LocalizedMessage(val stringRes: StringResource) : RepoEvent()
    data object InvalidUrl : LocalizedMessage(MR.strings.invalid_repo_name)
    data object RepoAlreadyExists : LocalizedMessage(MR.strings.error_repo_exists)
}

sealed class RepoDialog {
    data object Create : RepoDialog()
    data class Delete(val repo: String, val isStore: Boolean = false) : RepoDialog()
    data class Conflict(val oldRepo: ExtensionRepo, val newRepo: ExtensionRepo) : RepoDialog()
    data class Confirm(val url: String) : RepoDialog()
}

sealed class RepoScreenState {

    @Immutable
    data object Loading : RepoScreenState()

    @Immutable
    data class Success(
        val repos: ImmutableSet<ExtensionRepo>,
        val stores: ImmutableSet<MangaExtensionStore> = persistentSetOf(),
        val oldRepos: ImmutableSet<String>? = null,
        val dialog: RepoDialog? = null,
    ) : RepoScreenState() {

        val isEmpty: Boolean
            get() = repos.isEmpty() && stores.isEmpty()
    }
}
