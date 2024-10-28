package com.pandulapeter.gameTemplate.engine.gameObject

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameObjectStateWrapper(
    @SerialName("typeId") val typeId: String,
    @SerialName("state") val serializedState: String,
)