/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.ui.components.rememberFlashStrength

/**
 * Actors of the open Scene that light up, the canvas drawing them and the Actor list their rows in the flash's color,
 * to show what the AI just changed.
 *
 * @param key What makes them light up, a new one each time.
 * @param startedAt When they first lit up, in milliseconds since the epoch, so that a Scene opened again later goes on
 *   from there rather than lighting them up anew, or null for now.
 */
internal data class SceneHighlight(
    val key: Any,
    val actorIds: Set<String>,
    val startedAt: Long? = null,
)

/**
 * One flash of the Actors [actorIds] names, which the canvas and every row of the Actor list read, so that a row
 * scrolled into view later shows it as far as it got rather than flashing again.
 *
 * @param actorIds The last Actors a highlight named, kept while the flash fades out after it ends.
 */
internal class SceneFlash(
    val actorIds: List<String>,
    val strength: State<Float>,
)

/**
 * The [SceneFlash] of [highlight], keeping its Actors after it ends so that they fade out with it.
 */
@Composable
internal fun rememberSceneFlash(highlight: SceneHighlight?): SceneFlash {
    val strength = rememberFlashStrength(highlight?.key, highlight?.startedAt)
    var lastActorIds by remember { mutableStateOf(emptySet<String>()) }
    SideEffect { highlight?.actorIds?.let { lastActorIds = it } }
    val actorIds = highlight?.actorIds ?: lastActorIds
    return remember(actorIds, strength) { SceneFlash(actorIds.toList(), strength) }
}
