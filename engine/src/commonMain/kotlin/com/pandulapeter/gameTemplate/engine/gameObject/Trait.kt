package com.pandulapeter.gameTemplate.engine.gameObject

interface Trait<T: Trait<T>> {

    fun getSerializer(): Serializer<T>
}