package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableOffset
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableSize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Visible(
    @SerialName("bounds") var bounds: SerializableSize = Size.Zero,
    @SerialName("pivot") var pivot: SerializableOffset = bounds.center,
    @SerialName("position") var position: SerializableOffset = Offset.Zero,
    @SerialName("scale") var scale: SerializableSize = Size(1f, 1f),
    @SerialName("rotationDegrees") var rotationDegrees: Float = 1f,
    @SerialName("depth") var depth: Float = 0f,
    @Transient val draw: Visible.(DrawScope) -> Unit = {},
) : Trait<Visible> {

    override val typeId = "visible"

    override fun deserialize(json: String) = Json.decodeFromString<Visible>(json)

    override fun serialize() = Json.encodeToString(this)
}