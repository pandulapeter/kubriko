/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubriko.types.SceneOffset

/**
 * Where a [SceneView]'s camera looks and how far it is zoomed, for the view that replaces it.
 */
internal data class SceneCamera(
    val position: SceneOffset,
    val scaleFactor: Float,
)
