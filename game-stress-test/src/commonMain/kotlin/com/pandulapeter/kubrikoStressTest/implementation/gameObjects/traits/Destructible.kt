package com.pandulapeter.kubrikoStressTest.implementation.gameObjects.traits

import com.pandulapeter.kubriko.engine.traits.Movable
import com.pandulapeter.kubriko.engine.traits.Visible
import com.pandulapeter.kubriko.engine.implementation.extensions.angleTowards
import com.pandulapeter.kubriko.engine.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.engine.types.AngleRadians
import com.pandulapeter.kubriko.engine.types.ScenePixel

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
        direction = AngleRadians.Pi - angleTowards(character)
        speed = 3f.scenePixel
    }
}