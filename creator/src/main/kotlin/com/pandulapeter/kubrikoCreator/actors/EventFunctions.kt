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

import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.INDENT
import com.pandulapeter.kubrikoCreator.actors.BlockCodeGenerator.eventCode

/**
 * The functions the generated classes run their Events in: one per kind of Event (and key, other Actor type, or Timer),
 * which `onAdded`, `update`, `onKeyPressed`, `onCollisionDetected`, and a Manager's `onInitialize` and `onUpdate` only
 * call, so those stay small however many Blocks the Events have. A standalone Actor type's and a Manager type's are
 * private, a family's are the hooks its Actor types override.
 *
 * Each takes what its caller has that an Event can use, so a Kotlin Block compiles the same wherever it is.
 */
internal object EventFunctions {

    /**
     * @param imports What the parameters of [signature] import, for every file that declares or overrides it.
     */
    data class Function(
        val signature: String,
        val call: String,
        val imports: Set<String> = emptySet(),
    )

    /**
     * The function [event] runs in, or null if its code isn't generated: an Event the kind of type doesn't have, or one
     * with an unknown key, other Actor type, or Timer.
     *
     * @param scope The scope of the type, or of a family's topmost type, for the kind of type and the classes collision
     *   Events name.
     */
    fun of(event: EventDefinition, scope: BlockScope): Function? = if (!event.type.isAvailableIn(scope.kind)) null else when (event.type) {
        EventType.ADDED_TO_SCENE -> Function("$ADDED($KUBRIKO_PARAMETER)", "$ADDED(kubriko)", setOf(KUBRIKO_IMPORT))
        EventType.GAME_STARTED -> Function("$GAME_STARTED($KUBRIKO_PARAMETER)", "$GAME_STARTED(kubriko)", setOf(KUBRIKO_IMPORT))
        EventType.EVERY_FRAME -> Function("$EVERY_FRAME($DELTA_TIME_PARAMETER)", "$EVERY_FRAME($DELTA_TIME)")
        EventType.EDGE_TOUCHED -> Function("$EDGE($DELTA_TIME_PARAMETER, $EDGE_PARAMETERS)", "$EDGE($DELTA_TIME, $EDGE_ARGUMENTS)")
        EventType.KEY_PRESSED -> event.trigger.key?.takeIf { it.isKnown }?.let { Function("${key(it)}()", "${key(it)}()") }
        EventType.COLLISION -> event.trigger.otherActorType?.takeIf { it in scope.actorTypeNames }?.let { other ->
            Function("${collision(other)}(other: ${scope.classOf(other)})", "${collision(other)}(other)")
        }
        EventType.TIMER_FINISHED -> event.trigger.timer?.takeIf(TimerNaming::isValid)?.let { Function("${timer(it)}($DELTA_TIME_PARAMETER)", "${timer(it)}($DELTA_TIME)") }
    }

    /**
     * Writes [function] with [modifiers] (such as `private`), running the code of [events] in order, each in [scope],
     * and adds what its parameters use to [imports].
     */
    fun StringBuilder.appendFunction(
        modifiers: String,
        function: Function,
        events: List<EventDefinition>,
        scope: BlockScope,
        imports: MutableSet<String>,
    ) {
        imports += function.imports
        appendLine()
        appendLine("    $modifiers fun ${function.signature} {")
        events.forEach { event -> eventCode(event.blocks, event.type, scope).lines.forEach { appendLine(INDENT.repeat(2) + it) } }
        appendLine("    }")
    }

    const val ADDED = "onAddedToScene"
    const val GAME_STARTED = "onGameStarted"
    const val EVERY_FRAME = "onEveryFrame"
    const val EDGE = "onEdgeTouched"
    private const val DELTA_TIME = "deltaTimeInMilliseconds"
    private const val DELTA_TIME_PARAMETER = "$DELTA_TIME: Int"
    private const val KUBRIKO_PARAMETER = "kubriko: Kubriko"
    private const val KUBRIKO_IMPORT = "com.pandulapeter.kubriko.Kubriko"
    private const val EDGE_ARGUMENTS = "isTouchingLeftEdge, isTouchingRightEdge, isTouchingTopEdge, isTouchingBottomEdge"
    private const val EDGE_PARAMETERS = "isTouchingLeftEdge: Boolean, isTouchingRightEdge: Boolean, isTouchingTopEdge: Boolean, isTouchingBottomEdge: Boolean"

    fun key(key: KeyChoice) = "onKeyPressed${key.composeKeyName}"

    fun collision(other: String) = "onCollisionWith$other"

    fun timer(timer: String) = TimerNaming.hookName(timer)
}
