/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors.base.Collectable
import com.pandulapeter.kubriko.types.SceneOffset
import kubriko.examples.game_space_squadron.generated.resources.Res
import kubriko.examples.game_space_squadron.generated.resources.sprite_shield

internal class Shield(
    position: SceneOffset,
) : Collectable(
    position = position,
    spriteSheet = Res.drawable.sprite_shield,
    frameSize = IntSize(220, 218),
    frameCount = 70,
    framesPerRow = 9,
) {
    override fun Ship.onCollected() = onShieldCollected()
}