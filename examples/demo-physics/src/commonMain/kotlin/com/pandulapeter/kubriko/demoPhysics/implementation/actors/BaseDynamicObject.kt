package com.pandulapeter.kubriko.demoPhysics.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.isWithinViewportBounds
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.ViewportManager
import com.pandulapeter.kubriko.physics.RigidBody
import com.pandulapeter.kubriko.types.SceneOffset

internal abstract class BaseDynamicObject : RigidBody, Visible, Dynamic {

    private lateinit var actorManager: ActorManager
    private lateinit var viewportManager: ViewportManager

    override fun onAdded(kubriko: Kubriko) {
        actorManager = kubriko.require()
        viewportManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) {
        body.position = SceneOffset(physicsBody.position.x, physicsBody.position.y)
        body.rotation = physicsBody.orientation
        if (!body.axisAlignedBoundingBox.isWithinViewportBounds(viewportManager)) {
            actorManager.remove(this)
        }
    }
}