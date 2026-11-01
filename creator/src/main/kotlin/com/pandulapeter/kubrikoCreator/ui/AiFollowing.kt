/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.commands.AiStep
import com.pandulapeter.kubrikoCreator.commands.CommandTarget
import com.pandulapeter.kubrikoCreator.ui.actorTypes.ActorTypeHighlight

/**
 * What lights up the step's target once it has finished (only then is its target final: a Block being added has no
 * index before it exists), or null while it runs.
 */
internal val AiStep.flashKey get() = id.takeUnless { isRunning }

/**
 * The Actor type the editor switches to while following the AI through this step, if any.
 */
internal fun AiStep.actorTypeToFollow() = (target as? CommandTarget.BlockType)?.name

/**
 * What this finished step lights up in the editor of [actorTypeName], or null if it runs or is about something else.
 *
 * @param flashKey What makes it light up, a new one each time.
 */
internal fun AiStep.highlightIn(actorTypeName: String?, flashKey: Any? = this.flashKey) = (target as? CommandTarget.BlockType)
    ?.takeIf { it.name == actorTypeName }
    ?.let { target -> flashKey?.let { ActorTypeHighlight(key = it, eventIndex = target.eventIndex, blockIndex = target.blockIndex) } }

/**
 * The name of the Actor type, Manager type, or Scene this step lights up in the sidebar.
 */
internal fun AiStep.sidebarName() = when (val target = target) {
    is CommandTarget.BlockType -> target.name
    is CommandTarget.Scene -> target.name
    CommandTarget.VersionControl, CommandTarget.Game, CommandTarget.Project, null -> null
}

/**
 * Whether the editor has a place to go to for this step: an Actor type, a Manager type, or a Scene.
 */
internal fun AiStep.canBeShown() = target is CommandTarget.BlockType || target is CommandTarget.Scene

/**
 * A step the user went back to from the assistant panel, which the editor shows until the AI takes another one. It
 * compares by identity, so going back to the same step again lights it up again.
 *
 * @param latestStepId The ID of the AI's newest step when the user went back.
 */
internal class RevisitedAiStep(
    val step: AiStep,
    val latestStepId: Int?,
)

/**
 * The step the editor shows and what makes it light up.
 */
internal data class ShownAiStep(
    val step: AiStep,
    val flashKey: Any?,
)

/**
 * The step the editor shows: the one the user went back to ([revisited]) until the AI takes another one, otherwise
 * the AI's newest ([latest]).
 */
internal fun shownAiStep(latest: AiStep?, revisited: RevisitedAiStep?) = revisited
    ?.takeIf { it.latestStepId == latest?.id }
    ?.let { ShownAiStep(step = it.step, flashKey = it) }
    ?: latest?.let { ShownAiStep(step = it, flashKey = it.flashKey) }
