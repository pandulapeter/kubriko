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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubriko.debugMenu.DebugMenu
import com.pandulapeter.kubrikoShowcase.BuildConfig
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntry
import com.pandulapeter.kubrikoShowcase.implementation.ShowcaseEntryType
import kubriko.app.generated.resources.Res
import kubriko.app.generated.resources.back
import kubriko.app.generated.resources.debug_menu
import kubriko.app.generated.resources.ic_back
import kubriko.app.generated.resources.ic_debug_off
import kubriko.app.generated.resources.ic_debug_on
import kubriko.app.generated.resources.ic_info_off
import kubriko.app.generated.resources.ic_info_on
import kubriko.app.generated.resources.img_logo
import kubriko.app.generated.resources.info
import kubriko.app.generated.resources.kubriko_showcase
import org.jetbrains.compose.resources.imageResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun TopBar(
    modifier: Modifier = Modifier,
    shouldUseCompactUi: Boolean,
    selectedShowcaseEntry: ShowcaseEntry?,
    onShowcaseEntrySelected: (ShowcaseEntry?) -> Unit,
    isInfoPanelVisible: Boolean,
    toggleInfoPanelVisibility: () -> Unit,
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
    val imageBitmap = imageResource(Res.drawable.img_logo)
    AnimatedVisibility(
        visible = selectedShowcaseEntry.shouldShowLogo && imageBitmap.width > 1,
        enter = fadeIn() + slideIn(animationSpec = tween(easing = EaseOut)) { IntOffset((it.width * 0.5f).roundToInt(), 0) },
        exit = slideOut(animationSpec = tween(easing = EaseIn)) { IntOffset((it.width * 0.75f).roundToInt(), 0) } + fadeOut(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Image(
                modifier = Modifier.size(144.dp).align(Alignment.BottomEnd),
                contentScale = ContentScale.Crop,
                alignment = Alignment.BottomEnd,
                bitmap = imageBitmap,
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
                    isInfoPanelVisible = isInfoPanelVisible,
                    toggleInfoPanelVisibility = toggleInfoPanelVisibility,
                )
            }
        } else {
            Header(
                modifier = Modifier.fillMaxWidth(),
                shouldUseCompactUi = shouldUseCompactUi,
                selectedShowcaseEntry = selectedShowcaseEntry,
                onShowcaseEntrySelected = onShowcaseEntrySelected,
                isInfoPanelVisible = isInfoPanelVisible,
                toggleInfoPanelVisibility = toggleInfoPanelVisibility,
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
    isInfoPanelVisible: Boolean,
    toggleInfoPanelVisibility: () -> Unit,
) = TopAppBar(
    modifier = modifier,
    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
    colors = TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
    title = {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = if (shouldUseCompactUi && !selectedShowcaseEntry.shouldShowLogo) 0.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(
                    resource = if (shouldUseCompactUi && selectedShowcaseEntry != null) {
                        selectedShowcaseEntry.titleStringResource
                    } else {
                        Res.string.kubriko_showcase
                    }
                ),
            )
            AnimatedVisibility(
                visible = selectedShowcaseEntry.shouldShowInfoButton,
                enter = fadeIn() + scaleIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                IconButton(
                    modifier = Modifier.size(48.dp),
                    onClick = toggleInfoPanelVisibility,
                ) {
                    Icon(
                        painter = painterResource(if (isInfoPanelVisible) Res.drawable.ic_info_on else Res.drawable.ic_info_off),
                        contentDescription = stringResource(Res.string.info),
                    )
                }
            }
            if (BuildConfig.IS_DEBUG_MENU_ENABLED) {
                AnimatedVisibility(
                    visible = selectedShowcaseEntry.shouldShowDebugButton,
                    enter = fadeIn() + scaleIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    IconButton(
                        modifier = Modifier.size(48.dp),
                        onClick = DebugMenu::toggleVisibility,
                    ) {
                        Icon(
                            painter = painterResource(if (DebugMenu.isVisible.collectAsState().value) Res.drawable.ic_debug_on else Res.drawable.ic_debug_off),
                            contentDescription = stringResource(Res.string.debug_menu),
                        )
                    }
                }
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

private val ShowcaseEntry?.shouldShowLogo get() = this == null || this == ShowcaseEntry.ABOUT || this == ShowcaseEntry.LICENSES

private val ShowcaseEntry?.shouldShowInfoButton get() = this?.type == ShowcaseEntryType.DEMO || this?.type == ShowcaseEntryType.TEST

private val ShowcaseEntry?.shouldShowDebugButton get() = this != null && this.type != ShowcaseEntryType.OTHER