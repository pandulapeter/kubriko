/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.ui

import androidx.compose.runtime.Composable
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.editor
import kubriko.examples.game_annoyed_penguins.generated.resources.ic_editor
import org.jetbrains.compose.resources.stringResource

@Composable
internal actual fun PlatformSpecificContent(
    sceneEditorConnection: SceneEditorConnection,
    playHoverSoundEffect: () -> Unit,
    playToggleSoundEffect: () -> Unit,
) {
    AnnoyedPenguinsButton(
        onButtonPressed = {
            playToggleSoundEffect()
            sceneEditorConnection.toggle()
        },
        icon = Res.drawable.ic_editor,
        title = stringResource(Res.string.editor),
        onPointerEnter = playHoverSoundEffect,
    )
}