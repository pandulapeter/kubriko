package com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: David Hoskins
 * https://www.shadertoy.com/view/4tjSDt
 */
internal class WarpShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<WarpShader.State>, Dynamic {
    override var state = initialState
        private set
    override val cache = Shader.Cache()
    override val code = CODE
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMillis: Float) {
        state = state.copy(time = (metadataManager.activeRuntimeInMilliseconds.value % 100000L) / 1000f)
    }

    fun updateState(state: State) {
        this.state = state.copy(time = this.state.time)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 58f,
        val light: Int = 65,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
            uniform(SPEED, speed)
            uniform(LIGHT, light)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
        private const val LIGHT = "light"
        const val CODE = """
// Credit: David Hoskins
// https://www.shadertoy.com/view/4tjSDt

uniform float2 ${Shader.RESOLUTION};
uniform float $TIME;
uniform float $SPEED;
uniform int $LIGHT;

vec4 main(in float2 fragCoord) {
    float s = 0.0, v = 0.0;
    vec2 uv = (fragCoord / ${Shader.RESOLUTION}.xy) * 2.0 - 1.;
    float time = ($TIME-2.0)*$SPEED;
    vec3 col = vec3(0);
    vec3 init = vec3(sin(time * .0032)*.3, .35 - cos(time * .005)*.3, time * 0.002);
    for (int r = 0; r < 100; r++) {
        if (r > $LIGHT) {
            break;
            }
        vec3 p = init + s * vec3(uv, 0.05);
        p.z = fract(p.z);
        for (int i=0; i < 9; i++)	p = abs(p * 2.04) / dot(p, p) - .9;
        v += pow(dot(p, p), .7) * .06;
        col +=  vec3(v * 0.2+.4, 12.-s*2., .1 + v * 1.) * v * 0.00003;
        s += .025;
    }
    return vec4(clamp(col, 0.0, 1.0), 1.0);
}
"""
    }
}