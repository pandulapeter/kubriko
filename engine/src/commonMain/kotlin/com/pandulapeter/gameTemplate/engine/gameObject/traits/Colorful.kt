package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Colorful(
    @SerialName("color") var color: SerializableColor = Color.Gray,
) : Trait<Colorful> {

    override val typeId = "visible"

    override fun deserialize(json: String) = Json.decodeFromString<Colorful>(json)

    override fun serialize() = Json.encodeToString(this)
}