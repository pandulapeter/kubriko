package com.pandulapeter.kubrikoStressTest.implementation.gameObjects.traits

import com.pandulapeter.kubriko.engine.gameObject.traits.Movable
import com.pandulapeter.kubriko.engine.gameObject.traits.Visible
import com.pandulapeter.kubriko.engine.implementation.extensions.angleTowards
import com.pandulapeter.kubriko.engine.implementation.extensions.deg
import com.pandulapeter.kubriko.engine.types.AngleDegrees

interface Destructible : Movable {
    var destructionState: Float
    override var direction: AngleDegrees
    override val friction: Float get() = 0.015f

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
        direction = 180f.deg - angleTowards(character)
        speed = 3f
    }
}