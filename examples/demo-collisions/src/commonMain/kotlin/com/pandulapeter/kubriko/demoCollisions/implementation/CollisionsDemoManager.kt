package com.pandulapeter.kubriko.demoCollisions.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.demoCollisions.implementation.actors.TestBox
import com.pandulapeter.kubriko.demoCollisions.implementation.actors.TestCircle
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset

internal class CollisionsDemoManager : Manager() {

    private lateinit var actorManager: ActorManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        actorManager.add(
            TestBox(
                position = SceneOffset(
                    x = 100f.scenePixel,
                    y = 100f.scenePixel,
                ),
                hue = (0..360).random().toFloat(),
            ),
            TestCircle(
                position = SceneOffset(
                    x = (-100f).scenePixel,
                    y = (-100f).scenePixel,
                ),
                hue = (0..360).random().toFloat(),
            ),
        )
    }
}