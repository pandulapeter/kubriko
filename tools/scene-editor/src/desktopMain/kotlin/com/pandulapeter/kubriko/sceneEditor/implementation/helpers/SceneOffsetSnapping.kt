/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.math.roundToInt

internal fun SceneOffset.snapped(snapMode: Pair<Int, Int>) = SceneOffset(
    x = if (snapMode.first == 0) x else ((x.raw / snapMode.first).roundToInt() * snapMode.first).sceneUnit,
    y = if (snapMode.second == 0) y else ((y.raw / snapMode.second).roundToInt() * snapMode.second).sceneUnit,
)