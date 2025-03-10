/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.body.CircleBody
import com.pandulapeter.kubriko.actor.body.RectangleBody
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Group
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.helpers.extensions.get
import com.pandulapeter.kubriko.helpers.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.helpers.extensions.sceneUnit
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.JointWrapper
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.physics.implementation.dynamics.Body
import com.pandulapeter.kubriko.physics.implementation.geometry.Circle
import com.pandulapeter.kubriko.physics.implementation.joints.JointToBody
import com.pandulapeter.kubriko.physics.implementation.math.Vec2
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.serialization.Serializable
import com.pandulapeter.kubriko.serialization.typeSerializers.SerializableSceneOffset
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.types.SceneUnit
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlin.random.Random

// TODO: Something is off with the Editor preview
internal class DynamicChain private constructor(state: State) : Group, Dynamic, Visible, Editable<DynamicChain> {
    private val chainLinks = (0..state.linkCount).map { linkIndex ->
        ChainLink(
            initialPosition = SceneOffset(
                x = state.initialCenterOffset.x + LinkDistance * (state.linkCount / 2) - (LinkDistance * linkIndex),
                y = state.initialCenterOffset.y,
            )
        )
    }
    private val joints = chainLinks.mapIndexedNotNull { index, chainLink ->
        if (index > 0) JointWrapper(
            physicsJoint = JointToBody(
                body1 = chainLinks[index - 1].physicsBody,
                body2 = chainLink.physicsBody,
                jointLength = ChainLink.Radius,
                jointConstant = 2000f,
                dampening = 0.0001f,
                canGoSlack = false,
                offset1 = Vec2(-ChainLink.Radius, SceneUnit.Zero),
                offset2 = Vec2(ChainLink.Radius, SceneUnit.Zero),
            )
        ) else null
    }
    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager
    override val actors = chainLinks + joints
    override val body = RectangleBody()
    override val drawingOrder = -1f
    private val offset = SceneOffset(ChainLink.Radius * 2, ChainLink.Radius * 2)
    private val color = Color.hsv(
        hue = Random.nextFloat() * 360f,
        saturation = 0.3f,
        value = 1f,
    )

    init {
        refreshBodySize()
    }

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.get()
        viewportManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        if (chainLinks.none { it.body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager) }) {
            actorManager.remove(this)
        } else {
           refreshBodySize()
        }
    }

    private fun refreshBodySize() {
        val left = chainLinks.minOf { it.body.position.x } - offset.x
        val top = chainLinks.minOf { it.body.position.y } - offset.y
        val right = chainLinks.maxOf { it.body.position.x } + offset.x
        val bottom = chainLinks.maxOf { it.body.position.y } + offset.y
        body.position = SceneOffset(left, top)
        body.size = SceneSize(right - left, bottom - top)
    }

    private val strokeOutline = Stroke(
        width = ChainLink.Radius.raw * 2,
        cap = StrokeCap.Round,
    )
    private val strokeInside = Stroke(
        width = ChainLink.Radius.raw * 2 - 2,
        cap = StrokeCap.Round,
    )

    override fun DrawScope.draw() {
        if (chainLinks.size >= 2) {
            val path = Path().apply {
                val firstPoint = chainLinks.first().body.position - chainLinks.first().body.pivot - body.position + offset / 2
                moveTo(firstPoint.x.raw, firstPoint.y.raw)
                for (i in 1 until chainLinks.size) {
                    val currentPoint = chainLinks[i].body.position - chainLinks[i].body.pivot - body.position + offset / 2
                    val previousPoint = chainLinks[i - 1].body.position - chainLinks[i - 1].body.pivot - body.position + offset / 2
                    val midPoint = (currentPoint + previousPoint) / 2f
                    quadraticTo(previousPoint.x.raw, previousPoint.y.raw, midPoint.x.raw, midPoint.y.raw)
                }
                val lastPoint = chainLinks.last().body.position - chainLinks.last().body.pivot - body.position + offset / 2
                lineTo(lastPoint.x.raw, lastPoint.y.raw)
            }
            drawPath(
                path = path,
                color = Color.Black,
                style = strokeOutline,
            )
            drawPath(
                path = path,
                color = color,
                style = strokeInside,
            )
        }
    }

    override fun save() = State(
        linkCount = chainLinks.size,
        initialCenterOffset = body.position,
    )

    @kotlinx.serialization.Serializable
    data class State(
        @SerialName("linkCount") val linkCount: Int = 0,
        @SerialName("initialCenterOffset") val initialCenterOffset: SerializableSceneOffset = SceneOffset.Zero,
    ) : Serializable.State<DynamicChain> {

        override fun restore() = DynamicChain(this)

        override fun serialize() = Json.encodeToString(this)
    }

    // The only reason for this Actor to be Visible is for the debug menu overlay
    private class ChainLink(
        initialPosition: SceneOffset,
    ) : RigidBody, Visible, Dynamic {
        override val physicsBody = Body(
            shape = Circle(Radius),
            x = initialPosition.x,
            y = initialPosition.y,
        ).apply {
            density = 5f
            restitution = 0.1f
        }
        override val body = CircleBody(
            initialRadius = Radius,
            initialPosition = initialPosition,
        )

        override fun update(deltaTimeInMilliseconds: Int) {
            body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
            body.rotation = physicsBody.orientation
        }

        override fun DrawScope.draw() = Unit

        companion object {
            val Radius = 12f.sceneUnit
        }
    }

    companion object {
        private val LinkDistance = ChainLink.Radius * 3f
    }
}