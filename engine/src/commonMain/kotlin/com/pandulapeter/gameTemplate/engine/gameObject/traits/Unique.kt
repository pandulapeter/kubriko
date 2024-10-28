package com.pandulapeter.gameTemplate.engine.gameObject.traits

import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.Trait

class Unique : Trait<Unique>(), Serializer<Unique> {

    override val typeId = "unique"

    override fun instantiate() = Unique()

    override fun serialize() = ""

    override fun getSerializer(): Serializer<Unique> = this
}