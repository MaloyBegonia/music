package com.maloy.muzza.ui.screens.home

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.maloy.compose.persist.persistList
import com.maloy.muzza.Database
import com.maloy.muzza.LocalPlayerAwareWindowInsets
import com.maloy.muzza.R
import com.maloy.muzza.enums.ArtistSortBy
import com.maloy.muzza.enums.SortOrder
import com.maloy.muzza.models.Artist
import com.maloy.muzza.ui.components.themed.FloatingActionsContainerWithScrollToTop
import com.maloy.muzza.ui.components.themed.Header
import com.maloy.muzza.ui.components.themed.HeaderIconButton
import com.maloy.muzza.ui.components.themed.HeaderInfo
import com.maloy.muzza.ui.items.ArtistItem
import com.maloy.muzza.ui.styling.Dimensions
import com.maloy.muzza.ui.styling.LocalAppearance
import com.maloy.muzza.ui.styling.px
import com.maloy.muzza.utils.artistSortByKey
import com.maloy.muzza.utils.artistSortOrderKey
import com.maloy.muzza.utils.rememberPreference

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@Composable
fun HomeArtistList(
    onArtistClick: (Artist) -> Unit,
    onSearchClick: () -> Unit,
) {
    val (colorPalette) = LocalAppearance.current

    var sortBy by rememberPreference(artistSortByKey, ArtistSortBy.DateAdded)
    var sortOrder by rememberPreference(artistSortOrderKey, SortOrder.Descending)

    var items by persistList<Artist>("home/artists")

    LaunchedEffect(sortBy, sortOrder) {
        Database.artists(sortBy, sortOrder).collect { items = it }
    }

    val thumbnailSizeDp = Dimensions.thumbnails.song * 2
    val thumbnailSizePx = thumbnailSizeDp.px

    val sortOrderIconRotation by animateFloatAsState(
        targetValue = if (sortOrder == SortOrder.Ascending) 0f else 180f,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing)
    )

    val lazyGridState = rememberLazyGridState()

    Box {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(Dimensions.thumbnails.song * 2 + Dimensions.itemsVerticalPadding * 2),
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Vertical + WindowInsetsSides.End).asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(Dimensions.itemsVerticalPadding * 2),
            horizontalArrangement = Arrangement.spacedBy(
                space = Dimensions.itemsVerticalPadding * 2,
                alignment = Alignment.CenterHorizontally
            ),
            modifier = Modifier
                .background(colorPalette.background0)
                .fillMaxSize()
        ) {
            item(
                key = "header",
                contentType = 0,
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Header(title = stringResource(R.string.artists)) {
                    HeaderInfo(
                        title = "${items.size}",
                        icon = painterResource(R.drawable.person),
                        spacer = 0
                    )
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                    )
                    HeaderIconButton(
                        icon = R.drawable.text,
                        color = if (sortBy == ArtistSortBy.Name) colorPalette.text else colorPalette.textDisabled,
                        onClick = { sortBy = ArtistSortBy.Name }
                    )

                    HeaderIconButton(
                        icon = R.drawable.time,
                        color = if (sortBy == ArtistSortBy.DateAdded) colorPalette.text else colorPalette.textDisabled,
                        onClick = { sortBy = ArtistSortBy.DateAdded }
                    )

                    Spacer(
                        modifier = Modifier
                            .width(2.dp)
                    )

                    HeaderIconButton(
                        icon = R.drawable.arrow_up,
                        color = colorPalette.text,
                        onClick = { sortOrder = !sortOrder },
                        modifier = Modifier
                            .graphicsLayer { rotationZ = sortOrderIconRotation }
                    )
                }
            }

            items(items = items, key = Artist::id) { artist ->
                ArtistItem(
                    artist = artist,
                    thumbnailSizePx = thumbnailSizePx,
                    thumbnailSizeDp = thumbnailSizeDp,
                    alternative = true,
                    modifier = Modifier
                        .clickable(onClick = { onArtistClick(artist) })
                        .animateItemPlacement()
                )
            }
        }

        FloatingActionsContainerWithScrollToTop(
            lazyGridState = lazyGridState,
            iconId = R.drawable.search,
            onClick = onSearchClick
        )
    }
}
