/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders.collection

import com.pandulapeter.kubriko.shaders.ContentShader
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Special, hacky shader.
 */
open class BlurShader(
    override var shaderState: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<BlurShader.State> {
    final override val shaderCache = Shader.Cache()
    final override val shaderCode = CODE

    data class State(
        val blurHorizontal: Float = 20f,
        val blurVertical: Float = 20f,
        val mode: Mode = Mode.CLAMP,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() = Unit
    }

    enum class Mode {
        CLAMP,
        REPEAT,
        MIRROR,
        DECAL
    }

    companion object {
        // Not used. The BlurShader is an exception that's handled differently compared to other shaders.
        private const val CODE = """
half4 main(vec2 fragCoord) {
    return half4(0);
}
"""
    }
}