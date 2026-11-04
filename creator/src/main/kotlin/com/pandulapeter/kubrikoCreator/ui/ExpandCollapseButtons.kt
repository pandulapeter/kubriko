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

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.ui.components.IconActionButton
import com.pandulapeter.kubrikoCreator.ui.components.revealedWhen
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.action_collapse_all
import kubriko.creator.generated.resources.action_expand_all
import kubriko.creator.generated.resources.ic_unfold_less
import kubriko.creator.generated.resources.ic_unfold_more

/**
 * The buttons at the end of a sidebar row holding folders that open or close every one of them, shown while [isShown]
 * (the row is pointed at) and keeping their place otherwise, so nothing moves when they show.
 */
@Composable
internal fun ExpandCollapseButtons(
    isShown: Boolean,
    actions: SubfolderActions,
    modifier: Modifier = Modifier,
) = Row(modifier.revealedWhen(isShown)) {
    IconActionButton(
        icon = Res.drawable.ic_unfold_more,
        label = Res.string.action_expand_all,
        onClick = actions.onExpandAll,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    IconActionButton(
        icon = Res.drawable.ic_unfold_less,
        label = Res.string.action_collapse_all,
        onClick = actions.onCollapseAll,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
