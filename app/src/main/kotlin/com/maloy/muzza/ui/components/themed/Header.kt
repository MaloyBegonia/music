package com.maloy.muzza.ui.components.themed

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maloy.muzza.ui.styling.Dimensions
import com.maloy.muzza.ui.styling.LocalAppearance
import com.maloy.muzza.ui.styling.shimmer
import com.maloy.muzza.utils.medium
import com.maloy.muzza.utils.semiBold
import kotlin.random.Random

@Composable
fun Header(
    title: String,
    modifier: Modifier = Modifier,
    actionsContent: @Composable RowScope.() -> Unit = {},
) {
    val typography = LocalAppearance.current.typography

    Header(
        modifier = modifier,
        titleContent = {
            BasicText(
                text = title,
                style = typography.xxl.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actionsContent = actionsContent
    )
}

@Composable
fun Header(
    modifier: Modifier = Modifier,
    titleContent: @Composable () -> Unit,
    actionsContent: @Composable RowScope.() -> Unit,
) {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(Dimensions.headerHeight)
            .fillMaxWidth()
    ) {
        titleContent()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .heightIn(min = 48.dp),
            content = actionsContent,
        )
    }
}

@Composable
fun HeaderPlaceholder(
    modifier: Modifier = Modifier,
) {
    val (colorPalette, typography) = LocalAppearance.current

    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .height(Dimensions.headerHeight)
            .fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(colorPalette.shimmer)
                .fillMaxWidth(remember { 0.25f + Random.nextFloat() * 0.5f })
        ) {
            BasicText(
                text = "",
                style = typography.xxl.medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HeaderInfo (
    title: String,
    icon: Painter,
    spacer: Int
) {
    val (colorPalette, typography, thumbnailShape) = LocalAppearance.current
    Image(
        painter = icon,
        contentDescription = null,
        colorFilter = ColorFilter.tint(colorPalette.text),
        modifier = Modifier
            .size(14.dp)
    )
    BasicText(
        text = title,
        style = typography.xxs.semiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )

    Spacer(
        modifier = Modifier
            .width(spacer.dp)
    )
}
