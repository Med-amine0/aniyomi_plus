package eu.kanade.tachiyomi.ui.main.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import tachiyomi.domain.entries.anime.model.AnimeCover
import tachiyomi.domain.entries.manga.model.MangaCover
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import tachiyomi.i18n.MR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

private val AnimeAccent = Color(0xFF3B82F6)
private val MangaAccent = Color(0xFFF97316)
private val BackgroundColor = Color(0xFF1A1A1A)
private val SurfaceColor = Color(0xFF121212)

data object TrackerDashboardTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(AYMR.strings.label_dach),
            icon = painterResource(R.drawable.ic_dach_24dp),
        )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { TrackerDashboardScreenModel() }
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { screenModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundColor)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            when {
                state.isLoading -> LoadingScreen()
                else -> {
                    DashboardContent(
                        state = state,
                        onAnimeMangaToggle = { screenModel.toggleAnimeMangaView() },
                        onShowAllToggle = { screenModel.toggleShowAll() },
                        onAnimeClick = { navigator.push(AnimeScreen(it)) },
                        onMangaClick = { navigator.push(MangaScreen(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: TrackerDashboardState,
    onAnimeMangaToggle: () -> Unit,
    onShowAllToggle: () -> Unit,
    onAnimeClick: (Long) -> Unit,
    onMangaClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentPadding = PaddingValues(horizontal = 16.dp, bottom = 80.dp),
    ) {
        item {
            DachToggle(
                animeView = state.animeView,
                onToggle = onAnimeMangaToggle,
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        if (state.animeView) {
            item { SectionTitle(stringResource(AYMR.strings.recently_viewed)) }

            item {
                if (state.recentAnime.isNotEmpty()) {
                    RecentRow(
                        items = state.recentAnime,
                        getOverlayText = state::getOverlayText,
                        onItemClick = onAnimeClick,
                    )
                } else {
                    EmptySection()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(stringResource(AYMR.strings.in_progress))
            }

            item {
                if (state.inProgressAnime.isNotEmpty()) {
                    InProgressRow(
                        items = state.inProgressAnime.take(5),
                        onItemClick = onAnimeClick,
                    )
                } else {
                    EmptySection()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(stringResource(MR.strings.completed))
            }

            item {
                if (state.completedAnime.isNotEmpty()) {
                    CompletedSection(
                        items = state.completedAnime,
                        onItemClick = onAnimeClick,
                    )
                } else {
                    EmptySection()
                }
            }
        } else {
            item { SectionTitle(stringResource(AYMR.strings.recently_viewed)) }

            item {
                if (state.recentManga.isNotEmpty()) {
                    RecentMangaRow(
                        items = state.recentManga,
                        getOverlayText = state::getOverlayText,
                        onItemClick = onMangaClick,
                    )
                } else {
                    EmptySection()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(stringResource(AYMR.strings.in_progress))
            }

            item {
                if (state.inProgressManga.isNotEmpty()) {
                    InProgressMangaRow(
                        items = state.inProgressManga.take(5),
                        onItemClick = onMangaClick,
                    )
                } else {
                    EmptySection()
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(stringResource(MR.strings.completed))
            }

            item {
                if (state.completedManga.isNotEmpty()) {
                    CompletedMangaSection(
                        items = state.completedManga,
                        onItemClick = onMangaClick,
                    )
                } else {
                    EmptySection()
                }
            }
        }
    }
}

@Composable
private fun DachToggle(
    animeView: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToggleButton(
            text = "ANIME",
            selected = animeView,
            onClick = if (!animeView) onToggle else null,
            modifier = Modifier.weight(1f),
        )
        ToggleButton(
            text = "MANGA",
            selected = !animeView,
            onClick = if (animeView) onToggle else null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToggleButton(
    text: String,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (text == "ANIME") AnimeAccent else MangaAccent

    Button(
        onClick = { onClick?.invoke() },
        modifier = modifier,
        enabled = onClick != null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) accentColor else SurfaceColor,
            contentColor = if (selected) Color.White else Color(0xFF888888),
            disabledContainerColor = accentColor,
            disabledContentColor = Color.White,
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFF888888),
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
private fun EmptySection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No items",
            color = Color(0xFF888888),
        )
    }
}

@Composable
private fun RecentRow(
    items: List<LibraryAnime>,
    getOverlayText: (LibraryAnime) -> String,
    onItemClick: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            val anime = item.anime
            RecentCard(
                coverData = AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    url = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                ),
                overlayText = getOverlayText(item),
                onClick = { onItemClick(item.id) },
            )
        }
    }
}

@Composable
private fun RecentMangaRow(
    items: List<LibraryManga>,
    getOverlayText: (LibraryManga) -> String,
    onItemClick: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            val manga = item.manga
            RecentMangaCard(
                coverData = MangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified,
                ),
                overlayText = getOverlayText(item),
                onClick = { onItemClick(item.id) },
            )
        }
    }
}

@Composable
private fun RecentCard(
    coverData: AnimeCover,
    overlayText: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .aspectRatio(2f / 3f),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coverData,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(40.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = overlayText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RecentMangaCard(
    coverData: MangaCover,
    overlayText: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .aspectRatio(2f / 3f),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coverData,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(40.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = overlayText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun InProgressRow(
    items: List<LibraryAnime>,
    onItemClick: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            val anime = item.anime
            InProgressThumbnail(
                coverData = AnimeCover(
                    animeId = anime.id,
                    sourceId = anime.source,
                    isAnimeFavorite = anime.favorite,
                    url = anime.thumbnailUrl,
                    lastModified = anime.coverLastModified,
                ),
                progress = if (item.totalCount > 0) {
                    item.seenCount.toFloat() / item.totalCount.toFloat()
                } else {
                    0f
                },
                onClick = { onItemClick(item.id) },
            )
        }
    }
}

@Composable
private fun InProgressMangaRow(
    items: List<LibraryManga>,
    onItemClick: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            val manga = item.manga
            InProgressMangaThumbnail(
                coverData = MangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified,
                ),
                progress = if (item.totalChapters > 0) {
                    item.readCount.toFloat() / item.totalChapters.toFloat()
                } else {
                    0f
                },
                onClick = { onItemClick(item.id) },
            )
        }
    }
}

@Composable
private fun InProgressThumbnail(
    coverData: AnimeCover,
    progress: Float,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .aspectRatio(2f / 3f),
        shape = RoundedCornerShape(6.dp),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coverData,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(20.dp)
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun InProgressMangaThumbnail(
    coverData: MangaCover,
    progress: Float,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .width(80.dp)
            .aspectRatio(2f / 3f),
        shape = RoundedCornerShape(6.dp),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = coverData,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(20.dp)
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun CompletedSection(
    items: List<LibraryAnime>,
    onItemClick: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
    ) {
        Column {
            items.take(10).forEachIndexed { index, item ->
                CompletedRow(
                    title = item.anime.title,
                    subtitle = "${item.totalCount}/${item.totalCount}",
                    accentColor = AnimeAccent,
                    onClick = { onItemClick(item.id) },
                )
                if (index < items.size - 1 && index < 9) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = Color(0xFF333333),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedMangaSection(
    items: List<LibraryManga>,
    onItemClick: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
    ) {
        Column {
            items.take(10).forEachIndexed { index, item ->
                CompletedRow(
                    title = item.manga.title,
                    subtitle = "READ",
                    accentColor = MangaAccent,
                    onClick = { onItemClick(item.id) },
                )
                if (index < items.size - 1 && index < 9) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = Color(0xFF333333),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedRow(
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "✓",
                color = accentColor,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
