package com.pandulapeter.kubriko.engine.editorIntegration

import com.pandulapeter.kubriko.engine.traits.Editable

/**
 * Use this annotation on property setters of [Editable] Actors to expose those properties to the Scene Editor.
 * @param name - The name the Scene Editor will display on its UI for the property
 */
// TODO: Could make it work for constructor parameters as well
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class EditableProperty(
    val name: String,
)