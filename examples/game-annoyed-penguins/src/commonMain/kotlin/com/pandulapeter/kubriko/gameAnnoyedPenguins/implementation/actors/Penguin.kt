/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors

import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.extensions.sceneUnit
import com.pandulapeter.kubriko.sprites.SpriteManager
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_penguin

internal class Penguin : Visible {

    override val body = CircleBody(
        initialRadius = 128.sceneUnit,
    )
    private lateinit var spriteManager: SpriteManager

    override fun onAdded(kubriko: Kubriko) {
        spriteManager = kubriko.get()
    }

    override fun DrawScope.draw() {
        spriteManager.get(Res.drawable.sprite_penguin)?.let {
            drawImage(image = it)
        }
    }
}