/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

/**
 * The names of the members a generated Actor class keeps the Animate Blocks' animations in, one set per
 * [AnimationTarget], reserved as Property names since format version 10.
 */
public object AnimationNaming {

    /**
     * The function that turns how far an animation is into how far along its easing it is.
     */
    public const val PROGRESS_FUNCTION: String = "tweenProgress"

    public fun from(target: AnimationTarget): String = field(target, "From")

    public fun to(target: AnimationTarget): String = field(target, "To")

    public fun elapsed(target: AnimationTarget): String = field(target, "Elapsed")

    /**
     * How long the animation takes, in milliseconds, or 0 while none runs.
     */
    public fun duration(target: AnimationTarget): String = field(target, "Duration")

    public fun easing(target: AnimationTarget): String = field(target, "Easing")

    public fun removesActor(target: AnimationTarget): String = field(target, "RemovesActor")

    /**
     * The function an Animate Block starts the animation of [target] through, so it reads its numbers with no local in scope.
     */
    public fun startFunction(target: AnimationTarget): String = "start" + target.fieldPrefix.replaceFirstChar(Char::uppercaseChar) + "Tween"

    private fun field(target: AnimationTarget, part: String) = "${target.fieldPrefix}Tween$part"

    /**
     * The first part of the fully qualified names the animation code uses rather than importing a class an Actor type
     * may be named after, which a Property of that name would hide.
     */
    internal const val PACKAGE_ROOT: String = "com"

    internal val reservedNames: List<String> = AnimationTarget.entries.flatMap { target ->
        listOf(from(target), to(target), elapsed(target), duration(target), easing(target), removesActor(target), startFunction(target))
    } + PROGRESS_FUNCTION + PACKAGE_ROOT
}
