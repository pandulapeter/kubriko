/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.demoContentShaders.implementation

internal data class ContentShadersState(
    val isSmoothPixelationShaderEnabled: Boolean = false,
    val isVignetteShaderEnabled: Boolean = true,
    val isComicShaderEnabled: Boolean = false,
    val isBlurShaderEnabled: Boolean = false,
    val isRippleShaderEnabled: Boolean = true,
    val isChromaticAberrationShaderEnabled: Boolean = true,
)
