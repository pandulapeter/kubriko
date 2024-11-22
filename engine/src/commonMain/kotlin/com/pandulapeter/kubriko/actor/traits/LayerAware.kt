package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor

interface LayerAware : Actor {

    val layerIndex: Int? get() = null
}