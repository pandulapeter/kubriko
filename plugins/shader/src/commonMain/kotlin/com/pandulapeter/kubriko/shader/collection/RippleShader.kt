package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.require
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RippleShader(
    initialState: State = State(),
    override val canvasIndex: Int? = null,
) : Shader<RippleShader.State>, Dynamic {
    override val cache = Shader.Cache()
    private val _state = MutableStateFlow(initialState)
    override val state = _state.asStateFlow()
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform shader ${ShaderManager.CONTENT};
    uniform float $TIME;
    uniform float $SPEED;
    
    half4 main(float2 fragCoord) {
        float scale = 1 / ${ShaderManager.RESOLUTION}.x;
        float2 scaledCoord = fragCoord * scale;
        float2 center = ${ShaderManager.RESOLUTION} * 0.5 * scale;
        float dist = distance(scaledCoord, center);
        float2 dir = scaledCoord - center;
        float sin = sin(dist * 70 - $TIME * $SPEED);
        float2 offset = dir * sin;
        float2 textCoord = scaledCoord + offset / 30;
        return ${ShaderManager.CONTENT}.eval(textCoord / scale);
    }
""".trimIndent()
    private lateinit var metadataManager: MetadataManager

    override fun onAdd(kubriko: Kubriko) {
        metadataManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) = _state.update { currentValue ->
        currentValue.copy(time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 6.28f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
            uniform(SPEED, speed)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
    }
}