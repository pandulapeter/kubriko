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

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.requestFocus
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.ui.showing
import com.pandulapeter.kubrikoCreator.ui.uiTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.actor_type_properties_empty
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PropertyListTest {

    @Test
    fun removingAPropertyAboveTheOneBeingTypedIntoKeepsEachRowsText() = uiTest {
        var properties by mutableStateOf<List<PropertyDefinition>>(
            listOf(
                PropertyDefinition.Text(name = "first", initialValue = "A"),
                PropertyDefinition.Text(name = "second", initialValue = "B"),
                PropertyDefinition.Text(name = "third", initialValue = "C"),
            ),
        )
        showing {
            val itemKeys = remember { ListItemKeys<PropertyDefinition>() }
            val keys = remember(itemKeys, properties) { itemKeys.update(properties) }
            LazyColumn {
                propertyListItems(
                    properties = properties,
                    propertyKeys = keys,
                    emptyHint = Res.string.actor_type_properties_empty,
                    onAdded = {},
                    onChanged = { name, property -> properties = properties.map { if (it.name == name) property else it } },
                    onRemoved = { name -> properties = properties.filter { it.name != name } },
                )
            }
        }
        val field = onNode(hasSetTextAction() and hasText("B"))
        field.requestFocus()
        field.performTextReplacement("B2")
        waitForIdle()
        properties = properties.drop(1)
        waitForIdle()
        val typedField = onNode(hasSetTextAction() and hasText("B2"))
        typedField.assertIsFocused()
        onAllNodes(hasSetTextAction() and hasText("B2")).assertCountEquals(1)
        onAllNodes(hasSetTextAction() and hasText("C")).assertCountEquals(1)
        typedField.performTextReplacement("B3")
        waitForIdle()
        assertEquals(listOf("B3", "C"), properties.map { (it as PropertyDefinition.Text).initialValue })
    }
}
