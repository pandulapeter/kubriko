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

import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.REMOVED_FIELD
import com.pandulapeter.kubrikoCreator.actors.codegen.BlockCodeGenerator.SCENES_FIELD

/**
 * What a generated Actor does with the Scene's bounds (or the screen's, while there are none) after it moved: staying
 * inside them, the edge Event, and the leaving Event. Everything uses the bounding box rather than the body's size around
 * its position, so that a pivot that isn't in the middle, a rotation, or a scale don't move the edges.
 */
internal object SceneBoundsCode {

    const val STAYS_INSIDE_FIELD = "staysInsideScene"
    const val WAS_INSIDE_FIELD = "wasInsideScene"
    private val EDGES = listOf("Left", "Right", "Top", "Bottom")
    val WAS_TOUCHING_FIELDS = EDGES.map { "wasTouching${it}Edge" }

    /**
     * The fields [appendBoundsHandling] keeps between frames.
     *
     * @param isFamilyRoot Whether the class is a family's base class, whose members say whether they stay inside.
     */
    fun StringBuilder.appendBoundsFields(
        containment: SceneContainment,
        firesEdgeEvent: Boolean,
        firesLeftSceneEvent: Boolean,
        isFamilyRoot: Boolean,
    ) {
        if (containment == SceneContainment.SOME && isFamilyRoot) {
            appendLine()
            appendLine("    /**")
            appendLine("     * Whether it is pushed back whenever it reaches past an edge of the Scene.")
            appendLine("     */")
            appendLine("    protected open val $STAYS_INSIDE_FIELD = false")
        }
        if (firesEdgeEvent && containment != SceneContainment.ALL) {
            appendLine()
            appendLine("    /**")
            appendLine("     * The edges it reached past on the last frame, so the edge Event runs only when it starts reaching past one.")
            appendLine("     */")
            WAS_TOUCHING_FIELDS.forEach { appendLine("    private var $it = false") }
        }
        if (firesLeftSceneEvent) {
            appendLine()
            appendLine("    /**")
            appendLine("     * Whether it has been at least partly inside the Scene since it last left it.")
            appendLine("     */")
            appendLine("    private var $WAS_INSIDE_FIELD = false")
        }
    }

    /**
     * Pushes the Actor back inside per [containment], runs [edgeCall] (with `isTouchingLeftEdge` and the others in scope)
     * when it starts reaching past an edge, and [leftSceneCall] once it is wholly outside after having been inside.
     * An Actor that stays inside is pushed back every time, so for it the edge Event runs on every frame it reaches past
     * an edge, as it always did.
     */
    fun StringBuilder.appendBoundsHandling(containment: SceneContainment, edgeCall: String?, leftSceneCall: String?) {
        val readsEdges = edgeCall != null || containment != SceneContainment.NONE
        appendLine("        val box = body.axisAlignedBoundingBox")
        // The settings are read once, so a Scene that finishes loading meanwhile can't mix its bounds with the screen's,
        // and not through a nullable SceneOffset, which would box every frame.
        appendLine("        val sceneSettings = $SCENES_FIELD.settings")
        appendLine("        val leftEdge = if (sceneSettings == null) viewportManager.topLeft.value.x else sceneSettings.topLeft.x")
        appendLine("        val rightEdge = if (sceneSettings == null) viewportManager.bottomRight.value.x else sceneSettings.bottomRight.x")
        appendLine("        val topEdge = if (sceneSettings == null) viewportManager.topLeft.value.y else sceneSettings.topLeft.y")
        appendLine("        val bottomEdge = if (sceneSettings == null) viewportManager.bottomRight.value.y else sceneSettings.bottomRight.y")
        if (readsEdges) {
            // Strict, so an Actor resting where a push left it doesn't reach past the edge again.
            appendLine("        val isTouchingLeftEdge = box.min.x < leftEdge")
            appendLine("        val isTouchingRightEdge = box.max.x > rightEdge")
            appendLine("        val isTouchingTopEdge = box.min.y < topEdge")
            appendLine("        val isTouchingBottomEdge = box.max.y > bottomEdge")
        }
        // Read before the push below, which moves the box.
        if (leftSceneCall != null) appendLine("        val isInsideScene = box.max.x > leftEdge && box.min.x < rightEdge && box.max.y > topEdge && box.min.y < bottomEdge")
        val isTouchingAny = "isTouchingLeftEdge || isTouchingRightEdge || isTouchingTopEdge || isTouchingBottomEdge"
        when (containment) {
            SceneContainment.NONE -> Unit
            SceneContainment.SOME -> appendPush("$STAYS_INSIDE_FIELD && ($isTouchingAny)", thenCall = null)
            SceneContainment.ALL -> appendPush(isTouchingAny, thenCall = edgeCall)
        }
        if (edgeCall != null && containment != SceneContainment.ALL) {
            val startsTouching = EDGES.zip(WAS_TOUCHING_FIELDS).joinToString(" || ") { (edge, was) -> "(isTouching${edge}Edge && !$was)" }
            appendLine("        if ($startsTouching) $edgeCall")
            EDGES.zip(WAS_TOUCHING_FIELDS).forEach { (edge, was) ->
                appendLine("        $was = isTouching${edge}Edge${if (containment == SceneContainment.SOME) " && !$STAYS_INSIDE_FIELD" else ""}")
            }
        }
        if (leftSceneCall != null) {
            appendLine("        if (isInsideScene) {")
            appendLine("            $WAS_INSIDE_FIELD = true")
            appendLine("        } else if ($WAS_INSIDE_FIELD) {")
            appendLine("            $WAS_INSIDE_FIELD = false")
            appendLine("            if (!$REMOVED_FIELD) $leftSceneCall")
            appendLine("        }")
        }
    }

    private fun StringBuilder.appendPush(condition: String, thenCall: String?) {
        appendLine("        if ($condition) {")
        appendLine("            body.position += SceneOffset(")
        appendLine("                if (isTouchingLeftEdge) leftEdge - box.min.x else if (isTouchingRightEdge) rightEdge - box.max.x else SceneUnit.Zero,")
        appendLine("                if (isTouchingTopEdge) topEdge - box.min.y else if (isTouchingBottomEdge) bottomEdge - box.max.y else SceneUnit.Zero,")
        appendLine("            )")
        thenCall?.let { appendLine("            $it") }
        appendLine("        }")
    }
}
