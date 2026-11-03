/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.ic_more
import org.jetbrains.compose.resources.StringResource

/**
 * The ⋮ button that opens [menu] of [actions] under itself: the same [RightClickMenu] a right-click on its row opens, so
 * what a row offers is the same whichever way it is asked for. The menu is held by the button, so opening it never moves
 * anything in the row.
 */
@Composable
public fun RowActionsButton(
    menu: RightClickMenuState,
    actions: List<ContextAction>,
    label: StringResource,
    modifier: Modifier = Modifier,
): Unit = Box(modifier) {
    val anchor = rememberMenuAnchor(menu)
    IconActionButton(
        modifier = anchor.modifier,
        icon = Res.drawable.ic_more,
        label = label,
        onClick = anchor::open,
    )
    RightClickMenu(menu, actions)
}
