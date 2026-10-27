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

import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyKind
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertyChoiceTest {

    @Test
    fun blocksOfferOnlyThePropertiesOfTheKindTheyNeed() {
        val properties = listOf(PropertyDefinition.Number("score"), PropertyDefinition.Text("title"), PropertyDefinition.Number("lives"))
        assertEquals(listOf("score", "lives"), propertyChoices(properties, PropertyKind.NUMBER))
        assertEquals(listOf("title"), propertyChoices(properties, PropertyKind.TEXT))
        assertEquals(emptyList(), propertyChoices(properties, PropertyKind.TOGGLE))
    }
}
