package com.pandulapeter.gameTemplate.engine.gameObject

import kotlin.reflect.KClass

abstract class GameObject<T : GameObject<T>> {

    lateinit var traits: Map<KClass<out Trait<*>>, Trait<*>>
        private set

    protected fun registerTraits(vararg traits: Trait<*>) {
        this.traits = traits.sortedBy { it::class.simpleName }.associateBy { it::class }
        traits.forEach { it.gameObject = this }
        traits.forEach { it.initialize() }
    }

    abstract fun getSerializer(): Serializer<T>

}