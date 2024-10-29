package com.pandulapeter.gameTemplate.engine.gameObject.editor

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class Editable(
    val typeId: String,
    val category: String = "",
)