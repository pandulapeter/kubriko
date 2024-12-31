package com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

internal class GradientShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<GradientShader.State>, Dynamic {
    override var state = initialState
        private set
    override val cache = Shader.Cache()
    override val code = CODE
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        state = state.copy(time = (metadataManager.activeRuntimeInMilliseconds.value % 100000L) / 1000f)
    }

    fun updateState(state: State) {
        this.state = state.copy(time = this.state.time)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 2f,
        val dark: Float = 0.2f,
        val frequency: Float = 2.0f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
            uniform(SPEED, speed)
            uniform(DARK, dark)
            uniform(FREQUENCY, frequency)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
        private const val DARK = "dark"
        private const val FREQUENCY = "frequency"
        const val CODE = """
uniform float2 ${Shader.RESOLUTION};
uniform float $TIME;
uniform float $SPEED;
uniform float $DARK;
uniform float $FREQUENCY;

vec4 main(float2 fragCoord) {
    float2 uv = fragCoord / ${Shader.RESOLUTION}.xy;
    float3 color = (1.0 - $DARK) + $DARK * sin($TIME * $SPEED + uv.xyx * $FREQUENCY + float3(1,2,4));
    return color.xyz1;
}
"""
    }
}