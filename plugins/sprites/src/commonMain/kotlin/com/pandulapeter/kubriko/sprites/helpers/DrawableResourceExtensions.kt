/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites.helpers

import com.pandulapeter.kubriko.sprites.SpriteResource
import com.pandulapeter.kubriko.sprites.SpriteResource.Rotation
import org.jetbrains.compose.resources.DrawableResource

/**
 * Wraps this drawable into a [SpriteResource] that can be loaded through the `SpriteManager`.
 *
 * @param rotation The rotation applied to the loaded bitmap, none by default.
 */
fun DrawableResource.toSpriteResource(rotation: Rotation = Rotation.NONE) = SpriteResource(this, rotation)
