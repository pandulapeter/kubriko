package com.pandulapeter.kubriko.collision

import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.collision.implementation.CollisionManagerImpl
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.types.SceneOffset

/**
 * TODO: Documentation
 */
abstract class CollisionManager : Manager() {

    abstract fun isOverlapping(positionable1: Positionable, positionable2: Positionable) : Boolean

    abstract fun isInside(sceneOffset: SceneOffset, positionable: Positionable) : Boolean

    companion object {
        fun newInstance(): CollisionManager = CollisionManagerImpl()
    }
}