/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoShaderAnimations.implementation.shaders

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: Yutaka Sato
 * https://x.com/notargs/status/1250468645030858753
 */
internal class NoodleShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<NoodleShader.State>, Dynamic {
    override var shaderState = initialState
        private set
    override val shaderCache = Shader.Cache()
    override val shaderCode = CODE
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Float) {
        shaderState = shaderState.copy(time = (metadataManager.activeRuntimeInMilliseconds.value % 100000L) / 1000f)
    }

    fun updateState(state: State) {
        this.shaderState = state.copy(time = this.shaderState.time)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 2f,
        val red: Int = 4,
        val green: Int = 4,
        val blue: Int = 18,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
            uniform(SPEED, speed)
            uniform(RED, red)
            uniform(GREEN, green)
            uniform(BLUE, blue)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
        private const val RED = "red"
        private const val GREEN = "green"
        private const val BLUE = "blue"
        const val CODE = """
// Credit: Yutaka Sato
// https://x.com/notargs/status/1250468645030858753

uniform float2 ${Shader.RESOLUTION};
uniform float $TIME;
uniform float $SPEED;
uniform int $RED;
uniform int $GREEN;
uniform int $BLUE;

float f(float3 p) {
    p.z -= $TIME * $SPEED;
    float a = p.z * .1;
    p.xy *= mat2(cos(a), sin(a), -sin(a), cos(a));
    return .1 - length(cos(p.xy) + sin(p.yz));
}

vec4 main(float2 fragCoord) { 
    float3 d = .5 - fragCoord.xy1 / ${Shader.RESOLUTION}.y;
    float3 p=float3(0);
    for (int i = 0; i < 32; i++) {
      p += f(p) * d;
    }
    return ((sin(p) + float3($RED, $GREEN, $BLUE)) / length(p)).xyz1;
}
"""
    }
}