package com.pandulapeter.kubriko.types


/**
 * Defines the update frequency of the game loop relative to the screen's refresh rate.
 */
enum class FrameRate(val factor: Int) {

    /**
     * Updates occur on every frame.
     */
    NORMAL(1),

    /**
     * Updates occur every second frame.
     */
    HALF(2),

    /**
     * Updates occur every fourth frame.
     */
    QUARTER(4)
}