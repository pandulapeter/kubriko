package com.pandulapeter.kubriko.sceneEditor

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.actor.traits.Positionable
import com.pandulapeter.kubriko.actor.traits.Visible
import com.pandulapeter.kubriko.serialization.Serializable


// TODO: Revisit documentation.
/**
 * Should be implemented by [Actor]s that want to appear in the Scene Editor.
 * It's important that such types should also be registered as [EditableMetadata] when instantiating [Kubriko].
 * The main point of this interface is to enforce a serialization pattern (the deserialization logic is defined in [EditableMetadata]), so that
 * the Scene Editor can save and load instance [State]-s from text files. This [State] can then be used to restore the [Editable] instance.
 * Actors that appear in the Scene Editor must be [Positionable] so that they can be placed into the Scene, and they are usually [Visible] too, however the latter is not enforced.
 */
// TODO: Positionable could not be a requirement. The Editor could support invisible Actors separately
interface Editable<T : Editable<T>> : Serializable<T>, Positionable