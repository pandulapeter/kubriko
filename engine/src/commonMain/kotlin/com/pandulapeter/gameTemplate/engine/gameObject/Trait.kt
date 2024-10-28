package com.pandulapeter.gameTemplate.engine.gameObject

abstract class Trait<T : Trait<T>> {
    lateinit var gameObject: GameObject<*>
        internal set

    open fun initialize() = Unit

    abstract fun getSerializer(): Serializer<T>
}