/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import kotlin.test.Test
import kotlin.test.assertEquals

class MentionIndexTest {

    @Test
    fun `every page mentioning a file is found once, the first page first, but never a page itself`() {
        val index = MentionIndex.of(
            mapOf(
                "zebra" to "[Hero](../.kubriko/actors/Player.json)",
                "levels/cave" to "[Hero](../../.kubriko/actors/Player.json) and the [hero again](../../.kubriko/actors/Player.json) [me](cave.md)",
                "README" to "![Hero](../.kubriko/actors/Player.json) `[not](../.kubriko/actors/Bat.json)`",
            ),
        )
        assertEquals(listOf("README", "levels/cave", "zebra"), index.pagesMentioning(".kubriko/actors/Player.json"))
        assertEquals(emptyList(), index.pagesMentioning(".kubriko/actors/Bat.json"))
        assertEquals(emptyList(), index.pagesMentioning("screenplay/levels/cave.md"))
    }
}
