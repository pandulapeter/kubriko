package com.pandulapeter.kubriko.engine.editorIntegration

// TODO: Could make it work for constructor parameters as well
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class EditableProperty(
    val name: String,
)