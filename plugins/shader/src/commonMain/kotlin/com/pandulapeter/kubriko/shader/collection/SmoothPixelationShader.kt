package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SmoothPixelationShader(
    initialState: State = State(),
    override val canvasIndex: Int? = null,
) : Shader<SmoothPixelationShader.State> {
    private val _state = MutableStateFlow(initialState)
    override val state = _state.asStateFlow()
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform shader ${ShaderManager.CONTENT}; 
    uniform float $PIXEL_SIZE;

    vec4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / ${ShaderManager.RESOLUTION}.xy;
        float factor = (abs(sin(${ShaderManager.RESOLUTION}.y * (uv.y - 0.5) / $PIXEL_SIZE)) + abs(sin(${ShaderManager.RESOLUTION}.x * (uv.x - 0.5) / $PIXEL_SIZE))) / 2.0;
        half4 color = ${ShaderManager.CONTENT}.eval(fragCoord);
        return half4(factor * color.rgb, color.a); 
    }
""".trimIndent()

    data class State(
        val pixelSize: Float = 2f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(PIXEL_SIZE, pixelSize)
        }
    }

    companion object {
        private const val PIXEL_SIZE = "pixelSize"
    }
}