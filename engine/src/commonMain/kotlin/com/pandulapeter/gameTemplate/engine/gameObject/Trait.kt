package com.pandulapeter.gameTemplate.engine.gameObject

interface Trait<T: Trait<T>> {

    val typeId: String

    fun deserialize(json: String): T

    fun serialize(): String
}