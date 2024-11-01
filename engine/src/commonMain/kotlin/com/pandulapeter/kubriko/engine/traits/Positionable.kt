package com.pandulapeter.kubriko.engine.traits

import com.pandulapeter.kubriko.engine.types.Scale
import com.pandulapeter.kubriko.engine.types.WorldCoordinates
import com.pandulapeter.kubriko.engine.types.WorldSize

interface Positionable {
    val boundingBox: WorldSize
    val pivotOffset: WorldCoordinates get() = boundingBox.center
    var position: WorldCoordinates
    val scale: Scale get() = Scale.Unit
}