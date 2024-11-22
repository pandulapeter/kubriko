package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ChromaticAberrationShader(
    initialState: State = State(),
    override val canvasIndex: Int? = null,
) : Shader<ChromaticAberrationShader.State> {
    override val cache = Shader.Cache()
    private val _state = MutableStateFlow(initialState)
    override val state = _state.asStateFlow()
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform float $INTENSITY;
    uniform shader ${ShaderManager.CONTENT}; 

    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / ${ShaderManager.RESOLUTION}.xy;
        half4 color = ${ShaderManager.CONTENT}.eval(fragCoord);
        vec2 offset = $INTENSITY / ${ShaderManager.RESOLUTION}.xy;
        color.r = ${ShaderManager.CONTENT}.eval(${ShaderManager.RESOLUTION}.xy * ((uv - 0.5) * (1.0 + offset) + 0.5)).r;
        color.b = ${ShaderManager.CONTENT}.eval(${ShaderManager.RESOLUTION}.xy * ((uv - 0.5) * (1.0 - offset) + 0.5)).b;
        return color; 
    }
""".trimIndent()

    data class State(
        val intensity: Float = 20f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(INTENSITY, intensity)
        }
    }

    companion object {
        private const val INTENSITY = "intensity"
    }
}