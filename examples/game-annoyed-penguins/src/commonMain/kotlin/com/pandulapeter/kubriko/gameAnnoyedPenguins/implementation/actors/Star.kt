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
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.Collidable
import com.pandulapeter.kubriko.collision.CollisionDetector
import com.pandulapeter.kubriko.collision.mask.CircleCollisionMask
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.base.DestructiblePhysicsObject
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.AudioManager
import com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.managers.GameplayManager
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.Scale
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_star
import kotlin.reflect.KClass

internal class Star private constructor(
    state: State
) : Visible, Editable<Star>, Dynamic, CollisionDetector {

    override val body = state.body
    private lateinit var actorManager: ActorManager
    private lateinit var audioManager: AudioManager
    private lateinit var gameplayManager: GameplayManager
    private lateinit var spriteManager: SpriteManager
    override val collisionMask = CircleCollisionMask(
        initialRadius = 64.sceneUnit,
        initialPosition = body.position,
    )
    override val collidableTypes: List<KClass<out Collidable>> = listOf(Penguin::class, DestructiblePhysicsObject::class)
    private var isAnimating = false

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        audioManager = kubriko.get()
        try {
            gameplayManager = kubriko.get()
        } catch (_: IllegalStateException) {
            // Happens in the Editor
        }
        spriteManager = kubriko.get()
    }

    override fun onRemoved() {
        if (!gameplayManager.isLoadingLevel.value) {
            if (actorManager.allActors.value.filterIsInstance<Star>().isEmpty()) {
                audioManager.playLevelDoneSoundEffect()
            } else {
                audioManager.playStarSoundEffect()
            }
        }
    }

    override fun onCollisionDetected(collidables: List<Collidable>) {
        if (!isAnimating) {
            isAnimating = true
            gameplayManager.onStarCollected()
        }
    }

    override fun DrawScope.draw() {
        spriteManager.get(Res.drawable.sprite_star)?.let {
            drawImage(it)
        }
    }

    override fun save() = State(
        body = body,
    )

    override fun update(deltaTimeInMilliseconds: Int) {
        body.rotation += AngleRadians.Pi * 0.0005f * deltaTimeInMilliseconds
        if (isAnimating) {
            body.scale -= Scale.Unit * 0.01f * deltaTimeInMilliseconds
            if (body.scale.horizontal <= 0f) {
                actorManager.remove(this)
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
    ) : Serializable.State<Star> {

        override fun restore() = Star(this)

        override fun serialize() = Json.encodeToString(this)
    }
}