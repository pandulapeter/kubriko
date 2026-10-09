/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

internal val SpaceSquadronUIElementShape: CornerBasedShape = RoundedCornerShape(
    topStart = CornerSize(16.dp),
    topEnd = CornerSize(0),
    bottomStart = CornerSize(0),
    bottomEnd = CornerSize(16.dp),
)

internal fun Modifier.spaceSquadronUIElementBorder() = border(
    width = 2.dp,
    brush = SolidColor(Color.DarkGray),
    shape = SpaceSquadronUIElementShape,
)
