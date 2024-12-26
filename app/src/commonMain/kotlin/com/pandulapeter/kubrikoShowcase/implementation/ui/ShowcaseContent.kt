package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
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
) { paddingValues ->
    val lazyListState = rememberLazyListState()
    val topBarHeight = TopBarHeight + WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
    ) {
        Column {
            AnimatedVisibility(
                visible = !isInFullscreenMode,
            ) {
                Spacer(modifier = Modifier.height(topBarHeight))
            }
            ExpandedContent(
                lazyListState = lazyListState,
                allShowcaseEntries = allShowcaseEntries,
                onShowcaseEntrySelected = onShowcaseEntrySelected,
                selectedShowcaseEntry = selectedShowcaseEntry,
                isInFullscreenMode = isInFullscreenMode,
                shouldUseCompactUi = shouldUseCompactUi,
                onFullscreenModeToggled = onFullscreenModeToggled,
                getSelectedShowcaseEntry = getSelectedShowcaseEntry,
            )
        }
        AnimatedVisibility(
            modifier = Modifier.padding(bottom = 8.dp),
            visible = !isInFullscreenMode,
            enter = fadeIn() + slideIn { IntOffset(0, -it.height) },
            exit = slideOut { IntOffset(0, -it.height) } + fadeOut(),
        ) {
            TopBar(
                modifier = Modifier.height(topBarHeight),
                shouldUseCompactUi = shouldUseCompactUi,
                selectedShowcaseEntry = selectedShowcaseEntry,
                onShowcaseEntrySelected = onShowcaseEntrySelected,
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
) = Box {
    val shouldShowSideMenu = !shouldUseCompactUi && !isInFullscreenMode
    val sideMenuWidth = SideMenuWidth + WindowInsets.safeDrawing.only(WindowInsetsSides.Left).asPaddingValues().calculateStartPadding(LocalLayoutDirection.current)
    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        AnimatedVisibility(
            visible = shouldShowSideMenu,
        ) {
            Spacer(modifier = Modifier.width(sideMenuWidth).background(MaterialTheme.colorScheme.surface))
        }
        val compactTransitionSpec: AnimatedContentTransitionScope<ShowcaseEntry?>.() -> ContentTransform =
            { fadeIn() + slideIn { IntOffset(0, if (targetState == null) -it.height / 10 else it.height / 10) } togetherWith fadeOut() }
        val expandedTransitionSpec: AnimatedContentTransitionScope<ShowcaseEntry?>.() -> ContentTransform =
            { fadeIn() togetherWith fadeOut() }
        AnimatedContent(
            modifier = Modifier.weight(1f),
            transitionSpec = if (shouldUseCompactUi) compactTransitionSpec else expandedTransitionSpec,
            targetState = selectedShowcaseEntry,
        ) { showcaseEntry ->
            showcaseEntry?.ExampleScreen(
                shouldUseCompactUi = shouldUseCompactUi,
                isInFullscreenMode = isInFullscreenMode,
                onFullscreenModeToggled = onFullscreenModeToggled,
                getSelectedShowcaseEntry = getSelectedShowcaseEntry,
            ) ?: CompactContent(
                lazyListState = lazyListState,
                allShowcaseEntries = allShowcaseEntries,
                onShowcaseEntrySelected = onShowcaseEntrySelected,
                selectedShowcaseEntry = selectedShowcaseEntry,
                shouldUseCompactUi = shouldUseCompactUi,
            )
        }
    }
    AnimatedVisibility(
        visible = shouldShowSideMenu,
        enter = fadeIn() + slideIn { IntOffset(-it.width, 0) },
        exit = slideOut { IntOffset(-it.width, 0) } + fadeOut(),
    ) {
        Surface(
            modifier = Modifier
                .padding(end = 8.dp)
                .width(sideMenuWidth)
                .fillMaxHeight(),
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

@Composable
private fun CompactContent(
    lazyListState: LazyListState,
    allShowcaseEntries: List<ShowcaseEntry>,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
    selectedShowcaseEntry: ShowcaseEntry?,
    shouldUseCompactUi: Boolean,
) = Crossfade(
    targetState = !shouldUseCompactUi,
) { shouldUseExpandedUi ->
    if (shouldUseExpandedUi) {
        WelcomeScreen(
            modifier = Modifier.fillMaxSize(),
        )
    } else {
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
    }
}

private val TopBarHeight = 64.dp
private val SideMenuWidth = 192.dp
