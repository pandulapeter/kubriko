package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.body.PointBody

/**
 * [Actor]s that want to have a well defined position in the Scene should implement this interface.
 */
interface Positionable : Actor {

    val body: PointBody
}