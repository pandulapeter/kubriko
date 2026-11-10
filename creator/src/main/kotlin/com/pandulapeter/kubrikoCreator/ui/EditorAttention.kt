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

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.ai.assistant.AttentionChip
import com.pandulapeter.kubrikoCreator.ui.scenes.ScenePicture
import com.pandulapeter.kubrikoCreator.ui.scenes.ScenePictureHost

/**
 * What the center of the workspace shows beyond its selection, which the assistant offers with the next message: the Event
 * the type editor shows, and a way to take a picture of the open Scene. The editors report it here, since only they know
 * it, and the AI panel reads it.
 */
@Stable
internal class EditorAttention : ScenePictureHost {

    /**
     * The Event the type editor shows, if any.
     */
    var shownEvent: AttentionChip.Event? by mutableStateOf(null)
        private set

    /**
     * Takes a picture of the open Scene as the Scene editor draws it, while it shows one.
     */
    var scenePicture: ScenePicture? by mutableStateOf(null)
        private set

    /**
     * The type editor of [type] shows its Event at [index], of [eventType], or none.
     */
    fun showEvent(type: BlockTypeDefinition, index: Int?, eventType: EventType?) {
        shownEvent = if (index != null && eventType != null) AttentionChip.Event(type.name, type is ManagerTypeDefinition, index, eventType) else null
    }

    override fun provide(picture: ScenePicture) {
        scenePicture = picture
    }

    override fun withdraw(picture: ScenePicture) {
        if (scenePicture === picture) scenePicture = null
    }
}
