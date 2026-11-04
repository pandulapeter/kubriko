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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import com.pandulapeter.kubrikoCreator.ui.theme.CreatorMotion
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_collapse_group
import kubriko.creator.generated.resources.action_expand_group
import kubriko.creator.generated.resources.ic_chevron
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The chevron of whatever opens and closes in the sidebar (a module, a group, a folder), pointing down while it
 * [isExpanded] and turning to point at its name when it closes.
 */
@Composable
internal fun SidebarChevron(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (isExpanded) 0f else -90f, CreatorMotion.medium())
    Icon(
        modifier = modifier.rotate(rotation),
        painter = painterResource(Res.drawable.ic_chevron),
        contentDescription = stringResource(if (isExpanded) Res.string.action_collapse_group else Res.string.action_expand_group),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
