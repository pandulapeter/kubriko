package com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

/**
 * Credit: nimitz
 * https://www.shadertoy.com/view/MsjSW3
 */
internal class EtherShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<EtherShader.State>, Dynamic {
    override var state = initialState
        private set
    override val cache = Shader.Cache()
    override val code = CODE
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMillis: Float) {
        state = state.copy(time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f)
    }

    fun updateState(state: State) {
        this.state = state.copy(time = this.state.time)
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
        const val CODE = """
// Credit: nimitz
// https://www.shadertoy.com/view/MsjSW3

uniform float2 ${Shader.RESOLUTION};
uniform float $TIME;
uniform float $SPEED;

mat2 m(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, -s, s, c);
}

float map(vec3 p) {
    float t = $TIME * $SPEED;
    p.xz *= m(t * 0.4);
    p.xy *= m(t * 0.3);
    vec3 q = p * 2.0 + t;
    return length(p + vec3(sin(t * 0.7))) * log(length(p) + 1.0) +
           sin(q.x + sin(q.z + sin(q.y))) * 0.5 - 1.0;
}

vec4 main(in vec2 fragCoord) {
    vec2 p = fragCoord.xy / ${Shader.RESOLUTION}.y - vec2(0.9, 0.5);
    vec3 cl = vec3(0.0);
    float d = 2.5;

    for (int i = 0; i <= 5; i++) {
        vec3 pos = vec3(0.0, 0.0, 5.0) + normalize(vec3(p, -1.0)) * d;
        float rz = map(pos);
        float f = clamp((rz - map(pos + 0.1)) * 0.5, -0.1, 1.0);
        vec3 l = vec3(0.1, 0.3, 0.4) + vec3(5.0, 2.5, 3.0) * f;
        cl = cl * l + smoothstep(2.5, 0.0, rz) * 0.7 * l;
        d += min(rz, 1.0);
    }

    return vec4(cl, 1.0);
}
"""
    }
}