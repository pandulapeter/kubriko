package com.pandulapeter.kubriko.engine.traits

import com.pandulapeter.kubriko.engine.Kubriko

/**
 * Should be implemented by Actors who want to have a unique ID for each instance.
 * [Kubriko] will not allow to instances with the same [id] to be added to the scene simultaneously.
 * However, if this is the only reason for implementing this interface, take a look at the [Unique] trait instead.
 * [Identifiable] is meant for Actors who want to be referenced by other classes in a more abstract way.
 */
interface Identifiable {

    /**
     * The unique identifier of the instance.
     * The value will never be null after the Actor is added to the Scene.
     * If implementations provide a {null} [id], [Kubriko] will set a random-generated value.
     */
    var id: String?
}