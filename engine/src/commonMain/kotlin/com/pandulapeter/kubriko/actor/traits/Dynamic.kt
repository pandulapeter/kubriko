package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor

/**
 * Should be implemented by [Actor]s that want to hook into the game loop.
 */
interface Dynamic : Actor {

    /**
     * Called in every frame unless the game state is paused.
     *
     * @param deltaTimeInMillis - The number of milliseconds since the previous frame.
     */
    fun update(deltaTimeInMillis: Float)
}