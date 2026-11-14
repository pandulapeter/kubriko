/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import com.pandulapeter.kubrikoCreator.actors.AnimatedValue
import com.pandulapeter.kubrikoCreator.actors.AnimationEasing
import com.pandulapeter.kubrikoCreator.actors.AnimationNaming
import com.pandulapeter.kubrikoCreator.actors.AnimationTarget
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.OPACITY_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.REMOVED_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SCENE_OFFSET_IMPORT
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.colorLiteral

/**
 * The Animate Block and what plays it: each [AnimationTarget] an Actor type animates is a few plain fields (its two ends,
 * how far it is, how long it takes, its easing, and whether it removes the Actor), a function the Block starts it through,
 * and a step in `update` that sets the value from them, so playing one allocates nothing. A target has one animation at a
 * time, which the next Block of it starts over. The position moves by the difference its easing made since the frame
 * before, rather than being set, so it adds up with the velocity.
 */
internal object AnimateCode {

    /**
     * Kubriko's `Scale`, named in full rather than imported, since an Actor type may be called `Scale` too (no Property
     * may be called [AnimationNaming.PACKAGE_ROOT]).
     */
    private const val SCALE_CLASS = "${AnimationNaming.PACKAGE_ROOT}.pandulapeter.kubriko.types.Scale"
    private const val DURATION_LIMIT = "Int.MAX_VALUE / 2"

    fun needOf(target: AnimationTarget) = when (target) {
        AnimationTarget.SCALE -> BlockNeed.SCALE_TWEEN
        AnimationTarget.OPACITY -> BlockNeed.OPACITY_TWEEN
        AnimationTarget.COLOR -> BlockNeed.COLOR_TWEEN
        AnimationTarget.POSITION -> BlockNeed.POSITION_TWEEN
    }

    /**
     * The numbers are read in the call's arguments, with no local in scope, so a Property named like one is still the
     * Property.
     */
    fun animate(block: BlockDefinition.Animate, scope: BlockScope): BlockCode {
        val target = block.value.target
        val ends = when (val value = block.value) {
            is AnimatedValue.Scale -> "from = ${NumberValueCode.float(value.from, scope)}, to = ${NumberValueCode.float(value.to, scope)}"
            is AnimatedValue.Opacity -> "from = ${NumberValueCode.float(value.from, scope)} / 100f, to = ${NumberValueCode.float(value.to, scope)} / 100f"
            is AnimatedValue.Color -> "from = Color(${colorLiteral(value.from)}), to = Color(${colorLiteral(value.to)})"
            is AnimatedValue.Position -> "to = ${BlockCodeGenerator.placementCode(value.x, value.y, value.isRelative, scope)}"
        }
        val arguments = "$ends, seconds = ${NumberValueCode.float(block.seconds, scope)}, easing = ${easingCode(block.easing)}, removesActor = ${block.removesActor}"
        return BlockCode(
            lines = listOf("${AnimationNaming.startFunction(target)}($arguments)"),
            needs = setOf(BlockNeed.MOVEMENT, BlockNeed.ACTOR_MANAGER, needOf(target)),
            imports = if (target == AnimationTarget.POSITION) setOf(SCENE_OFFSET_IMPORT) else emptySet(),
        )
    }

    private fun easingCode(easing: AnimationEasing) = when (easing) {
        AnimationEasing.LINEAR -> 0
        AnimationEasing.EASE_IN -> 1
        AnimationEasing.EASE_OUT -> 2
        AnimationEasing.EASE_IN_OUT -> 3
    }

    private fun typeOf(target: AnimationTarget) = when (target) {
        AnimationTarget.SCALE, AnimationTarget.OPACITY -> "Float"
        AnimationTarget.COLOR -> "Color"
        AnimationTarget.POSITION -> "SceneOffset"
    }

    private fun initialValueOf(target: AnimationTarget) = when (target) {
        AnimationTarget.SCALE, AnimationTarget.OPACITY -> "0f"
        AnimationTarget.COLOR -> "Color.White"
        AnimationTarget.POSITION -> "SceneOffset.Zero"
    }

    fun importsOf(targets: List<AnimationTarget>) = if (AnimationTarget.POSITION in targets) setOf(SCENE_OFFSET_IMPORT) else emptySet()

    /**
     * The fields of the animation of each of [targets].
     *
     * @param visibility `private` for a type of its own, `protected` for a family's base class.
     */
    fun StringBuilder.appendFields(targets: List<AnimationTarget>, visibility: String) = targets.forEach { target ->
        appendLine()
        appendLine("    /**")
        appendLine("     * The ${target.fieldPrefix} animation of the Animate Blocks: its two ends, how far it is and how long it takes in milliseconds")
        appendLine("     * (0 while none runs), its easing, and whether it removes the Actor when it ends.")
        appendLine("     */")
        appendLine("    $visibility var ${AnimationNaming.from(target)}: ${typeOf(target)} = ${initialValueOf(target)}")
        appendLine("    $visibility var ${AnimationNaming.to(target)}: ${typeOf(target)} = ${initialValueOf(target)}")
        appendLine("    $visibility var ${AnimationNaming.elapsed(target)} = 0")
        appendLine("    $visibility var ${AnimationNaming.duration(target)} = 0")
        appendLine("    $visibility var ${AnimationNaming.easing(target)} = 0")
        appendLine("    $visibility var ${AnimationNaming.removesActor(target)} = false")
    }

    /**
     * Advances the animation of each of [targets] in `update`, before the Events, so one an Event starts shows its start
     * on that frame. One that removes the Actor ends the update there.
     */
    fun StringBuilder.appendSteps(targets: List<AnimationTarget>) = targets.forEach { target ->
        val elapsed = AnimationNaming.elapsed(target)
        val duration = AnimationNaming.duration(target)
        val from = AnimationNaming.from(target)
        val to = AnimationNaming.to(target)
        val progress = "${AnimationNaming.PROGRESS_FUNCTION}($elapsed, $duration, ${AnimationNaming.easing(target)})"
        appendLine("        if ($duration > 0) {")
        if (target == AnimationTarget.POSITION) appendLine("            val progressBefore = $progress")
        appendLine("            $elapsed = ($elapsed + deltaTimeInMilliseconds).coerceAtMost($duration)")
        when (target) {
            AnimationTarget.SCALE -> {
                appendLine("            val scale = $from + ($to - $from) * $progress")
                appendLine("            body.scale = $SCALE_CLASS(scale, scale)")
            }
            AnimationTarget.OPACITY -> appendLine("            $OPACITY_FIELD = $from + ($to - $from) * $progress")
            AnimationTarget.COLOR -> {
                appendLine("            val progress = $progress")
                appendLine("            color = Color(")
                appendLine("                red = $from.red + ($to.red - $from.red) * progress,")
                appendLine("                green = $from.green + ($to.green - $from.green) * progress,")
                appendLine("                blue = $from.blue + ($to.blue - $from.blue) * progress,")
                appendLine("                alpha = color.alpha,")
                appendLine("            )")
            }
            AnimationTarget.POSITION -> appendLine("            body.position += ($to - $from) * ($progress - progressBefore)")
        }
        appendLine("            if ($elapsed == $duration) {")
        appendLine("                $duration = 0")
        appendLine("                if (${AnimationNaming.removesActor(target)}) {")
        appendLine("                    $REMOVED_FIELD = true")
        appendLine("                    actorManager.remove(this)")
        appendLine("                    return")
        appendLine("                }")
        appendLine("            }")
        appendLine("        }")
    }

    /**
     * The functions the Animate Blocks start the animation of each of [targets] through, and the easing they share.
     *
     * @param visibility `private` for a type of its own, `protected` for a family's base class.
     */
    fun StringBuilder.appendFunctions(targets: List<AnimationTarget>, visibility: String) {
        targets.forEach { target -> appendStartFunction(target, visibility) }
        appendLine()
        appendLine("    /**")
        appendLine("     * How far along its easing an animation [elapsed] milliseconds into its [duration] is, from 0 to 1. [easing] is 0 to")
        appendLine("     * go evenly, 1 to start slowly, 2 to end slowly, and 3 for both.")
        appendLine("     */")
        appendLine("    $visibility fun ${AnimationNaming.PROGRESS_FUNCTION}(elapsed: Int, duration: Int, easing: Int): Float {")
        appendLine("        val linear = elapsed.toFloat() / duration")
        appendLine("        return when (easing) {")
        appendLine("            1 -> linear * linear")
        appendLine("            2 -> linear * (2f - linear)")
        appendLine("            3 -> if (linear < 0.5f) 2f * linear * linear else 1f - 2f * (1f - linear) * (1f - linear)")
        appendLine("            else -> linear")
        appendLine("        }")
        appendLine("    }")
    }

    private fun StringBuilder.appendStartFunction(target: AnimationTarget, visibility: String) {
        val from = AnimationNaming.from(target)
        val to = AnimationNaming.to(target)
        val type = typeOf(target)
        appendLine()
        appendLine("    /**")
        appendLine("     * Starts the ${target.fieldPrefix} animation over, taking [seconds] (at least a millisecond) with the easing [easing].")
        appendLine("     */")
        if (target == AnimationTarget.POSITION) {
            appendLine("    $visibility fun ${AnimationNaming.startFunction(target)}(to: $type, seconds: Float, easing: Int, removesActor: Boolean) {")
            appendLine("        $from = body.position")
            appendLine("        $to = to")
        } else {
            appendLine("    $visibility fun ${AnimationNaming.startFunction(target)}(from: $type, to: $type, seconds: Float, easing: Int, removesActor: Boolean) {")
            val bounded = when (target) {
                AnimationTarget.OPACITY -> ".coerceIn(0f, 1f)"
                AnimationTarget.SCALE -> ".coerceAtLeast(0f)"
                AnimationTarget.COLOR, AnimationTarget.POSITION -> ""
            }
            appendLine("        $from = from$bounded")
            appendLine("        $to = to$bounded")
        }
        appendLine("        ${AnimationNaming.elapsed(target)} = 0")
        appendLine("        ${AnimationNaming.duration(target)} = (seconds * 1000f + 0.5f).toInt().coerceIn(1, $DURATION_LIMIT)")
        appendLine("        ${AnimationNaming.easing(target)} = easing")
        appendLine("        ${AnimationNaming.removesActor(target)} = removesActor")
        when (target) {
            AnimationTarget.SCALE -> appendLine("        body.scale = $SCALE_CLASS($from, $from)")
            AnimationTarget.OPACITY -> appendLine("        $OPACITY_FIELD = $from")
            AnimationTarget.COLOR -> appendLine("        color = $from.copy(alpha = color.alpha)")
            AnimationTarget.POSITION -> Unit
        }
        appendLine("    }")
    }
}
