/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.sprites.AnimatedSprite
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.SceneOffset
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_penguin
import kotlin.math.roundToInt
import kotlin.random.Random

internal abstract class BlinkingPenguin(
    initialPosition: SceneOffset,
) : Visible, Dynamic {

    override val body = CircleBody(
        initialPosition = initialPosition,
        initialRadius = 128.sceneUnit,
    )
    private lateinit var spriteManager: SpriteManager
    private val animatedSprite = AnimatedSprite(
        getImageBitmap = { spriteManager.get(Res.drawable.sprite_penguin) },
        frameSize = IntSize(248, 256),
        frameCount = 2,
        framesPerRow = 2,
        framesPerSecond = 1f,
    )

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) = animatedSprite.stepForward(
        deltaTimeInMilliseconds = (deltaTimeInMilliseconds * (Random.nextFloat() * 5f)).roundToInt(),
        shouldLoop = true,
        speed = if (animatedSprite.isLastFrame) 2f else 0.3f,
    )

    final override fun DrawScope.draw() = animatedSprite.draw(this)
}