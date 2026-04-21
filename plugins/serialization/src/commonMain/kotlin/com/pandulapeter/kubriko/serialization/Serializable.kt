/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.serialization

import com.pandulapeter.kubriko.actor.Actor


/**
 * An interface for [Actor]s that can be serialized and deserialized.
 *
 * Actors implementing this interface must be registered using [SerializableMetadata] when
 * instantiating the [SerializationManager].
 *
 * @param T The type of the serializable actor.
 */
interface Serializable<T : Serializable<T>> : Actor {

    /**
     * Saves the current state of the actor.
     *
     * @return A [State] object containing all necessary data to restore the actor.
     */
    fun save(): State<T>

    /**
     * Represents the serializable state of an [Actor] instance.
     *
     * Implementations should define properties for each important aspect of an [Actor] in a way
     * that those properties can be saved to and restored from a [String].
     */
    interface State<T : Serializable<T>> {

        /**
         * Instantiates an [Actor] from the current state.
         *
         * @return A new instance of the actor [T].
         */
        fun restore(): T

        /**
         * Serializes the current state into a string representation.
         *
         * @return A string containing the serialized state.
         */
        fun serialize(): String
    }
}
