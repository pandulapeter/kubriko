package com.pandulapeter.kubriko.engine.actor.editor

// TODO: Could make it work for constructor parameters as well
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class EditableProperty(
    val name: String,
)