package com.maloy.muzza.ui.screens.localplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maloy.compose.persist.persist
import com.maloy.innertube.Innertube
import com.maloy.innertube.models.bodies.BrowseBody
import com.maloy.innertube.requests.playlistPage
import com.maloy.compose.reordering.ReorderingLazyColumn
import com.maloy.compose.reordering.animateItemPlacement
import com.maloy.compose.reordering.draggedItem
import com.maloy.compose.reordering.rememberReorderingState
import com.maloy.compose.reordering.reorder
import com.maloy.muzza.Database
import com.maloy.muzza.LocalPlayerAwareWindowInsets
import com.maloy.muzza.LocalPlayerServiceBinder
import com.maloy.muzza.R
import com.maloy.muzza.models.PlaylistWithSongs
import com.maloy.muzza.models.Song
import com.maloy.muzza.models.SongPlaylistMap
import com.maloy.muzza.query
import com.maloy.muzza.transaction
import com.maloy.muzza.ui.components.LocalMenuState
import com.maloy.muzza.ui.components.themed.ConfirmationDialog
import com.maloy.muzza.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.maloy.muzza.ui.components.themed.Header
import com.maloy.muzza.ui.components.themed.HeaderIconButton
import com.maloy.muzza.ui.components.themed.IconButton
import com.maloy.muzza.ui.components.themed.InPlaylistMediaItemMenu
import com.maloy.muzza.ui.components.themed.Menu
import com.maloy.muzza.ui.components.themed.MenuEntry
import com.maloy.muzza.ui.components.themed.SecondaryTextButton
import com.maloy.muzza.ui.components.themed.TextFieldDialog
import com.maloy.muzza.ui.items.SongItem
import com.maloy.muzza.ui.styling.Dimensions
import com.maloy.muzza.ui.styling.LocalAppearance
import com.maloy.muzza.ui.styling.px
import com.maloy.muzza.utils.asMediaItem
import com.maloy.muzza.utils.completed
import com.maloy.muzza.utils.enqueue
import com.maloy.muzza.utils.forcePlayAtIndex
import com.maloy.muzza.utils.forcePlayFromBeginning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LocalPlaylistSongs(
    playlistId: Long,
    onDelete: () -> Unit,
) {
    val (colorPalette) = LocalAppearance.current
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current

    var playlistWithSongs by persist<PlaylistWithSongs?>("localPlaylist/$playlistId/playlistWithSongs")

    LaunchedEffect(Unit) {
        Database.playlistWithSongs(playlistId).filterNotNull().collect { playlistWithSongs = it }
    }

    val lazyListState = rememberLazyListState()

    val reorderingState = rememberReorderingState(
        lazyListState = lazyListState,
        key = playlistWithSongs?.songs ?: emptyList<Any>(),
        onDragEnd = { fromIndex, toIndex ->
            query {
                Database.move(playlistId, fromIndex, toIndex)
            }
        },
        extraItemCount = 1
    )

    var isRenaming by rememberSaveable {
        mutableStateOf(false)
    }

    if (isRenaming) {
        TextFieldDialog(
            hintText = stringResource(R.string.enter_the_playlist_name),
            initialTextInput = playlistWithSongs?.playlist?.name ?: "",
            onDismiss = { isRenaming = false },
            onDone = { text ->
                query {
                    playlistWithSongs?.playlist?.copy(name = text)?.let(Database::update)
                }
            }
        )
    }

    var isDeleting by rememberSaveable {
        mutableStateOf(false)
    }

    if (isDeleting) {
        ConfirmationDialog(
            text = stringResource(R.string.delete_playlist),
            onDismiss = { isDeleting = false },
            onConfirm = {
                query {
                    playlistWithSongs?.playlist?.let(Database::delete)
                }
                onDelete()
            }
        )
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song
    val thumbnailSizePx = thumbnailSizeDp.px

    val rippleIndication = rememberRipple(bounded = false)

    Box {
        ReorderingLazyColumn(
            reorderingState = reorderingState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0
            ) {
                Header(
                    title = playlistWithSongs?.playlist?.name ?: "Unknown",
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                ) {
                    SecondaryTextButton(
                        text = stringResource(R.string.enqueue),
                        enabled = playlistWithSongs?.songs?.isNotEmpty() == true,
                        onClick = {
                            playlistWithSongs?.songs
                                ?.map(Song::asMediaItem)
                                ?.let { mediaItems ->
                                    binder?.player?.enqueue(mediaItems)
                                }
                        }
                    )

                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                    )

                    HeaderIconButton(
                        icon = R.drawable.ellipsis_horizontal,
                        color = colorPalette.text,
                        onClick = {
                            menuState.display {
                                Menu {
                                    playlistWithSongs?.playlist?.browseId?.let { browseId ->
                                        MenuEntry(
                                            icon = R.drawable.sync,
                                            text = stringResource(R.string.sync),
                                            onClick = {
                                                menuState.hide()
                                                transaction {
                                                    runBlocking(Dispatchers.IO) {
                                                        withContext(Dispatchers.IO) {
                                                            Innertube.playlistPage(BrowseBody(browseId = browseId))
                                                                ?.completed()
                                                        }
                                                    }?.getOrNull()?.let { remotePlaylist ->
                                                        Database.clearPlaylist(playlistId)

                                                        remotePlaylist.songsPage
                                                            ?.items
                                                            ?.map(Innertube.SongItem::asMediaItem)
                                                            ?.onEach(Database::insert)
                                                            ?.mapIndexed { position, mediaItem ->
                                                                SongPlaylistMap(
                                                                    songId = mediaItem.mediaId,
                                                                    playlistId = playlistId,
                                                                    position = position
                                                                )
                                                            }?.let(Database::insertSongPlaylistMaps)
                                                    }
                                                }
                                            }
                                        )
                                    }

                                    MenuEntry(
                                        icon = R.drawable.pencil,
                                        text = stringResource(R.string.rename),
                                        onClick = {
                                            menuState.hide()
                                            isRenaming = true
                                        }
                                    )

                                    MenuEntry(
                                        icon = R.drawable.trash,
                                        text = stringResource(R.string.delete),
                                        onClick = {
                                            menuState.hide()
                                            isDeleting = true
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }

            itemsIndexed(
                items = playlistWithSongs?.songs ?: emptyList(),
                key = { _, song -> song.id },
                contentType = { _, song -> song },
            ) { index, song ->
                SongItem(
                    song = song,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    trailingContent = {
                        IconButton(
                            icon = R.drawable.reorder,
                            color = colorPalette.textDisabled,
                            indication = rippleIndication,
                            onClick = {},
                            modifier = Modifier
                                .reorder(reorderingState = reorderingState, index = index)
                                .size(18.dp)
                        )
                    },
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                menuState.display {
                                    InPlaylistMediaItemMenu(
                                        playlistId = playlistId,
                                        positionInPlaylist = index,
                                        song = song,
                                        onDismiss = menuState::hide
                                    )
                                }
                            },
                            onClick = {
                                playlistWithSongs?.songs
                                    ?.map(Song::asMediaItem)
                                    ?.let { mediaItems ->
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(mediaItems, index)
                                    }
                            }
                        )
                        .animateItemPlacement(reorderingState = reorderingState)
                        .draggedItem(reorderingState = reorderingState, index = index)
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyListState = lazyListState,
            iconId = R.drawable.shuffle,
            visible = !reorderingState.isDragging,
            onClick = {
                playlistWithSongs?.songs?.let { songs ->
                    if (songs.isNotEmpty()) {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(
                            songs.shuffled().map(Song::asMediaItem)
                        )
                    }
                }
            }
        )
    }
}
