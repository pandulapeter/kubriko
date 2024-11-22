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

class GradientShader(
    initialState: State = State(),
    override val canvasIndex: Int? = null,
) : Shader<GradientShader.State>, Dynamic {
    private val _state = MutableStateFlow(initialState)
    override val state = _state.asStateFlow()
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform shader ${ShaderManager.CONTENT};
    uniform float $TIME;
    uniform float $SPEED;
    
    float4 main(float2 fragCoord) {
        // Normalized pixel coordinates (from 0 to 1)
        float2 uv = fragCoord/${ShaderManager.RESOLUTION}.xy;

        // Time varying pixel color
        float3 col = 0.8 + 0.2 * cos($TIME*$SPEED+uv.xxx*2.0+float3(1,2,4));

        // Output to screen
        return float4(col,1.0);
    }
""".trimIndent()
    private lateinit var metadataManager: MetadataManager

    override fun onAdd(kubriko: Kubriko) {
        metadataManager = kubriko.require()
    }

    override fun update(deltaTimeInMillis: Float) = _state.update { currentValue ->
        currentValue.copy(time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f)
    }

    fun updateState(state: State) = _state.update { currentValue ->
        state.copy(time = currentValue.time)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 2f,
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