package com.pandulapeter.gameTemplate.engine.gameObject.traits

import androidx.compose.ui.graphics.Color
import com.pandulapeter.gameTemplate.engine.gameObject.Serializer
import com.pandulapeter.gameTemplate.engine.gameObject.Trait
import com.pandulapeter.gameTemplate.engine.gameObject.editor.VisibleInEditor
import com.pandulapeter.gameTemplate.engine.implementation.serializers.SerializableColor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@VisibleInEditor(typeId = "colorful")
class Colorful(
    @VisibleInEditor(typeId = "color") var color: SerializableColor = Color.Gray,
) : Trait<Colorful>() {

    private constructor(state: State) : this(
        color = state.color,
    )

    override fun getSerializer(): Serializer<Colorful> = State(
        colorful = this,
    )

    @Serializable
    private data class State(
        @SerialName("color") val color: SerializableColor = Color.Gray,
    ) : Serializer<Colorful> {

        constructor(colorful: Colorful) : this(
            color = colorful.color,
        )

        override val typeId = "colorful"

        override fun instantiate() = Colorful(
            state = this,
        )

        override fun serialize() = Json.encodeToString(this)
    }
}