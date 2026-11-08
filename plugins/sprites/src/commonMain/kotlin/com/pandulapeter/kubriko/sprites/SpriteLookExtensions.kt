/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sprites

import com.pandulapeter.kubriko.actor.looks.SpriteLook

/**
 * Shows [region] through this look: its page becomes the [SpriteLook.texture], and the region the part of it drawn.
 * Null clears the texture, so the look draws nothing, as while an atlas is still loading. Setting the region the look
 * already shows changes nothing, so this can be called every frame (with [SpriteManager.getRegion], for example)
 * without rebuilding the look's geometry or allocating.
 */
fun SpriteLook.setRegion(region: SpriteRegion?) {
    if (region == null) {
        texture = null
        return
    }
    texture = region.page
    srcOffset = region.offset
    srcSize = region.size
}
