package com.pandulapeter.kubriko.sceneEditor

import com.pandulapeter.kubriko.actor.Actor


/**
 * Use this annotation on property setters of [Editable] [Actor]s to expose those properties to the Scene Editor.
 * @param name - The name the Scene Editor will display on its UI for the property
 */
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class Exposed(
    val name: String,
)