package com.pandulapeter.kubriko.shaders.collection

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: Juraj Kusnier
 * https://github.com/jurajkusnier/agsl-fun
 */
class RippleShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<RippleShader.State>, Dynamic {
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

    data class State(
        val time: Float = 0f,
        val speed: Float = 6.28f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
            uniform(SPEED, speed)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
        private const val CODE = """
uniform float2 ${Shader.RESOLUTION};
uniform shader ${ContentShader.CONTENT};
uniform float $TIME;
uniform float $SPEED;

half4 main(float2 fragCoord) {
    float scale = 1 / ${Shader.RESOLUTION}.x;
    float2 scaledCoord = fragCoord * scale;
    float2 center = ${Shader.RESOLUTION} * 0.5 * scale;
    float dist = distance(scaledCoord, center);
    float2 dir = scaledCoord - center;
    float sin = sin(dist * 70 - $TIME * $SPEED);
    float2 offset = dir * sin;
    float2 textCoord = scaledCoord + offset / 30;
    return ${ContentShader.CONTENT}.eval(textCoord / scale);
}
"""
    }
}