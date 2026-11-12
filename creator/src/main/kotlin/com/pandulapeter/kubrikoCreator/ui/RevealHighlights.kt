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

import com.pandulapeter.kubrikoCreator.ui.actorTypes.ActorTypeHighlight
import com.pandulapeter.kubrikoCreator.ui.scenes.SceneHighlight
import com.pandulapeter.kubrikoCreator.workspace.Reveal
import com.pandulapeter.kubrikoCreator.workspace.RevealedPlace

/**
 * What this reveal lights up in the editor of the type [typeName], or null if it is about something else.
 */
internal fun Reveal.highlightIn(typeName: String): ActorTypeHighlight? = (place as? RevealedPlace.InType)
    ?.takeIf { it.type == typeName }
    ?.let { place -> ActorTypeHighlight(key = this, eventIndex = place.eventIndex, blockIndex = place.blockIndex, property = place.property) }

/**
 * The Actors this reveal lights up and selects in the Scene called [sceneName], or null if it is about something else.
 *
 * @param startedAt When they first lit up ([SceneHighlight.startedAt]).
 */
internal fun Reveal.highlightInScene(sceneName: String, startedAt: Long? = null): SceneHighlight? = (place as? RevealedPlace.InScene)
    ?.takeIf { it.scene == sceneName && it.actorIds.isNotEmpty() }
    ?.let { place -> SceneHighlight(key = this, actorIds = place.actorIds.toSet(), startedAt = startedAt, isSelecting = true) }

/**
 * Tells which of a place the user went to and the AI's step shown lit up last, so that the editors light that one up:
 * a place gone to stays until something else is shown, and would otherwise keep the AI's later steps in the same type
 * or Scene dark.
 */
internal class NewestHighlight {
    private var reveal: Reveal? = null
    private var aiStepKey: Any? = null
    private var isRevealNewer = false

    /**
     * [reveal], when it changed after [aiStepKey] (what lights up the AI's step shown) last did, or null.
     */
    fun revealIfNewer(reveal: Reveal?, aiStepKey: Any?): Reveal? {
        if (aiStepKey !== this.aiStepKey) {
            this.aiStepKey = aiStepKey
            if (aiStepKey != null) isRevealNewer = false
        }
        if (reveal !== this.reveal) {
            this.reveal = reveal
            if (reveal != null) isRevealNewer = true
        }
        return reveal?.takeIf { isRevealNewer }
    }
}
