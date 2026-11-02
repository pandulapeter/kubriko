/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenTextDefinitionTest {

    @Test
    fun aNewTextShowsTheFirstPropertyOrScore() {
        assertEquals("{score}", ScreenTextDefinition.defaultFor(emptyList()).text)
        assertEquals("{lives}", ScreenTextDefinition.defaultFor(listOf(PropertyDefinition.Number("lives"), PropertyDefinition.Number("score"))).text)
    }
}
