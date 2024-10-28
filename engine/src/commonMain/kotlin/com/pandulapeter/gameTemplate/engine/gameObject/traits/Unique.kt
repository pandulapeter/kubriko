package com.pandulapeter.gameTemplate.engine.gameObject.traits

import com.pandulapeter.gameTemplate.engine.gameObject.Trait

data object Unique : Trait<Unique> {

    override val typeId = "unique"

    override fun deserialize(json: String) = Unique

    override fun serialize() = ""
}