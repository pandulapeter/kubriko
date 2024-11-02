package com.pandulapeter.kubriko.engine.traits

import com.pandulapeter.kubriko.engine.Kubriko
import com.pandulapeter.kubriko.engine.editorIntegration.EditableMetadata


/**
 * Should be implemented by Actors who want to appear in the Editor.
 * It's important that such types should also be registered as [EditableMetadata] when instantiating [Kubriko].
 * The main point of this interface is to enforce a serialization pattern (the deserialization logic is defined in [EditableMetadata]), so that
 * the Editor can save and load instance [State]-s from text files. This [State] can then be used to restore the [Editable] instance.
 * Actors that appear in the Editor must be [Positionable] so that they can be placed into the Scene, and they are usually [Visible] too, however the latter is not enforced.
 * If an [Editable] Actor is not [Visible], the Editor will create a default representation for it (that's only visible in the Editor). Use [editorPreview] to override this representation.
 */
interface Editable<T : Editable<T>> : Positionable {

    /**
     * The appearance of the Actor in the Editor.
     */
    val editorPreview: Visible get() = this as? Visible ?: throw IllegalStateException("EditorPreview must be configured") // TODO: Default should come from the editor module

    /**
     * Invoked when saving the state of the Actor instance.
     */
    fun save(): State<T>

    /**
     * Represents the serializable state of an Actor instance.
     * Implementations should define properties for each important aspect of an Actor in a way that those properties can be saved to and restored from a [String].
     * See [EditableMetadata] for more information.
     */
    interface State<T> {

        /**
         * Instantiates an [Editable] Actor from the current [State].
         */
        fun restore(): T

        /**
         * Serializes the current [State] into a [String].
         * The deserialization logic is defined using [EditableMetadata].
         */
        fun serialize(): String
    }
}