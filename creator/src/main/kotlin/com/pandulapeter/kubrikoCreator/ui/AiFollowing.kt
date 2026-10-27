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
internal fun AiStep.actorTypeToFollow() = (target as? CommandTarget.ActorType)?.name

/**
 * What this finished step lights up in the editor of [actorTypeName], or null if it runs or is about something else.
 */
internal fun AiStep.highlightIn(actorTypeName: String?) = (target as? CommandTarget.ActorType)
    ?.takeIf { it.name == actorTypeName }
    ?.let { target -> flashKey?.let { ActorTypeHighlight(key = it, eventIndex = target.eventIndex, blockIndex = target.blockIndex) } }

/**
 * The name of the Actor type or Scene this step lights up in the sidebar.
 */
internal fun AiStep.sidebarName() = when (val target = target) {
    is CommandTarget.ActorType -> target.name
    is CommandTarget.Scene -> target.name
    CommandTarget.VersionControl, CommandTarget.Game, null -> null
}
