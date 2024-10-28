package com.pandulapeter.gameTemplate.engine.gameObject.traits

import com.pandulapeter.gameTemplate.engine.gameObject.Trait

data class Dynamic(
    private val updater: Dynamic.(Float) -> Unit = {},
) : Trait<Dynamic> {

    private var allUpdaters = listOf(updater)

    fun registerUpdater(updater: Dynamic.(Float) -> Unit) {
        allUpdaters = allUpdaters + updater
    }

    fun update(deltaTimeInMillis: Float) {
        allUpdaters.forEach { it(deltaTimeInMillis) }
    }

    override val typeId = "dynamic"

    override fun deserialize(json: String) = Dynamic()

    override fun serialize() = ""
}