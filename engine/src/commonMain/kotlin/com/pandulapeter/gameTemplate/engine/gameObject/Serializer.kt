package com.pandulapeter.gameTemplate.engine.gameObject

interface Serializer<T> {

    val typeId: String

    fun instantiate(): T

    fun serialize(): String
}