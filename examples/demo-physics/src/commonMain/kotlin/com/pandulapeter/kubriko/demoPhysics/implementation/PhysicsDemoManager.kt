package com.pandulapeter.kubriko.demoPhysics.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.BouncyBall
import com.pandulapeter.kubriko.demoPhysics.implementation.actors.Platform

internal class PhysicsDemoManager : Manager() {

    private lateinit var actorManager: ActorManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        actorManager.add(
            actors = ((0..40).map {
                BouncyBall(
                    radius = (10..50).random().toFloat().scenePixel,
                    initialPosition = SceneOffset(
                        x = (-500..500).random().toFloat().scenePixel,
                        y = (-800..0).random().toFloat().scenePixel,
                    ),
                )
            } + Platform(
                initialPosition = SceneOffset(0f.scenePixel, 350f.scenePixel),
                boundingBox = SceneSize(800f.scenePixel, 40f.scenePixel),
            )).toTypedArray()
        )
    }
}