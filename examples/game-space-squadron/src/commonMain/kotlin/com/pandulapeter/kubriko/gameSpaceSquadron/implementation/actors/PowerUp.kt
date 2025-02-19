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
import kubriko.examples.game_space_squadron.generated.resources.sprite_power_up

internal class PowerUp(
    position: SceneOffset,
) : Collectable(
    position = position,
    spriteSheet = Res.drawable.sprite_power_up,
    frameSize = IntSize(198, 186),
    frameCount = 87,
    framesPerRow = 10,
) {
    override fun Ship.onCollected() = onPowerUpCollected()
}