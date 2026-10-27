/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.mcp

import kotlin.test.Test
import kotlin.test.assertEquals

class ClientSetupTest {

    @Test
    fun claudeCodeConnectsOverHttpWithTheTokenAsABearerToken() {
        assertEquals(
            "claude mcp add --transport http kubriko-creator http://127.0.0.1:9000/mcp --header \"Authorization: Bearer abc\"",
            McpSettings(port = 9000, token = "abc").claudeCodeCommand(),
        )
    }
}
