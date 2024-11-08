package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor

interface CanvasAware : Actor {

    val canvasIndex: Int? get() = null
}