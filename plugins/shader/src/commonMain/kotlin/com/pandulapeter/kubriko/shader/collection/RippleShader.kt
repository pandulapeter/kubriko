package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class RippleShader(
    initialState: State = State(),
    override val canvasIndex: Int? = null,
) : Shader<RippleShader.State> {
    private val _state = MutableStateFlow(initialState)
    override val state = _state.asStateFlow()
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform float $TIME;
    uniform shader ${ShaderManager.CONTENT};
    
    half4 main(float2 fragCoord) {
        float scale = 1 / ${ShaderManager.RESOLUTION}.x;
        float2 scaledCoord = fragCoord * scale;
        float2 center = ${ShaderManager.RESOLUTION} * 0.5 * scale;
        float dist = distance(scaledCoord, center);
        float2 dir = scaledCoord - center;
        float sin = sin(dist * 70 - $TIME * 6.28);
        float2 offset = dir * sin;
        float2 textCoord = scaledCoord + offset / 30;
        return ${ShaderManager.CONTENT}.eval(textCoord / scale);
    }
""".trimIndent()


    data class State(
        val time: Float = 0f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
        }
    }

    companion object {
        private const val TIME = "time"
    }
}