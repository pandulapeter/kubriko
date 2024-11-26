package com.pandulapeter.kubriko.actor

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.manager.ActorManager

/**
 * Marker interface that should be implemented by all classes handled by [ActorManager].
 */
interface Actor {

    // TODO: Documentation
    fun onAdded(kubriko: Kubriko) = Unit

    // TODO: Documentation
    fun onRemoved() = Unit
}