package com.pandulapeter.gameTemplate.engine.gameObject

import kotlinx.serialization.Serializable

@Serializable
data class GameObjectStateWrapper(
    val typeId: String,
    val state: String,
)