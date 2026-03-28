package eu.kanade.tachiyomi.ui.category.manga

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastMap
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import eu.kanade.presentation.category.MangaCategoryScreen
import eu.kanade.presentation.category.components.CategoryCreateDialog
import eu.kanade.presentation.category.components.CategoryDeleteDialog
import eu.kanade.presentation.category.components.CategoryRenameDialog
import eu.kanade.presentation.components.TabContent
import kotlinx.collections.immutable.toImmutableList
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun Screen.mangaCategoryTab(): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = rememberScreenModel { MangaCategoryScreenModel() }

    val state by screenModel.state.collectAsState()

    var navigationStack by remember { mutableStateOf(listOf<Long?>(null)) }
    val currentParentId = navigationStack.last()

    return TabContent(
        titleRes = AYMR.strings.label_manga,
        searchEnabled = false,
        content = { contentPadding, _ ->
            if (state is MangaCategoryScreenState.Loading) {
                LoadingScreen()
            } else {
                val successState = state as MangaCategoryScreenState.Success
                val currentCategories = successState.categories.filter { it.parentId == currentParentId }
                val currentState = successState.copy(categories = currentCategories.toImmutableList())

                Column {
                    if (navigationStack.size > 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navigationStack = navigationStack.dropLast(1) }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Text(text = "Subcategories")
                        }
                    }

                    MangaCategoryScreen(
                        state = currentState,
                        onClickCategory = { navigationStack = navigationStack + it.id },
                        onClickCreate = { screenModel.showDialog(MangaCategoryDialog.Create(currentParentId)) },
                        onClickRename = { screenModel.showDialog(MangaCategoryDialog.Rename(it)) },
                        onClickHide = screenModel::hideCategory,
                        onClickDelete = { screenModel.showDialog(MangaCategoryDialog.Delete(it)) },
                        onChangeOrder = screenModel::changeOrder,
                    )
                }

                when (val dialog = successState.dialog) {
                    null -> {}
                    is MangaCategoryDialog.Create -> {
                        CategoryCreateDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onCreate = { screenModel.createCategory(it, dialog.parentId) },
                            categories = successState.categories.fastMap { it.name }.toImmutableList(),
                        )
                    }
                    is MangaCategoryDialog.Rename -> {
                        CategoryRenameDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onRename = { screenModel.renameCategory(dialog.category, it) },
                            categories = successState.categories.fastMap { it.name }.toImmutableList(),
                            category = dialog.category.name,
                        )
                    }
                    is MangaCategoryDialog.Delete -> {
                        CategoryDeleteDialog(
                            onDismissRequest = screenModel::dismissDialog,
                            onDelete = { screenModel.deleteCategory(dialog.category.id) },
                            category = dialog.category.name,
                        )
                    }
                }
            }
        },
        navigateUp = navigator::pop,
    )
}
