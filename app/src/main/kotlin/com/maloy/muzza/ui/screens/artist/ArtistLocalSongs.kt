package com.maloy.muzza.ui.screens.artist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.maloy.compose.persist.persist
import com.maloy.muzza.Database
import com.maloy.muzza.LocalPlayerAwareWindowInsets
import com.maloy.muzza.LocalPlayerServiceBinder
import com.maloy.muzza.R
import com.maloy.muzza.models.Song
import com.maloy.muzza.ui.components.LocalMenuState
import com.maloy.muzza.ui.components.ShimmerHost
import com.maloy.muzza.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.maloy.muzza.ui.components.themed.LayoutWithAdaptiveThumbnail
import com.maloy.muzza.ui.components.themed.NonQueuedMediaItemMenu
import com.maloy.muzza.ui.components.themed.SecondaryTextButton
import com.maloy.muzza.ui.items.SongItem
import com.maloy.muzza.ui.items.SongItemPlaceholder
import com.maloy.muzza.ui.styling.Dimensions
import com.maloy.muzza.ui.styling.LocalAppearance
import com.maloy.muzza.ui.styling.px
import com.maloy.muzza.utils.asMediaItem
import com.maloy.muzza.utils.enqueue
import com.maloy.muzza.utils.forcePlayAtIndex
import com.maloy.muzza.utils.forcePlayFromBeginning

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun ArtistLocalSongs(
    browseId: String,
    headerContent: @Composable (textButton: (@Composable () -> Unit)?) -> Unit,
    thumbnailContent: @Composable () -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val (colorPalette) = LocalAppearance.current
    val menuState = LocalMenuState.current

    var songs by persist<List<Song>?>("artist/$browseId/localSongs")

    LaunchedEffect(Unit) {
        Database.artistSongs(browseId).collect { songs = it }
    }

    val songThumbnailSizeDp = Dimensions.thumbnails.song
    val songThumbnailSizePx = songThumbnailSizeDp.px

    val lazyListState = rememberLazyListState()

    LayoutWithAdaptiveThumbnail(thumbnailContent = thumbnailContent) {
        Box {
            LazyColumn(
                state = lazyListState,
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        headerContent {
                            SecondaryTextButton(
                                text = stringResource(R.string.enqueue),
                                enabled = !songs.isNullOrEmpty(),
                                onClick = {
                                    binder?.player?.enqueue(songs!!.map(Song::asMediaItem))
                                }
                            )
                        }

                        thumbnailContent()
                    }
                }

                songs?.let { songs ->
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        SongItem(
                            song = song,
                            thumbnailSizeDp = songThumbnailSizeDp,
                            thumbnailSizePx = songThumbnailSizePx,
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        menuState.display {
                                            NonQueuedMediaItemMenu(
                                                onDismiss = menuState::hide,
                                                mediaItem = song.asMediaItem,
                                            )
                                        }
                                    },
                                    onClick = {
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(
                                            songs.map(Song::asMediaItem),
                                            index
                                        )
                                    }
                                )
                        )
                    }
                } ?: item(key = "loading") {
                    ShimmerHost {
                        repeat(4) {
                            SongItemPlaceholder(thumbnailSizeDp = Dimensions.thumbnails.song)
                        }
                    }
                }
            }

            FloatingActionsContainerWithScrollToTop(
                lazyListState = lazyListState,
                iconId = R.drawable.shuffle,
                onClick = {
                    songs?.let { songs ->
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
}
