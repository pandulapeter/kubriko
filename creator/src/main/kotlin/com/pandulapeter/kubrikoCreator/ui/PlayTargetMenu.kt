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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.ui.components.MenuStyle
import com.pandulapeter.kubrikoCreator.ui.components.WithTooltip
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_choose_play_target
import kubriko.creator.generated.resources.action_more_play_targets
import kubriko.creator.generated.resources.ic_android
import kubriko.creator.generated.resources.ic_check
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
    var isExpanded by remember { mutableStateOf(false) }
    WithTooltip(stringResource(Res.string.action_choose_play_target)) {
        TextButton(
            modifier = Modifier.height(32.dp),
            contentPadding = PaddingValues(start = 8.dp, end = 4.dp),
            onClick = {
                onOpened()
                isExpanded = true
            },
        ) {
            Icon(modifier = Modifier.size(16.dp), painter = painterResource(iconOf(targets.chosen.kind)), contentDescription = null)
            Text(modifier = Modifier.padding(start = 6.dp), text = playTargetText(targets.chosen))
            Icon(modifier = Modifier.size(16.dp), painter = painterResource(Res.drawable.ic_chevron), contentDescription = null)
        }
    }
    DropdownMenu(
        expanded = isExpanded,
        onDismissRequest = { isExpanded = false },
        shape = MenuStyle.shape,
        containerColor = MenuStyle.color,
        border = MenuStyle.border,
        shadowElevation = MenuStyle.elevation,
    ) {
        targets.available.forEach { target ->
            DropdownMenuItem(
                text = { Text(playTargetText(target)) },
                leadingIcon = { Icon(modifier = Modifier.size(16.dp), painter = painterResource(iconOf(target.kind)), contentDescription = null) },
                trailingIcon = if (target == targets.chosen) {
                    { Icon(modifier = Modifier.size(16.dp), painter = painterResource(Res.drawable.ic_check), contentDescription = null) }
                } else {
                    null
                },
                onClick = {
                    isExpanded = false
                    onChosen(target)
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.action_more_play_targets)) },
            leadingIcon = { Icon(modifier = Modifier.size(16.dp), painter = painterResource(Res.drawable.ic_settings), contentDescription = null) },
            onClick = {
                isExpanded = false
                onMoreTargets()
            },
        )
    }
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
