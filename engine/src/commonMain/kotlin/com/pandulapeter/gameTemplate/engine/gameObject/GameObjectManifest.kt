package com.pandulapeter.gameTemplate.engine.gameObject

interface GameObjectManifest {

    fun deserializeState(wrapper: GameObjectStateWrapper): GameObject.State<*>
}