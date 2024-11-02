package com.pandulapeter.kubriko.actor.traits

import com.pandulapeter.kubriko.actor.Actor

/**
 * Signalling interface that ensures that [Actor]s implementing it can only be added once to the Scene.
 * Adding a second [Unique] [Actor] instance of the same type will have the side effect of removing the first one.
 */
interface Unique : Actor