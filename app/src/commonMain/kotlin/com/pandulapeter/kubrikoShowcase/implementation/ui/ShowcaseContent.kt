/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubrikoShowcase.BuildConfig
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntryType
import com.pandulapeter.kubrikoShowcase.implementation.ui.welcome.WelcomeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.welcome
import kubriko.app.generated.resources.welcome_subtitle

@Composable
internal fun ShowcaseContent(
    shouldUseCompactUi: Boolean,
    shouldUseWideSideMenu: Boolean,
    allShowcaseEntries: List<ShowcaseEntry>,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
    activeKubrikoInstance: Kubriko?,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    isInfoPanelVisible: Boolean,
    toggleInfoPanelVisibility: () -> Unit,
) = Surface(
    tonalElevation = if (isSystemInDarkTheme()) 2.dp else 0.dp,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        val collapsedLazyListState = rememberLazyListState()
        val expandedLazyListState = rememberLazyListState()
        val topBarHeight = TopBarHeight + WindowInsets.safeDrawing.only(WindowInsetsSides.Top).asPaddingValues().calculateTopPadding()
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            Column {
                AnimatedVisibility(
                    visible = isInFullscreenMode != true,
                ) {
                    Spacer(modifier = Modifier.height(topBarHeight))
                }
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val windowInsets = when {
                        isInFullscreenMode == true -> WindowInsets.safeDrawing
                        shouldUseCompactUi -> WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                        else -> WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Right)
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                    ) {
                        ExpandedContent(
                            modifier = Modifier.weight(1f),
                            collapsedLazyListState = collapsedLazyListState,
                            expandedLazyListState = expandedLazyListState,
                            allShowcaseEntries = allShowcaseEntries,
                            onShowcaseEntrySelected = onShowcaseEntrySelected,
                            activeKubrikoInstance = activeKubrikoInstance,
                            selectedShowcaseEntry = selectedShowcaseEntry,
                            windowInsets = windowInsets,
                            isInFullscreenMode = isInFullscreenMode,
                            shouldUseCompactUi = shouldUseCompactUi,
                            shouldUseWideSideMenu = shouldUseWideSideMenu,
                            onFullscreenModeToggled = onFullscreenModeToggled,
                            getSelectedShowcaseEntry = getSelectedShowcaseEntry,
                        )
                        if (BuildConfig.IS_DEBUG_MENU_ENABLED) {
                            DebugMenu.Vertical(
                                kubriko = activeKubrikoInstance,
                                isEnabled = !shouldUseCompactUi && selectedShowcaseEntry.hasDebugMenu,
                                windowInsets = windowInsets,
                            )
                        }
                    }
                    if (BuildConfig.IS_DEBUG_MENU_ENABLED) {
                        DebugMenu.Horizontal(
                            kubriko = activeKubrikoInstance,
                            isEnabled = shouldUseCompactUi && selectedShowcaseEntry.hasDebugMenu,
                            windowInsets = windowInsets,
                        )
                    }
                }
            }
            AnimatedVisibility(
                modifier = Modifier.padding(bottom = 8.dp),
                visible = isInFullscreenMode != true,
                enter = fadeIn() + slideIn { IntOffset(0, -it.height) },
                exit = slideOut { IntOffset(0, -it.height) } + fadeOut(),
            ) {
                TopBar(
                    modifier = Modifier.height(topBarHeight),
                    shouldUseCompactUi = shouldUseCompactUi,
                    selectedShowcaseEntry = selectedShowcaseEntry,
                    onShowcaseEntrySelected = onShowcaseEntrySelected,
                    isInfoPanelVisible = isInfoPanelVisible,
                    toggleInfoPanelVisibility = toggleInfoPanelVisibility,
                )
            }
        }
    }
}

@Composable
private fun ExpandedContent(
    modifier: Modifier,
    collapsedLazyListState: LazyListState,
    expandedLazyListState: LazyListState,
    allShowcaseEntries: List<ShowcaseEntry>,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
    selectedShowcaseEntry: ShowcaseEntry?,
    activeKubrikoInstance: Kubriko?,
    windowInsets: WindowInsets,
    shouldUseCompactUi: Boolean,
    shouldUseWideSideMenu: Boolean,
    isInFullscreenMode: Boolean?,
    onFullscreenModeToggled: () -> Unit,
    getSelectedShowcaseEntry: () -> ShowcaseEntry?,
) = Box(
    modifier = modifier,
) {
    val shouldShowSideMenu = !shouldUseCompactUi && isInFullscreenMode != true
    val sideMenuWidth by animateDpAsState(
        targetValue = (if (shouldUseWideSideMenu) WideSideMenuWidth else ThinSideMenuWidth) + WindowInsets.safeDrawing
            .only(WindowInsetsSides.Left)
            .asPaddingValues()
            .calculateStartPadding(LocalLayoutDirection.current),
        animationSpec = tween(),
    )
    val previouslyFocusedShowcaseEntry = remember { mutableStateOf(selectedShowcaseEntry) }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(shouldUseCompactUi) {
        val lazyListState = if (shouldUseCompactUi) collapsedLazyListState else expandedLazyListState
        val menuItemIndex = selectedShowcaseEntry.menuItemIndex
        if (!lazyListState.isScrollInProgress) {
            if (lazyListState.firstVisibleItemIndex >= menuItemIndex || (lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) <= menuItemIndex) {
                coroutineScope.launch { lazyListState.scrollToItem(menuItemIndex) }
            }
        }
    }
    LaunchedEffect(selectedShowcaseEntry) {
        val lazyListState = if (shouldUseCompactUi) collapsedLazyListState else expandedLazyListState
        if (!lazyListState.isScrollInProgress) {
            val itemIndex = (if (shouldUseCompactUi) selectedShowcaseEntry ?: previouslyFocusedShowcaseEntry.value else selectedShowcaseEntry).menuItemIndex
            if (lazyListState.firstVisibleItemIndex >= itemIndex) {
                coroutineScope.launch { lazyListState.animateScrollToItem(itemIndex) }
            } else if ((lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) <= itemIndex && !shouldUseCompactUi) {
                coroutineScope.launch {
                    lazyListState.run {
                        if (selectedShowcaseEntry == ShowcaseEntry.entries.last()) {
                            animateScrollToItem(itemIndex)
                        } else {
                            while (canScrollForward && (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) <= itemIndex) {
                                scrollBy(2f)
                                delay(1)
                            }
                        }
                    }
                }
            }
        }
        previouslyFocusedShowcaseEntry.value = selectedShowcaseEntry
    }
    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        AnimatedVisibility(
            visible = shouldShowSideMenu,
        ) {
            Spacer(modifier = Modifier.width(sideMenuWidth).background(MaterialTheme.colorScheme.surface))
        }
        @Composable
        fun Content() {
            val compactTransitionSpec: AnimatedContentTransitionScope<ShowcaseEntry?>.() -> ContentTransform =
                { fadeIn() + slideIn { IntOffset(0, if (targetState == null) -it.height / 10 else it.height / 10) } togetherWith fadeOut() }
            val expandedTransitionSpec: AnimatedContentTransitionScope<ShowcaseEntry?>.() -> ContentTransform =
                { fadeIn() togetherWith fadeOut() }
            AnimatedContent(
                transitionSpec = if (shouldUseCompactUi) compactTransitionSpec else expandedTransitionSpec,
                targetState = selectedShowcaseEntry,
                contentAlignment = Alignment.Center,
            ) { showcaseEntry ->
                showcaseEntry?.ExampleScreen(
                    windowInsets = windowInsets,
                    isInFullscreenMode = isInFullscreenMode,
                    onFullscreenModeToggled = onFullscreenModeToggled,
                    getSelectedShowcaseEntry = getSelectedShowcaseEntry,
                ) ?: CompactContent(
                    lazyListState = collapsedLazyListState,
                    allShowcaseEntries = allShowcaseEntries,
                    onShowcaseEntrySelected = onShowcaseEntrySelected,
                    selectedShowcaseEntry = selectedShowcaseEntry,
                    shouldUseCompactUi = shouldUseCompactUi,
                )
            }
        }
        if (BuildConfig.IS_DEBUG_MENU_ENABLED) {
            DebugMenu.OverlayOnly(
                kubriko = activeKubrikoInstance,
                kubrikoViewport = { Content() },
                buttonAlignment = null,
            )
        } else {
            Content()
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
                state = expandedLazyListState,
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
    welcomeScreenScrollState: ScrollState = rememberScrollState(),
) = Crossfade(
    targetState = !shouldUseCompactUi,
) { shouldUseExpandedUi ->
    val coroutineScope = rememberCoroutineScope()
    if (shouldUseExpandedUi) {
        WelcomeScreen(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(welcomeScreenScrollState)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Right))
                .padding(bottom = 8.dp),
            shouldUseCompactUi = false,
            scrollToTop = { coroutineScope.launch { lazyListState.animateScrollToItem(0) } },
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
                    WelcomeScreen(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                        shouldUseCompactUi = true,
                        scrollToTop = { coroutineScope.launch { lazyListState.animateScrollToItem(0) } },
                    )
                }
                menu(
                    allShowcaseEntries = allShowcaseEntries,
                    selectedShowcaseEntry = selectedShowcaseEntry,
                    onShowcaseEntrySelected = onShowcaseEntrySelected,
                )
                item("spacer") {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

private val ShowcaseEntry?.hasDebugMenu
    get() = when (this?.type) {
        null, ShowcaseEntryType.OTHER -> false
        else -> true
    }

private val ShowcaseEntry?.menuItemIndex
    get() = when (this?.type) {
        null -> 0
        ShowcaseEntryType.GAME -> ShowcaseEntry.entries.indexOf(this) + 2
        ShowcaseEntryType.DEMO -> ShowcaseEntry.entries.indexOf(this) + 3
        ShowcaseEntryType.TEST -> ShowcaseEntry.entries.indexOf(this) + 4
        ShowcaseEntryType.OTHER -> ShowcaseEntry.entries.indexOf(this) + if (BuildConfig.ARE_TEST_EXAMPLES_ENABLED) 4 else 5
    }

private val TopBarHeight = 64.dp
private val ThinSideMenuWidth = 192.dp
private val WideSideMenuWidth = 320.dp