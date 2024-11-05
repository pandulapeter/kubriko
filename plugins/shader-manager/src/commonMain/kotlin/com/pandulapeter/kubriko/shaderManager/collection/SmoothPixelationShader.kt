package com.pandulapeter.kubriko.shaderManager.collection

import com.pandulapeter.kubriko.shaderManager.Shader
import com.pandulapeter.kubriko.shaderManager.implementation.extensions.ShaderUniformProvider

class SmoothPixelationShader : Shader {

    // TODO: Changing this value should trigger an update
    private var pixelSize = 2f

    override val shaderCode = """
    uniform float2 resolution;
    uniform shader content; 
    uniform float pixelSize;

    vec4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / resolution.xy;
        float factor = (abs(sin( resolution.y * (uv.y - 0.5) / pixelSize)) + abs(sin( resolution.x * (uv.x - 0.5) / pixelSize))) / 2.0;
        half4 color = content.eval(fragCoord);
        return half4(factor * color.rgb, color.a); 
    }
""".trimIndent()

    override val uniformsBlock: ShaderUniformProvider.() -> Unit = {
        uniform("pixelSize", pixelSize)
    }
}