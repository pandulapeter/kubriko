/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameAnnoyedPenguins.implementation.actors.slingshot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerId
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.BoxBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Unique
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.collision.mask.PolygonCollisionMask
import com.pandulapeter.kubriko.helpers.extensions.abs
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.isInside
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.helpers.extensions.toOffset
import com.pandulapeter.kubriko.helpers.extensions.toSceneOffset
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.PhysicsBody
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.pointerInput.PointerInputAware
import com.pandulapeter.kubriko.pointerInput.PointerInputManager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableBoxBody
import com.pandulapeter.kubriko.sprites.SpriteManager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kubriko.examples.game_annoyed_penguins.generated.resources.Res
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_slingshot_background
import kubriko.examples.game_annoyed_penguins.generated.resources.sprite_slingshot_foreground

internal class Slingshot private constructor(state: State) : Visible, Editable<Slingshot>, Dynamic, PointerInputAware, RigidBody, Unique {
    override val body = state.body
    private lateinit var actorManager: ActorManager
    private lateinit var pointerInputManager: PointerInputManager
    private lateinit var spriteManager: SpriteManager
    private lateinit var viewportManager: ViewportManager
    override val collisionMask = PolygonCollisionMask(
        vertices = listOf(
            SceneOffset(
                x = SceneUnit.Zero,
                y = (-225).sceneUnit,
            ),
            SceneOffset(
                x = 75.sceneUnit,
                y = 225.sceneUnit,
            ),
            SceneOffset(
                x = (-75).sceneUnit,
                y = 225.sceneUnit,
            ),
        ),
        initialPosition = body.position + SceneOffset(
            x = 25.sceneUnit,
            y = 220.sceneUnit,
        ),
    )
    override val physicsBody = PhysicsBody(collisionMask).apply {
        density = 0f
    }
    override val drawingOrder = 1f
    override val isAlwaysActive = true
    private var aimingPointerId: PointerId? = null
        set(value) {
            field = value
            fakePenguin.isVisible = value != null
        }
    private val stringColor = Color(0xFF080808)
    private val armOffset1 = SceneOffset(
        x = body.size.width * 0.25f,
        y = -body.size.height * 0.4f,
    )
    private val armOffset2 = SceneOffset(
        x = -body.size.width * 0.21f,
        y = -body.size.height * 0.4f,
    )
    private val fakePenguin = FakePenguin(
        initialPosition = body.position - SceneOffset(
            x = SceneUnit.Zero,
            y = body.size.height * 0.4f,
        ),
    )
    private val front = object : Visible, Unique {
        override val body = this@Slingshot.body
        override val drawingOrder = -1f

        override fun DrawScope.draw() {
            spriteManager.get(Res.drawable.sprite_slingshot_foreground)?.let { foreground ->
                drawImage(foreground)
            }
        }
    }
    override val shouldClip = false

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        pointerInputManager = kubriko.get()
        spriteManager = kubriko.get()
        viewportManager = kubriko.get()
        actorManager.add(fakePenguin, front)
    }

    override fun DrawScope.draw() {
        spriteManager.get(Res.drawable.sprite_slingshot_background)?.let { background ->
            drawImage(background)
            if (fakePenguin.isVisible) {
                drawLine(
                    color = stringColor.copy(alpha = 1f - fakePenguin.distanceFromTarget),
                    start = body.pivot.raw + armOffset1.raw,
                    end = fakePenguin.body.position.raw - body.position.raw + body.pivot.raw,
                    cap = StrokeCap.Round,
                    strokeWidth = 16f,
                )
                drawLine(
                    color = stringColor.copy(alpha = 1f - fakePenguin.distanceFromTarget),
                    start = body.pivot.raw + armOffset2.raw,
                    end = fakePenguin.body.position.raw - body.position.raw + body.pivot.raw,
                    cap = StrokeCap.Round,
                    strokeWidth = 16f,
                )
            }
        }
    }

    override fun onPointerDrag(screenOffset: Offset) {
        if (aimingPointerId == null) {
            viewportManager.addToCameraPosition(-screenOffset)
        }
    }

    override fun onPointerZoom(position: Offset, factor: Float) = viewportManager.multiplyScaleFactor(factor)

    override fun onPointerReleased(pointerId: PointerId, screenOffset: Offset) {
        if (pointerId == aimingPointerId) {
            aimingPointerId = null
        }
    }

    private var isPointerPressedInPreviousStep = false

    override fun update(deltaTimeInMilliseconds: Int) {
        val pressedPointerPositions = pointerInputManager.pressedPointerPositions.value
        if (pressedPointerPositions.isNotEmpty()) {
            if (isPointerPressedInPreviousStep) {
                // Move the penguin that's already on the slingshot
                pressedPointerPositions[aimingPointerId]?.let { position ->
                    fakePenguin.pointerPosition = position.toSceneOffset(viewportManager)
                }
            } else {
                // Detect if the initial press was on the slingshot
                if (pressedPointerPositions.isNotEmpty()) {
                    pressedPointerPositions.firstNotNullOf { it }.let { pressPosition ->
                        if (pressPosition.value.toSceneOffset(viewportManager).isInside(body.axisAlignedBoundingBox)) {
                            aimingPointerId = pressPosition.key
                        }
                    }
                }
            }
        } else {
            // Move the camera automatically to keep the slingshot in focus
            val cameraPosition = viewportManager.cameraPosition.value
            val targetCameraPosition = SceneOffset(
                x = body.position.x,
                y = body.position.y,
            )
            if (abs(cameraPosition.x - targetCameraPosition.x).raw > 0 || abs(cameraPosition.y - targetCameraPosition.y).raw > 0) {
                viewportManager.addToCameraPosition(
                    (targetCameraPosition - cameraPosition).toOffset(viewportManager) * 0.05f
                )
            }
        }
        isPointerPressedInPreviousStep = pressedPointerPositions.isNotEmpty()
    }

    override fun save() = State(
        body = body,
    )

    @kotlinx.serialization.Serializable
    internal data class State(
        @SerialName("body") val body: SerializableBoxBody = BoxBody(),
    ) : Serializable.State<Slingshot> {

        override fun restore() = Slingshot(this)

        override fun serialize() = Json.encodeToString(this)
    }
}