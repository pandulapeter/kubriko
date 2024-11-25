package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.types.SceneOffset
import kotlin.reflect.KClass

//TODO: Documentation
interface CollisionDetector : Collidable, Actor {

    val collidableTypes: List<KClass<out Collidable>>

    fun onCollisionDetected(collidables: List<Collidable>)
}