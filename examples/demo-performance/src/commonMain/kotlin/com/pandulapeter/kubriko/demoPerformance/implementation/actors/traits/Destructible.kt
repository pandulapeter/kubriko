package com.pandulapeter.kubriko.demoPerformance.implementation.actors.traits

import com.pandulapeter.kubriko.actor.traits.Movable
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.implementation.extensions.angleTowards
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.types.AngleRadians
import com.pandulapeter.kubriko.types.ScenePixel

interface Destructible : Movable {
    var destructionState: Float
    override var direction: AngleRadians
    override val friction: ScenePixel get() = 0.015f.scenePixel

    override fun update(deltaTimeInMillis: Float) {
        super.update(deltaTimeInMillis)
        if (destructionState > 0) {
            if (destructionState < 1f) {
                destructionState += 0.001f * deltaTimeInMillis
            } else {
                destructionState = 1f
            }
        }
    }

    fun destroy(character: Visible) {
        if (destructionState == 0f) {
            destructionState = 0.01f
        }
        direction = AngleRadians.Pi - if (this is Visible) angleTowards(character) else angleTowards(character)
        speed = 3f.scenePixel
    }

    private fun Positionable.angleTowards(other: Positionable): AngleRadians = (body.position).angleTowards(other.body.position)

    private fun Visible.angleTowards(other: Visible): AngleRadians = (body.position + body.pivot).angleTowards(other.body.position + other.body.pivot)
}