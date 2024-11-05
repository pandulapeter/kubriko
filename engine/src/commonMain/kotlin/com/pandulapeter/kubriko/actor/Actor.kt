package com.pandulapeter.kubriko.actor

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager

/**
 * Marker interface that should be implemented by all classes handled by [ActorManager].
 */
interface Actor {

    // TODO: Documentation
    fun onAdd(kubriko: Kubriko) = Unit

    // TODO: Documentation
    fun onRemove() = Unit
}