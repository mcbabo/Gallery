/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.albums

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.pinchzoomgrid.PinchZoomGridLayout
import com.dokar.pinchzoomgrid.rememberPinchZoomGridState
import com.dot.gallery.R
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.albumCellsList
import com.dot.gallery.core.LocalEventHandler
import com.dot.gallery.core.LocalMediaDistributor
import com.dot.gallery.core.Settings
import com.dot.gallery.core.Settings.Album.rememberAlbumGridSize
import com.dot.gallery.core.Settings.Album.rememberLastSort
import com.dot.gallery.core.Settings.Album.rememberLastViewType
import com.dot.gallery.core.presentation.components.EmptyAlbum
import com.dot.gallery.core.presentation.components.Error
import com.dot.gallery.core.presentation.components.FilterButton
import com.dot.gallery.core.presentation.components.FilterKind
import com.dot.gallery.core.presentation.components.FilterOption
import com.dot.gallery.core.presentation.components.LoadingAlbum
import com.dot.gallery.feature_node.domain.model.Album
import com.dot.gallery.feature_node.domain.model.MediaState
import com.dot.gallery.feature_node.domain.util.MediaOrder
import com.dot.gallery.feature_node.presentation.albums.components.AlbumComponent
import com.dot.gallery.feature_node.presentation.albums.components.AlbumRowComponent
import com.dot.gallery.feature_node.presentation.albums.components.CarouselPinnedAlbums
import com.dot.gallery.feature_node.presentation.search.MainSearchBar
import com.dot.gallery.feature_node.presentation.timeline.components.TimelineNavActions
import com.dot.gallery.feature_node.presentation.util.LocalHazeState
import com.dot.gallery.feature_node.presentation.util.mediaSharedElement
import com.dot.gallery.feature_node.presentation.util.rememberActivityResult
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun AlbumsScreen(
    paddingValues: PaddingValues,
    filterOptions: SnapshotStateList<FilterOption>,
    isScrolling: MutableState<Boolean>,
    onAlbumClick: (Album) -> Unit,
    onAlbumLongClick: (Album) -> Unit,
    onMoveAlbumToTrash: (ActivityResultLauncher<IntentSenderRequest>, Album) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
) {
    val eventHandler = LocalEventHandler.current
    val distributor = LocalMediaDistributor.current
    val mediaState = distributor.timelineMediaFlow.collectAsStateWithLifecycle(
        context = Dispatchers.IO,
        initialValue = MediaState()
    )
    val albumsState = distributor.albumsFlow.collectAsStateWithLifecycle()

    var lastCellIndex by rememberAlbumGridSize()

    val pinchState = rememberPinchZoomGridState(
        cellsList = albumCellsList,
        initialCellsIndex = lastCellIndex
    )
    val listState = rememberLazyListState()
    var viewType by rememberLastViewType()

    LaunchedEffect(pinchState.isZooming) {
        lastCellIndex = albumCellsList.indexOf(pinchState.currentCells)
    }
    val lastSort by rememberLastSort()
    LaunchedEffect(lastSort) {
        val selectedFilter = filterOptions.first { it.filterKind == lastSort.kind }
        selectedFilter.onClick(
            when (selectedFilter.filterKind) {
                FilterKind.DATE -> MediaOrder.Date(lastSort.orderType)
                FilterKind.NAME -> MediaOrder.Label(lastSort.orderType)
            }
        )
    }

    Scaffold(
        topBar = {
            MainSearchBar(
                isScrolling = isScrolling,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope,
            ) {
                TimelineNavActions()
            }
        }
    ) { innerPaddingValues ->
        when (viewType) {
            Settings.Album.ViewType.GRID -> {
                with(sharedTransitionScope) {
                    PinchZoomGridLayout(
                        state = pinchState,
                        modifier = Modifier.hazeSource(LocalHazeState.current)
                    ) {
                        LaunchedEffect(gridState.isScrollInProgress) {
                            isScrolling.value = gridState.isScrollInProgress
                        }
                        LazyVerticalGrid(
                            state = gridState,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxSize(),
                            columns = gridCells,
                            contentPadding = PaddingValues(
                                top = innerPaddingValues.calculateTopPadding(),
                                bottom = innerPaddingValues.calculateBottomPadding() + 16.dp + 64.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "pinnedAlbums"
                            ) {
                                AnimatedVisibility(
                                    visible = albumsState.value.albumsPinned.isNotEmpty(),
                                    enter = enterAnimation,
                                    exit = exitAnimation
                                ) {
                                    Column {
                                        Text(
                                            modifier = Modifier
                                                .pinchItem(key = "pinnedAlbums")
                                                .padding(horizontal = 8.dp)
                                                .padding(vertical = 24.dp),
                                            text = stringResource(R.string.pinned_albums_title),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        CarouselPinnedAlbums(
                                            albumList = albumsState.value.albumsPinned,
                                            onAlbumClick = onAlbumClick,
                                            onAlbumLongClick = onAlbumLongClick
                                        )
                                    }
                                }
                            }
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "filterButton"
                            ) {
                                AnimatedVisibility(
                                    visible = albumsState.value.albumsUnpinned.isNotEmpty(),
                                    enter = enterAnimation,
                                    exit = exitAnimation
                                ) {
                                    FilterButton(
                                        modifier = Modifier.pinchItem(key = "filterButton"),
                                        filterOptions = filterOptions.toTypedArray()
                                    )
                                }
                            }
                            items(
                                items = albumsState.value.albumsUnpinned,
                                key = { item -> item.toString() }
                            ) { item ->
                                val trashResult = rememberActivityResult()
                                with(sharedTransitionScope) {
                                    AlbumComponent(
                                        modifier = Modifier
                                            .pinchItem(key = item.toString())
                                            .animateItem(),
                                        thumbnailModifier = Modifier
                                            .mediaSharedElement(
                                                album = item,
                                                animatedVisibilityScope = animatedContentScope
                                            ),
                                        album = item,
                                        onItemClick = onAlbumClick,
                                        onTogglePinClick = onAlbumLongClick,
                                        onMoveAlbumToTrash = {
                                            onMoveAlbumToTrash(trashResult, it)
                                        }
                                    )
                                }
                            }

                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "albumDetails"
                            ) {
                                AnimatedVisibility(
                                    visible = mediaState.value.media.isNotEmpty() && albumsState.value.albums.isNotEmpty(),
                                    enter = enterAnimation,
                                    exit = exitAnimation
                                ) {
                                    Text(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pinchItem(key = "albumDetails")
                                            .padding(horizontal = 8.dp)
                                            .padding(vertical = 24.dp),
                                        text = stringResource(
                                            R.string.images_videos,
                                            mediaState.value.media.size
                                        ),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "emptyAlbums"
                            ) {
                                AnimatedVisibility(
                                    visible = albumsState.value.albums.isEmpty() && albumsState.value.error.isEmpty(),
                                    enter = enterAnimation,
                                    exit = exitAnimation
                                ) {
                                    EmptyAlbum()
                                }
                            }

                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "loadingAlbums"
                            ) {
                                AnimatedVisibility(
                                    visible = albumsState.value.isLoading,
                                    enter = enterAnimation,
                                    exit = exitAnimation
                                ) {
                                    LoadingAlbum()
                                }
                            }
                        }
                    }
                }
            }

            Settings.Album.ViewType.LIST -> {
                with(sharedTransitionScope) {
                    LaunchedEffect(listState.isScrollInProgress) {
                        isScrolling.value = listState.isScrollInProgress
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .fillMaxSize(),
                        contentPadding = innerPaddingValues,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item("pinnedAlbums") {
                            AnimatedVisibility(
                                visible = albumsState.value.albumsPinned.isNotEmpty(),
                                enter = enterAnimation,
                                exit = exitAnimation
                            ) {
                                Column {
                                    Text(
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .padding(vertical = 24.dp),
                                        text = stringResource(R.string.pinned_albums_title),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    CarouselPinnedAlbums(
                                        albumList = albumsState.value.albumsPinned,
                                        onAlbumClick = onAlbumClick,
                                        onAlbumLongClick = onAlbumLongClick
                                    )
                                }
                            }
                        }

                        item("filterButton") {
                            AnimatedVisibility(
                                visible = albumsState.value.albumsUnpinned.isNotEmpty(),
                                enter = enterAnimation,
                                exit = exitAnimation
                            ) {
                                FilterButton(
                                    modifier = Modifier,
                                    filterOptions = filterOptions.toTypedArray(),
                                    viewType = viewType,
                                    onViewTypeChange = { viewType = it }
                                )
                            }
                        }

                        items(
                            items = albumsState.value.albumsUnpinned,
                            key = { it.toString() }
                        ) { item ->
                            val trashResult = rememberActivityResult()
                            with(sharedTransitionScope) {
                                AlbumRowComponent(
                                    modifier = Modifier
                                        .animateItem(),
                                    thumbnailModifier = Modifier
                                        .mediaSharedElement(
                                            album = item,
                                            animatedVisibilityScope = animatedContentScope
                                        ),
                                    album = item,
                                    onItemClick = onAlbumClick,
                                    onTogglePinClick = onAlbumLongClick,
                                    onMoveAlbumToTrash = {
                                        onMoveAlbumToTrash(trashResult, it)
                                    }
                                )
                            }
                        }

                        item(key = "albumDetails") {
                            AnimatedVisibility(
                                visible = mediaState.value.media.isNotEmpty() && albumsState.value.albums.isNotEmpty(),
                                enter = enterAnimation,
                                exit = exitAnimation
                            ) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                        .padding(vertical = 24.dp),
                                    text = stringResource(
                                        R.string.images_videos,
                                        mediaState.value.media.size
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        item(key = "emptyAlbums") {
                            AnimatedVisibility(
                                visible = albumsState.value.albums.isEmpty() && albumsState.value.error.isEmpty(),
                                enter = enterAnimation,
                                exit = exitAnimation
                            ) {
                                EmptyAlbum()
                            }
                        }

                        item(key = "loadingAlbums") {
                            AnimatedVisibility(
                                visible = albumsState.value.isLoading,
                                enter = enterAnimation,
                                exit = exitAnimation
                            ) {
                                LoadingAlbum()
                            }
                        }
                    }
                }
            }
        }
    }
    /** Error State Handling Block **/
    AnimatedVisibility(
        visible = albumsState.value.error.isNotEmpty(),
        enter = enterAnimation,
        exit = exitAnimation
    ) {
        Error(errorMessage = albumsState.value.error)
    }
    /** ************ **/
}