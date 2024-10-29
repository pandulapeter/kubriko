package com.pandulapeter.gameTemplate.engine.gameObject

abstract class Trait<T : Trait<T>> {
    protected lateinit var gameObject: GameObject<*>
        private set

    internal fun initialize(gameObject: GameObject<*>) {
        this.gameObject = gameObject
    }

    open fun initialize() = Unit

    abstract fun getSerializer(): Serializer<T>
}