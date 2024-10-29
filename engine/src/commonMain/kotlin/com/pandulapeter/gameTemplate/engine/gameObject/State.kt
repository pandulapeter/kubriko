package com.pandulapeter.gameTemplate.engine.gameObject

interface State<T> {

    val typeId: String

    fun restore(): T

    fun serialize(): String
}