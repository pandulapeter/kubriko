package com.pandulapeter.kubriko.sceneEditor

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.sceneSerializer.integration.Serializable


// TODO: Revisit documentation.
/**
 * Should be implemented by [Actor]s that want to appear in the Scene Editor.
 * It's important that such types should also be registered as [EditableMetadata] when instantiating [Kubriko].
 * The main point of this interface is to enforce a serialization pattern (the deserialization logic is defined in [EditableMetadata]), so that
 * the Scene Editor can save and load instance [State]-s from text files. This [State] can then be used to restore the [Editable] instance.
 * Actors that appear in the Scene Editor must be [Positionable] so that they can be placed into the Scene, and they are usually [Visible] too, however the latter is not enforced.
 * If an [Editable] Actor is not [Visible], the Scene Editor will create a default representation for it (that's only visible in the Editor). Use [editorPreview] to override this representation.
 */
interface Editable<T : Editable<T>> : Serializable<T>, Positionable, Actor {

    /**
     * The appearance of the [Actor] in the Editor.
     */
    val editorPreview: Visible get() = this as? Visible ?: throw IllegalStateException("EditorPreview must be configured") // TODO: Default should come from the editor module
}