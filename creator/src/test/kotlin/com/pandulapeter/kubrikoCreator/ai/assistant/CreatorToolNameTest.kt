/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ai.assistant

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CreatorToolNameTest {
    @Test
    fun creatorsToolsAreRecognizedAsEachAgentNamesThem() {
        assertEquals("read_actor_type", CreatorToolName.of("mcp__kubriko-creator__read_actor_type"))
        assertEquals("read_actor_type", CreatorToolName.of("kubriko-creator_read_actor_type"))
        assertEquals("list_scenes", CreatorToolName.of("kubriko-creator/list_scenes"))
    }

    @Test
    fun otherToolsAndNamesCreatorDoesntHaveAreNot() {
        assertNull(CreatorToolName.of("Edit Game.kt"))
        assertNull(CreatorToolName.of("kubriko-creator_delete_everything"))
        assertNull(CreatorToolName.of("other-server_read_actor_type"))
        assertNull(CreatorToolName.of(null))
    }
}
