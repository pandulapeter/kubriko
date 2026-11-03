/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * The few durations and the one easing every transition in Kubriko Creator uses, so things that move together feel related:
 * quick enough never to hold anyone up, slow enough to show where something came from.
 */
public object CreatorMotion {
    /**
     * For what reacts to the pointer: hover, press, a fade out.
     */
    public const val SHORT_IN_MILLISECONDS: Int = 120

    /**
     * For what appears or changes place: a panel, a page, a section opening.
     */
    public const val MEDIUM_IN_MILLISECONDS: Int = 220

    /**
     * Starts briskly and settles gently, as things that come to rest do.
     */
    public val easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /**
     * A [MEDIUM_IN_MILLISECONDS] tween with [easing].
     */
    public fun <T> medium(): FiniteAnimationSpec<T> = tween(MEDIUM_IN_MILLISECONDS, easing = easing)

    /**
     * A [SHORT_IN_MILLISECONDS] tween with [easing].
     */
    public fun <T> short(): FiniteAnimationSpec<T> = tween(SHORT_IN_MILLISECONDS, easing = easing)
}
