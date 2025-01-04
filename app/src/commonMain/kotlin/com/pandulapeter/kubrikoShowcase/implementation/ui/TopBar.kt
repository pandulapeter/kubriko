package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.back
import kubriko.app.generated.resources.ic_back
import kubriko.app.generated.resources.img_logo
import kubriko.app.generated.resources.kubriko_showcase
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun TopBar(
    modifier: Modifier = Modifier,
    shouldUseCompactUi: Boolean,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = Surface(
    modifier = modifier,
    tonalElevation = when (isSystemInDarkTheme()) {
        true -> 4.dp
        false -> 0.dp
    },
    shadowElevation = when (isSystemInDarkTheme()) {
        true -> 4.dp
        false -> 2.dp
    },
) {
    AnimatedVisibility(
        visible = selectedShowcaseEntry == null,
        enter = fadeIn() + slideIn { IntOffset((it.width * 0.5f).roundToInt(), 0) },
        exit = slideOut { IntOffset((it.width * 0.75f).roundToInt(), 0) } + fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                modifier = Modifier.size(144.dp).align(Alignment.BottomEnd),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomEnd,
                painter = painterResource(Res.drawable.img_logo),
                contentDescription = null,
            )
        }
    }
    Crossfade(
        targetState = shouldUseCompactUi,
    ) { shouldUseCompactUi ->
        if (shouldUseCompactUi) {
            Crossfade(
                targetState = selectedShowcaseEntry
            ) { showcaseEntry ->
                Header(
                    modifier = Modifier.fillMaxWidth(),
                    shouldUseCompactUi = shouldUseCompactUi,
                    selectedShowcaseEntry = showcaseEntry,
                    onShowcaseEntrySelected = onShowcaseEntrySelected,
                )
            }
        } else {
            Header(
                modifier = Modifier.fillMaxWidth(),
                shouldUseCompactUi = shouldUseCompactUi,
                selectedShowcaseEntry = selectedShowcaseEntry,
                onShowcaseEntrySelected = onShowcaseEntrySelected,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(
    modifier: Modifier = Modifier,
    shouldUseCompactUi: Boolean,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
) = TopAppBar(
    modifier = modifier,
    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
    title = {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(
                    resource = if (shouldUseCompactUi && selectedShowcaseEntry != null) {
                        selectedShowcaseEntry.titleStringResource
                    } else {
                        Res.string.kubriko_showcase
                    }
                ),
            )
            if (shouldUseCompactUi && selectedShowcaseEntry != null) {
                Text(
                    color = LocalContentColor.current.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.titleSmall,
                    text = stringResource(selectedShowcaseEntry.subtitleStringResource),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    },
    navigationIcon = {
        if (shouldUseCompactUi && selectedShowcaseEntry != null) {
            IconButton(
                onClick = { onShowcaseEntrySelected(null) }
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_back),
                    contentDescription = stringResource(Res.string.back),
                )
            }
        }
    }
)