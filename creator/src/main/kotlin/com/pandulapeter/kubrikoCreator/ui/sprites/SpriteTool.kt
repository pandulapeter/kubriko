/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.sprites

/**
 * What a press on the sprite editor's canvas does.
 */
internal enum class SpriteTool {
    PENCIL,
    ERASER,
    LINE,
    RECTANGLE,
    ELLIPSE,
    FILL,
    PICKER,
    SELECT,
    ;

    /**
     * Whether the tool draws what it covers while the pointer is held, and only then, when it is let go, as one step.
     */
    val isStroke: Boolean get() = this != PICKER && this != SELECT
}
