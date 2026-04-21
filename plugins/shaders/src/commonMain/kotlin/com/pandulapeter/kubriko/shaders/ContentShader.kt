/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.shaders

/**
 * A specialized [Shader] that has access to the content it is being applied to.
 *
 * This is useful for post-processing effects like blur, chromatic aberration, or color correction.
 *
 * @param T The type of the shader state which holds the uniforms.
 */
interface ContentShader<T : Shader.State> : Shader<T> {

    companion object {
        /**
         * The reserved uniform name for the content input (sampler2D).
         */
        const val CONTENT = "content"
    }
}
