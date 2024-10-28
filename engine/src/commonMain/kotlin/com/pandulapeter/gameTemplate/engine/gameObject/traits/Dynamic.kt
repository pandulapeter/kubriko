package com.pandulapeter.gameTemplate.engine.gameObject.traits

import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.Trait

class Dynamic(
    updater: ((Float) -> Unit)? = null,
) : Trait<Dynamic>(), Serializer<Dynamic> {

    override val typeId = "dynamic"
    private val allUpdaters = mutableListOf<(Float) -> Unit>().apply {
        updater?.let(::add)
    }

    fun registerUpdater(updater: (Float) -> Unit) {
        allUpdaters.add(updater)
    }

    fun update(deltaTimeInMillis: Float) = allUpdaters.forEach { it(deltaTimeInMillis) }

    override fun instantiate() = Dynamic()

    override fun serialize() = ""

    override fun getSerializer(): Serializer<Dynamic> = this
}