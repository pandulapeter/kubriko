package com.pandulapeter.gameTemplate.engine.gameObject.editor

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY_SETTER)
annotation class VisibleInEditor(
    val typeId: String,
)