package com.pandulapeter.gameTemplate.engine.gameObject

// TODO: Interface
abstract class GameObject<T : GameObject<T>> {
    abstract val traits: Set<Trait<*>>
    var isSelectedInEditor = false

    abstract fun getState(): Serializer<T>

    interface Serializer<T : GameObject<T>> {

        val typeId: String

        fun instantiate(): T

        fun serialize(): String
    }
}