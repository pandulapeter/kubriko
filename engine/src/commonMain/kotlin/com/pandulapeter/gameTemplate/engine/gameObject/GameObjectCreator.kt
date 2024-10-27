package com.pandulapeter.gameTemplate.engine.gameObject

interface GameObjectCreator<T: GameObject> {

    fun instantiate() : T
}