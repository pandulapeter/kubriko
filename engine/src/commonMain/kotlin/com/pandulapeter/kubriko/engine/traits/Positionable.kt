package com.pandulapeter.kubriko.engine.traits

import com.pandulapeter.kubriko.engine.types.Scale
import com.pandulapeter.kubriko.engine.types.SceneOffset
import com.pandulapeter.kubriko.engine.types.SceneSize

/**
 * Actors that have a well defined position in the Scene should implement this interface.
 */
interface Positionable {

    /**
     * The virtual size of the Actor, calculated from the [pivotOffset].
     */
    val boundingBox: SceneSize

    /**
     * The absolute position of the Actor in the Scene.
     */
    var position: SceneOffset

    /**
     * The relative origin point of the [boundingBox]. Used as a center point positioning, scaling and rotation.
     */
    val pivotOffset: SceneOffset get() = boundingBox.center

    /**
     * A 2D multiplier that can be applied to the size of the [boundingBox].
     */
    val scale: Scale get() = Scale.Unit
}