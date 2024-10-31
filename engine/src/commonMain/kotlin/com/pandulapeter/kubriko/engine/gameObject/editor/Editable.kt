package com.pandulapeter.kubriko.engine.gameObject.editor

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class Editable(
    val name: String,
)