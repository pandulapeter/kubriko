package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor

/**
 * TODO
 */
interface Group : Actor {

    val actors: List<Actor>
}