package com.pandulapeter.kubriko.traits

/**
 * Should be implemented by Actors who want to hook into the game loop.
 */
interface Dynamic {

    /**
     * Called in every frame unless the game state is paused.
     *
     * @param deltaTimeInMillis - The number of milliseconds since the previous frame.
     */
    fun update(deltaTimeInMillis: Float)
}