package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.actor.traits.Shader
import com.pandulapeter.kubriko.implementation.extensions.ShaderUniformProvider

class VignetteShader : Shader {

    // TODO: Changing these values should trigger an update
    private var intensity = 30f
    private var decayFactor = 0.6f

    override val shaderCode = """
    uniform float2 resolution;
    uniform shader content; 
    uniform float intensity;
    uniform float decayFactor;

    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / resolution.xy;
        half4 color = content.eval(fragCoord);
        uv *=  1.0 - uv.yx;
        float vig = clamp(uv.x*uv.y * intensity, 0., 1.);
        vig = pow(vig, decayFactor);
        return half4(vig * color.rgb, color.a);
    }
""".trimIndent()

    override val uniformsBlock: ShaderUniformProvider.() -> Unit = {
        uniform("intensity", intensity)
        uniform("decayFactor", decayFactor)
    }
}