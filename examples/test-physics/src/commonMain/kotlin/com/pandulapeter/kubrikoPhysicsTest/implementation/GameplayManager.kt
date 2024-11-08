package com.pandulapeter.kubrikoPhysicsTest.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.ActorManager
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.Body
import com.pandulapeter.kubriko.physicsManager.implementation.dynamics.World
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Circle
import com.pandulapeter.kubriko.physicsManager.implementation.geometry.Polygon
import com.pandulapeter.kubriko.physicsManager.implementation.math.Vec2
import com.pandulapeter.kubrikoPhysicsTest.implementation.actors.BouncyBallActor
import com.pandulapeter.kubrikoPhysicsTest.implementation.actors.PlatformActor

internal class GameplayManager : Manager() {

    private lateinit var actorManager: ActorManager
    private val world = World(Vec2(.0, 9.81))

    override fun onInitialize(kubriko: Kubriko) {
        actorManager = kubriko.require()
        val platform = Body(Polygon(600.0, 20.0), .0, 400.0)
        platform.density = .0
        world.addBody(platform)
        actorManager.add(PlatformActor(platform))
        (0..100).forEach {
            createBall(
                radius = (10..50).random().toDouble(),
                x = (-600..600).random().toDouble(),
                y = (-400..200).random().toDouble()
            )
        }
    }

    private fun createBall(
        radius: Double,
        x: Double,
        y: Double
    ) {
        val bouncyBall = Body(Circle(radius), x, y)
        world.addBody(bouncyBall)
        actorManager.add(BouncyBallActor(bouncyBall, radius.toFloat()))
    }

    override fun onUpdate(deltaTimeInMillis: Float, gameTimeNanos: Long) {
        world.step(0.1) // TODO: Should be in function of deltaTimeInMillis
    }
}