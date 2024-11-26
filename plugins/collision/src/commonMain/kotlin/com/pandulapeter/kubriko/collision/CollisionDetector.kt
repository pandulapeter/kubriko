package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.actor.traits.Positionable
import kotlin.reflect.KClass

//TODO: Documentation
interface CollisionDetector : Positionable {

    val collidableTypes: List<KClass<out Collidable>>

    fun onCollisionDetected(collidables: List<Collidable>)
}