package com.pandulapeter.kubriko.traits

import com.pandulapeter.kubriko.Kubriko

/**
 * Should be implemented by Actors who want to have a [name] set for their instances.
 * [Kubriko] does NOT enforce these [name]-s to be unique. If you want "singleton" instances, take a look at the [Unique] trait instead.
 * [Identifiable] is meant for Actors who want to be referenced by other classes in a more abstract way. Searching for Actor by name might return multiple results.
 */
interface Identifiable {

    /**
     * The name of the Actor instance.
     * If implementations provide a {null} [name], [Kubriko] will set a random-generated value when the Actor is added to the Scene.
     * The engine will not change the value during runtime.
     */
    var name: String?
}