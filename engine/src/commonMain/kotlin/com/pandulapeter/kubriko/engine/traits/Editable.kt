package com.pandulapeter.kubriko.engine.traits

interface Editable<T : Editable<T>> : Positionable {

    val editorPreview: Visible get() = this as? Visible ?: throw IllegalStateException("EditorPreview must be configured") // TODO: Add default

    fun save(): State<T>

    interface State<T> {

        fun restore(): T

        fun serialize(): String
    }
}