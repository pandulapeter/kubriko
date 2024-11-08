package com.pandulapeter.kubrikoPhysicsTest.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.implementation.extensions.scenePixel
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset
import com.pandulapeter.kubriko.types.SceneSize
import com.pandulapeter.kubrikoPhysicsTest.implementation.actors.BouncyBall
import com.pandulapeter.kubrikoPhysicsTest.implementation.actors.Platform

internal class GameplayManager : Manager() {

    private lateinit var actorManager: ActorManager

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        actorManager.add(
            actors = ((0..50).map {
                BouncyBall(
                    radius = (10..50).random().toFloat().scenePixel,
                    position = SceneOffset(
                        x = (-0..0).random().toFloat().scenePixel,
                        y = (-1200..1200).random().toFloat().scenePixel,
                    ),
                )
            } + Platform(
                position = SceneOffset(0f.scenePixel, 350f.scenePixel),
                boundingBox = SceneSize(1200f.scenePixel, 40f.scenePixel),
            )).toTypedArray()
        )
    }
}