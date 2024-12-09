package com.pandulapeter.kubriko.serialization.integration

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.serialization.integration.Serializable.State


// TODO: Revisit documentation.
/**
 * Should be implemented by [Actor]s that want to appear in the Scene Editor.
 * It's important that such types should also be registered as [SerializableMetadata] when instantiating [Kubriko].
 * The main point of this interface is to enforce a serialization pattern (the deserialization logic is defined in [SerializableMetadata]), so that
 * the Scene Editor can save and load instance [State]-s from text files. This [State] can then be used to restore the [Serializable] instance.
 * Actors that appear in the Scene Editor must be [Positionable] so that they can be placed into the Scene, and they are usually [Visible] too, however the latter is not enforced.
 * If an [Serializable] Actor is not [Visible], the Scene Editor will create a default representation for it (that's only visible in the Editor). Use [editorPreview] to override this representation.
 */
interface Serializable<T : Serializable<T>> : Actor {

    /**
     * Invoked when saving the state of the [Actor] instance.
     */
    fun save(): State<T>

    /**
     * Represents the serializable state of an [Actor] instance.
     * Implementations should define properties for each important aspect of an [Actor] in a way that those properties can be saved to and restored from a [String].
     * See [SerializableMetadata] for more information.
     */
    interface State<T: Serializable<T>> {

        /**
         * Instantiates an [Serializable] [Actor] from the current [State].
         */
        fun restore(): T

        /**
         * Serializes the current [State] into a [String].
         * The deserialization logic is defined using [SerializableMetadata].
         */
        fun serialize(): String
    }
}