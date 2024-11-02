package com.pandulapeter.kubriko.sceneEditor

import com.pandulapeter.kubriko.actor.Actor
import com.pandulapeter.kubriko.sceneSerializer.Editable


/**
 * Use this annotation on property setters of [Editable] [Actor]s to expose those properties to the Scene Editor.
 * @param name - The name the Scene Editor will display on its UI for the property
 */
// TODO: Could make it work for constructor parameters as well
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class EditableProperty(
    val name: String,
)