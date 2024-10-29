package com.pandulapeter.gameTemplate.engine.gameObject

import kotlin.reflect.KClass

abstract class GameObject<O : GameObject<O>>(
    vararg traits: O.() -> Trait<*>,
) {
    @Suppress("UNCHECKED_CAST")
    val allTraits: Map<KClass<out Trait<*>>, Trait<*>> by lazy {
        traits
            .map { traitGetter -> traitGetter(this as O).also { it.initialize(gameObject = this) } }
            .associateBy { it::class }
    }

    init {
        this.allTraits.values.forEach { it.initialize() }
    }

    abstract fun getSerializer(): Serializer<O>

}