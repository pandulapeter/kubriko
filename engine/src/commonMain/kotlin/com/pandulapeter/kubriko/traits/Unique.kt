package com.pandulapeter.kubriko.traits

/**
 * Signalling interface that ensures that Actors implementing it can only be added once to the Scene.
 * Trying to add a second [Unique] instance of the same type will have the side effect of removing the first one.
 */
interface Unique