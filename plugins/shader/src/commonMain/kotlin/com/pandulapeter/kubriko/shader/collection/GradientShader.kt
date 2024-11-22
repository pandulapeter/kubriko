package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GradientShader(
    initialState: State = State(),
    override val canvasIndex: Int? = null,
) : Shader<GradientShader.State> {
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