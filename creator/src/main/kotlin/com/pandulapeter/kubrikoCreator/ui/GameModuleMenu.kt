/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import com.pandulapeter.kubrikoCreator.ui.components.rememberMenuAnchor
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import com.pandulapeter.kubrikoCreator.workspace.GameModules
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_choose_game_module
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_module
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Which of the project's desktop modules is the game, the one Play plays and the Scene editor loads the classes of: the
 * chosen module's Gradle path, and a menu of every module that could be, for a project with several (a game beside its
 * own tools).
 */
@Composable
internal fun GameModuleMenu(
    modules: GameModules,
    onChosen: (String) -> Unit,
) = Box {
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    WithTooltip(stringResource(Res.string.action_choose_game_module)) {
        TextButton(
            modifier = anchor.modifier.height(32.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 4.dp),
            onClick = anchor::open,
        ) {
            Icon(modifier = Modifier.size(16.dp), painter = painterResource(Res.drawable.ic_module), contentDescription = null)
            Text(modifier = Modifier.padding(start = 6.dp), text = modules.chosen)
            Icon(modifier = Modifier.size(16.dp), painter = painterResource(Res.drawable.ic_chevron), contentDescription = null)
        }
    }
    RightClickMenu(
        state = menu,
        actions = modules.available.map { path ->
            ContextAction(label = path, isCurrent = path == modules.chosen) { onChosen(path) }
        },
    )
}
