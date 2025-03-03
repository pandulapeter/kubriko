/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kubriko.examples.game_blockys_journey.generated.resources.Res
import kubriko.examples.game_blockys_journey.generated.resources.editor
import kubriko.examples.game_blockys_journey.generated.resources.ic_editor
import org.jetbrains.compose.resources.stringResource

internal val isSceneEditorVisible = MutableStateFlow(false)

@Composable
internal actual fun PlatformSpecificContent(
    playHoverSoundEffect: () -> Unit,
    playToggleSoundEffect: () -> Unit,
) {
    val isEditorVisible = isSceneEditorVisible.collectAsState()
    BlockysJourneyButton(
        onButtonPressed = {
            playToggleSoundEffect()
            isSceneEditorVisible.value = !isEditorVisible.value
        },
        icon = Res.drawable.ic_editor,
        title = stringResource(Res.string.editor),
        onPointerEnter = playHoverSoundEffect,
    )
}