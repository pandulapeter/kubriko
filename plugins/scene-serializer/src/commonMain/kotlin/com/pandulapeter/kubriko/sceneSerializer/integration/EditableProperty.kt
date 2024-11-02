package com.pandulapeter.kubriko.sceneSerializer.integration

import com.pandulapeter.kubriko.sceneSerializer.Editable


/**
 * Use this annotation on property setters of [Editable] Actors to expose those properties to the Scene Editor.
 * @param name - The name the Scene Editor will display on its UI for the property
 */
// TODO: Could make it work for constructor parameters as well
// TODO: Move to the sceneEditor module
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class EditableProperty(
    val name: String,
)