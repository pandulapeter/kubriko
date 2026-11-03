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
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.ui.components.ContextAction
import com.pandulapeter.kubrikoCreator.ui.components.RightClickMenu
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import com.pandulapeter.kubrikoCreator.ui.components.rememberMenuAnchor
import com.pandulapeter.kubrikoCreator.ui.components.rememberRightClickMenuState
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_choose_play_target
import kubriko.creator.generated.resources.action_more_play_targets
import kubriko.creator.generated.resources.ic_android
import kubriko.creator.generated.resources.ic_chevron
import kubriko.creator.generated.resources.ic_desktop
import kubriko.creator.generated.resources.ic_iphone
import kubriko.creator.generated.resources.ic_settings
import kubriko.creator.generated.resources.ic_web
import kubriko.creator.generated.resources.play_target_android
import kubriko.creator.generated.resources.play_target_desktop
import kubriko.creator.generated.resources.play_target_ios
import kubriko.creator.generated.resources.play_target_web
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Where Play plays the game: the chosen place, and a menu of every place it can be played now, looked for again each time
 * it opens ([onOpened]), with the way to turn more targets on ([onMoreTargets]).
 */
@Composable
internal fun PlayTargetMenu(
    targets: PlayTargets,
    onOpened: () -> Unit,
    onChosen: (PlayTarget) -> Unit,
    onMoreTargets: () -> Unit,
) = Box {
    val menu = rememberRightClickMenuState()
    val anchor = rememberMenuAnchor(menu)
    WithTooltip(stringResource(Res.string.action_choose_play_target)) {
        TextButton(
            modifier = anchor.modifier.height(32.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 4.dp),
            onClick = {
                onOpened()
                anchor.open()
            },
        ) {
            Icon(modifier = Modifier.size(16.dp), painter = painterResource(iconOf(targets.chosen.kind)), contentDescription = null)
            Text(modifier = Modifier.padding(start = 6.dp), text = playTargetText(targets.chosen))
            Icon(modifier = Modifier.size(16.dp), painter = painterResource(Res.drawable.ic_chevron), contentDescription = null)
        }
    }
    RightClickMenu(
        state = menu,
        actions = targets.available.map { target ->
            ContextAction(label = playTargetText(target), icon = iconOf(target.kind), isCurrent = target == targets.chosen) { onChosen(target) }
        } + ContextAction(label = stringResource(Res.string.action_more_play_targets), icon = Res.drawable.ic_settings, isSeparated = true, onClick = onMoreTargets),
    )
}

/**
 * How the menu names [target]: by what it calls itself, or by its kind.
 */
@Composable
private fun playTargetText(target: PlayTarget) = target.name ?: when (target.kind) {
    PlayTarget.Kind.DESKTOP -> stringResource(Res.string.play_target_desktop)
    PlayTarget.Kind.WEB -> stringResource(Res.string.play_target_web)
    PlayTarget.Kind.ANDROID -> stringResource(Res.string.play_target_android)
    PlayTarget.Kind.IOS -> stringResource(Res.string.play_target_ios)
}

private fun iconOf(kind: PlayTarget.Kind) = when (kind) {
    PlayTarget.Kind.DESKTOP -> Res.drawable.ic_desktop
    PlayTarget.Kind.WEB -> Res.drawable.ic_web
    PlayTarget.Kind.ANDROID -> Res.drawable.ic_android
    PlayTarget.Kind.IOS -> Res.drawable.ic_iphone
}
