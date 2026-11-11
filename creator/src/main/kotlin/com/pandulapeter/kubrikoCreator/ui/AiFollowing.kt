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
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneHighlight
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceSelection

/**
 * What lights up the step's target once it has finished (only then is its target final: a Block being added has no
 * index before it exists), or null while it runs.
 */
internal val AiStep.flashKey get() = id.takeUnless { isRunning }

/**
 * The newest step that changes the project, the kind the editor follows.
 */
internal fun List<AiStep>.newestChange() = lastOrNull { it.changesProject }

/**
 * What the editor goes to while following the AI through this step: the Actor type, Manager type, or Scene it changes.
 */
internal fun AiStep.selectionToFollow(): WorkspaceSelection? = when (val target = target?.takeIf { changesProject }) {
    is CommandTarget.BlockType -> WorkspaceSelection.BlockType(target.name)
    is CommandTarget.Scene -> WorkspaceSelection.Scene(target.name)
    is CommandTarget.Page -> WorkspaceSelection.Page(target.name)
    is CommandTarget.Document -> WorkspaceSelection.Document(target.path)
    CommandTarget.VersionControl, CommandTarget.Game, CommandTarget.Project, null -> null
}

/**
 * What this finished step lights up in the editor of [actorTypeName], or null if it runs or is about something else.
 *
 * @param flashKey What makes it light up, a new one each time.
 */
internal fun AiStep.highlightIn(actorTypeName: String?, flashKey: Any? = this.flashKey) = (target as? CommandTarget.BlockType)
    ?.takeIf { it.name == actorTypeName }
    ?.let { target -> flashKey?.let { ActorTypeHighlight(key = it, eventIndex = target.eventIndex, blockIndex = target.blockIndex) } }

/**
 * The Actors this finished step lights up in the Scene called [sceneName], or null if it runs, is about something else,
 * or names no Actors that are still there.
 *
 * @param flashKey What makes them light up, a new one each time.
 * @param startedAt When they first lit up ([SceneHighlight.startedAt]).
 */
internal fun AiStep.highlightInScene(sceneName: String?, flashKey: Any? = this.flashKey, startedAt: Long? = null) = (target as? CommandTarget.Scene)
    ?.takeIf { it.name == sceneName && it.actorIds.isNotEmpty() }
    ?.let { target -> flashKey?.let { SceneHighlight(key = it, actorIds = target.actorIds.toSet(), startedAt = startedAt) } }

/**
 * The name of the Actor type, Manager type, or Scene this step lights up in the sidebar.
 */
internal fun AiStep.sidebarName() = when (val target = target) {
    is CommandTarget.BlockType -> target.name
    is CommandTarget.Scene -> target.name
    is CommandTarget.Page -> target.name
    is CommandTarget.Document -> target.path
    CommandTarget.VersionControl, CommandTarget.Game, CommandTarget.Project, null -> null
}

/**
 * Whether the editor has a place to go to for this step: an Actor type, a Manager type, a Scene, a page, or a document.
 */
internal fun AiStep.canBeShown() = target.let { it is CommandTarget.BlockType || it is CommandTarget.Scene || it is CommandTarget.Page || it is CommandTarget.Document }

/**
 * A step the user went back to from the assistant panel, which the editor shows until the AI makes another change. It
 * compares by identity, so going back to the same step again lights it up again.
 *
 * @param latestStepId The ID of the AI's newest change ([newestChange]) when the user went back.
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
 * The step the editor shows: the one the user went back to ([revisited]) until the AI makes another change (after
 * [newestChangeId]), otherwise the one it follows ([followed]).
 */
internal fun shownAiStep(followed: AiStep?, revisited: RevisitedAiStep?, newestChangeId: Int? = followed?.id) = revisited
    ?.takeIf { it.latestStepId == newestChangeId }
    ?.let { ShownAiStep(step = it.step, flashKey = it) }
    ?: followed?.let { ShownAiStep(step = it, flashKey = it.flashKey) }
