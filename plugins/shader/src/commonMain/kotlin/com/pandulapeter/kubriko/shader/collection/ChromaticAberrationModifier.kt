package com.pandulapeter.kubriko.shader.collection

import androidx.compose.ui.Modifier
import com.pandulapeter.kubriko.shader.runtimeShader

private val shader = """
    uniform float2 resolution;
    uniform float intensity;
    uniform shader content; 

    half4 main(vec2 fragCoord) {
        vec2 uv = fragCoord.xy / resolution.xy;
        half4 color = content.eval(fragCoord);
        vec2 offset = intensity / resolution.xy;
        color.r = content.eval(resolution.xy * ((uv - 0.5) * (1.0 + offset) + 0.5)).r;
        color.b = content.eval(resolution.xy * ((uv - 0.5) * (1.0 - offset) + 0.5)).b;
        return color; 
    }
""".trimIndent()

fun Modifier.chromaticAberrationShader(
    intensity: Float,
): Modifier = this then runtimeShader(shader) {
    uniform("intensity", intensity)
}