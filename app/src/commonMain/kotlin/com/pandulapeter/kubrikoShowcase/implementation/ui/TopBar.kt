package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.back
import kubriko.app.generated.resources.ic_back
import kubriko.app.generated.resources.kubriko_showcase
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
    title = {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
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