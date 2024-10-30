package com.pandulapeter.gameTemplate.engine.gameObject

interface EditorState<T> {

    val typeId: String

    fun restore(): T

    fun serialize(): String
}