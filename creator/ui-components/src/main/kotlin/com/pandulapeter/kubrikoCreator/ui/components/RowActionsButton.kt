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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.Res
import com.pandulapeter.kubrikoCreator.uiComponents.generated.resources.ic_more
import org.jetbrains.compose.resources.StringResource

/**
 * The ⋮ button that opens [menu] under itself: the same [RightClickMenu] a right-click on its row opens, so what a row
 * offers is the same whichever way it is asked for.
 */
@Composable
public fun RowActionsButton(
    menu: RightClickMenuState,
    label: StringResource,
    modifier: Modifier = Modifier,
) {
    val anchor = remember { arrayOfNulls<LayoutCoordinates>(1) }
    IconActionButton(
        modifier = modifier.onGloballyPositioned { anchor[0] = it },
        icon = Res.drawable.ic_more,
        label = label,
        onClick = { anchor[0]?.let(menu::openBelow) },
    )
}
