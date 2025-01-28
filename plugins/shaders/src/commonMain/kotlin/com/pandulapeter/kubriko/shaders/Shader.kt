/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders

import com.pandulapeter.kubriko.actor.traits.LayerAware
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

//TODO: Documentation
interface Shader<T : Shader.State> : LayerAware {

    val shaderState: T
    val shaderCache: Cache
    val shaderCode: String
    override val layerIndex: Int? get() = null

    interface State {
        // TODO: Delegates could be used to simplify this
        fun ShaderUniformProvider.applyUniforms() = Unit
    }

    class Cache {
        internal var runtimeShader: Any? = null
        internal var uniformProvider: ShaderUniformProvider? = null
    }

    companion object {
        const val RESOLUTION = "resolution"
    }
}