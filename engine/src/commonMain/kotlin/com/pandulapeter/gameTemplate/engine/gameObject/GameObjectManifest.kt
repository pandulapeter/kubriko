package com.pandulapeter.gameTemplate.engine.gameObject

interface GameObjectManifest {

    fun getCreator(wrapper: GameObjectStateWrapper): GameObjectCreator<*>
}