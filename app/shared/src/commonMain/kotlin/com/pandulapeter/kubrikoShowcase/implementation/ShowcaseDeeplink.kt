/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation

internal val ShowcaseEntry?.deeplink
    get() = when (this) {
        ShowcaseEntry.WALLBREAKER -> "wallbreaker"
        ShowcaseEntry.SPACE_SQUADRON -> "space-squadron"
        ShowcaseEntry.ANNOYED_PENGUINS -> "annoyed-penguins"
        ShowcaseEntry.BLOCKYS_JOURNEY -> "blockys-journey"
        ShowcaseEntry.CONTENT_SHADERS -> "content-shaders"
        ShowcaseEntry.ISOMETRIC_GRAPHICS -> "isometric-graphics"
        ShowcaseEntry.PARTICLES -> "particles"
        ShowcaseEntry.PERFORMANCE -> "performance"
        ShowcaseEntry.PHYSICS -> "physics"
        ShowcaseEntry.SHADER_ANIMATIONS -> "shader-animations"
        ShowcaseEntry.AUDIO -> "audio"
        ShowcaseEntry.COLLISION -> "collision"
        ShowcaseEntry.INPUT -> "input"
        ShowcaseEntry.ABOUT -> "about"
        ShowcaseEntry.LICENSES -> "licenses"
        null -> null
    }

internal fun String?.processDeeplink() = this?.trim()?.lowercase()?.split("/")?.filterNot { it.isBlank() }?.lastOrNull().let { deeplink ->
    ShowcaseEntry.entries.firstOrNull { it.deeplink == deeplink }
}?.takeIf { it.isAvailable }
