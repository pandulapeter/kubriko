package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.welcome
import kubriko.app.generated.resources.welcome_subtitle

@Composable
internal fun ShowcaseContent(
    shouldUseCompactUi: Boolean,
    allShowcaseEntries: List<ShowcaseEntry>,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
    isInFullscreenMode: Boolean,
    onFullscreenModeToggled: () -> Unit,
) = Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    topBar = {
        if (!isInFullscreenMode) {
            TopBar(
                shouldUseCompactUi = shouldUseCompactUi,
                selectedShowcaseEntry = selectedShowcaseEntry,
                onShowcaseEntrySelected = onShowcaseEntrySelected,
            )
        }
    }
) { paddingValues ->
    val lazyListState = rememberLazyListState()
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues)
    ) {
        if (isInFullscreenMode) FullscreenContent(
            selectedShowcaseEntry = selectedShowcaseEntry,
            shouldUseCompactUi = shouldUseCompactUi,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
            getSelectedShowcaseEntry = getSelectedShowcaseEntry,
        ) else if (shouldUseCompactUi) CompactContent(
            lazyListState = lazyListState,
            allShowcaseEntries = allShowcaseEntries,
            onShowcaseEntrySelected = onShowcaseEntrySelected,
            selectedShowcaseEntry = selectedShowcaseEntry,
            shouldUseCompactUi = shouldUseCompactUi,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
            getSelectedShowcaseEntry = getSelectedShowcaseEntry,
        ) else ExpandedContent(
            lazyListState = lazyListState,
            allShowcaseEntries = allShowcaseEntries,
            onShowcaseEntrySelected = onShowcaseEntrySelected,
            selectedShowcaseEntry = selectedShowcaseEntry,
            shouldUseCompactUi = shouldUseCompactUi,
            isInFullscreenMode = isInFullscreenMode,
            onFullscreenModeToggled = onFullscreenModeToggled,
            getSelectedShowcaseEntry = getSelectedShowcaseEntry,
        )
    }
}

@Composable
private fun FullscreenContent(
    selectedShowcaseEntry: ShowcaseEntry?,
    shouldUseCompactUi: Boolean,
    isInFullscreenMode: Boolean,
    onFullscreenModeToggled: () -> Unit,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
) {
    selectedShowcaseEntry?.exampleScreen(
        shouldUseCompactUi = shouldUseCompactUi,
        isInFullscreenMode = isInFullscreenMode,
        onFullscreenModeToggled = onFullscreenModeToggled,
        getSelectedShowcaseEntry = getSelectedShowcaseEntry,
    )
}

@Composable
private fun CompactContent(
    lazyListState: LazyListState,
    allShowcaseEntries: List<ShowcaseEntry>,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
    selectedShowcaseEntry: ShowcaseEntry?,
    shouldUseCompactUi: Boolean,
    isInFullscreenMode: Boolean,
    onFullscreenModeToggled: () -> Unit,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
) {
    AnimatedVisibility(
        visible = selectedShowcaseEntry == null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
            state = lazyListState,
        ) {
            item("welcome") {
                WelcomeScreen()
            }
            menu(
                allShowcaseEntries = allShowcaseEntries,
                selectedShowcaseEntry = selectedShowcaseEntry,
                onShowcaseEntrySelected = onShowcaseEntrySelected,
            )
        }
    }
    AnimatedVisibility(
        visible = selectedShowcaseEntry != null,
        enter = slideIn { IntOffset(0, it.height / 10) },
        exit = slideOut { IntOffset(0, it.height / 10) },
    ) {
        Crossfade(
            targetState = selectedShowcaseEntry,
        ) { showcaseEntry ->
            showcaseEntry?.exampleScreen(
                shouldUseCompactUi = shouldUseCompactUi,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = onFullscreenModeToggled,
                getSelectedShowcaseEntry = getSelectedShowcaseEntry,
            )
        }
    }
}

@Composable
private fun ExpandedContent(
    lazyListState: LazyListState,
    allShowcaseEntries: List<ShowcaseEntry>,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
    selectedShowcaseEntry: ShowcaseEntry?,
    shouldUseCompactUi: Boolean,
    isInFullscreenMode: Boolean,
    onFullscreenModeToggled: () -> Unit,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        Spacer(
            modifier = Modifier.width(
                200.dp + WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
            )
        )
        Crossfade(
            modifier = Modifier.weight(1f),
            targetState = selectedShowcaseEntry,
        ) { showcaseEntry ->
            showcaseEntry?.exampleScreen(
                shouldUseCompactUi = shouldUseCompactUi,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = onFullscreenModeToggled,
                getSelectedShowcaseEntry = getSelectedShowcaseEntry,
            ) ?: WelcomeScreen()
        }
    }
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.width(
                200.dp + WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
            ).fillMaxHeight(),
            tonalElevation = when (isSystemInDarkTheme()) {
                true -> 4.dp
                false -> 0.dp
            },
            shadowElevation = when (isSystemInDarkTheme()) {
                true -> 4.dp
                false -> 2.dp
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom).asPaddingValues(),
                state = lazyListState,
            ) {
                item {
                    MenuItem(
                        isSelected = selectedShowcaseEntry == null,
                        title = Res.string.welcome,
                        subtitle = Res.string.welcome_subtitle,
                        onSelected = { onShowcaseEntrySelected(null) },
                    )
                }
                menu(
                    allShowcaseEntries = allShowcaseEntries,
                    selectedShowcaseEntry = selectedShowcaseEntry,
                    onShowcaseEntrySelected = onShowcaseEntrySelected,
                )
            }
        }
    }
}