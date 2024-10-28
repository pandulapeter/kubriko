package com.pandulapeter.gameTemplate.engine.gameObject

// TODO: Interface
abstract class GameObject<T : GameObject<T>> {
    abstract val traits: Set<Trait<*>> // TODO: Should not be a Set
    var isSelectedInEditor = false

    abstract fun getSerializer(): Serializer<T>

}