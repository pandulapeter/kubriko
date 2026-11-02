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
 * The hooks a family's base class has: one per kind of Event any of its Actor types reacts to.
 *
 * @param scope The scope of the family's topmost Actor type, for the classes collision Events name.
 */
internal class FamilyHooks(features: ActorTypeFeatures, val scope: BlockScope) {

    data class Hook(val signature: String, val call: String)

    val all = features.generatedEvents.mapNotNull(::of).distinct()

    fun of(event: EventDefinition): Hook? = when (event.type) {
        EventType.ADDED_TO_SCENE -> Hook("$ADDED()", "$ADDED()")
        EventType.EVERY_FRAME -> Hook("$EVERY_FRAME()", "$EVERY_FRAME()")
        EventType.EDGE_TOUCHED -> Hook("$EDGE($EDGE_PARAMETERS)", "$EDGE($EDGE_ARGUMENTS)")
        EventType.KEY_PRESSED -> event.key?.takeIf { it.isKnown }?.let { Hook("${key(it)}()", "${key(it)}()") }
        EventType.COLLISION -> event.otherActorType?.takeIf { it in scope.actorTypeNames }?.let { other ->
            Hook("${collision(other)}(other: ${scope.classOf(other)})", "${collision(other)}(other)")
        }
        EventType.TIMER_FINISHED -> event.timer?.takeIf(TimerNaming::isValid)?.let { Hook("${timer(it)}()", "${timer(it)}()") }
        EventType.GAME_STARTED -> null
    }

    companion object {
        const val ADDED = "onAddedToScene"
        const val EVERY_FRAME = "onEveryFrame"
        const val EDGE = "onEdgeTouched"
        const val EDGE_ARGUMENTS = "isTouchingLeftEdge, isTouchingRightEdge, isTouchingTopEdge, isTouchingBottomEdge"
        const val EDGE_PARAMETERS = "isTouchingLeftEdge: Boolean, isTouchingRightEdge: Boolean, isTouchingTopEdge: Boolean, isTouchingBottomEdge: Boolean"

        fun key(key: KeyChoice) = "onKeyPressed${key.composeKeyName}"

        fun collision(other: String) = "onCollisionWith$other"

        fun timer(timer: String) = TimerNaming.hookName(timer)
    }
}
