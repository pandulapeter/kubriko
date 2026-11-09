/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.actorTypes

import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.audio_files_not_read
import kubriko.creator.generated.resources.block_music_none_yet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AudioFileChoiceTest {

    @Test
    fun noFilesYetAndFilesNotKnownYetAreToldApart() {
        assertEquals(Res.string.audio_files_not_read, audioFilesHint(null, Res.string.block_music_none_yet))
        assertEquals(Res.string.block_music_none_yet, audioFilesHint(emptyList(), Res.string.block_music_none_yet))
        assertNull(audioFilesHint(listOf("files/music/theme.mp3"), Res.string.block_music_none_yet))
    }
}
