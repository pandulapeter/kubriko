/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data

import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.demoIsometricGraphics.implementation.renderer.data.generic.Vec3

data class RenderableCuboidModel(
    val id: String,
    val cuboidModel: CuboidModel,
    var position: Vec3 = Vec3.Zero,
    var rotationZ: AngleRadians = AngleRadians.Zero,
    // TODO: Add rotationX, rotationY, and maybe scale
)