/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.search

import com.pandulapeter.kubrikoCreator.context.UsagePlace
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_music
import kubriko.creator.generated.resources.ic_sound
import kubriko.creator.generated.resources.ic_sprite
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaceLookTest {

    @Test
    fun aFileLooksAsTheSidebarShowsItsKind() = assertEquals(
        listOf(Res.drawable.ic_sprite to Accent.CODE, Res.drawable.ic_sound to Accent.AUDIO, Res.drawable.ic_music to Accent.AUDIO),
        listOf("drawable/ball.png", "files/sounds/hit.wav", "files/music/theme.mp3").map { UsagePlace.ResourceFile(it).look },
    )
}
