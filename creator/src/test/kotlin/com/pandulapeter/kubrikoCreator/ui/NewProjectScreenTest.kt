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

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.template.NewProjectForm
import com.pandulapeter.kubrikoCreator.template.TemplateProblem
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class NewProjectScreenTest {
    private val form = NewProjectForm(parentDirectory = File("/Users/me/Games"), isUnderVersionControl = false).withName("My Game").withPackageName("io.github.me.game")

    @Test
    fun aFailedCreationComesBackToTheFormAsItWasFilledInSayingWhy() = uiTest(width = 900, height = 900) {
        val created = mutableListOf<NewProjectForm>()
        showing {
            NewProjectScreen(
                initialForm = form,
                error = TemplateProblem.NOT_CREATED_OFFLINE.resource,
                errorArguments = emptyList(),
                defaultParentDirectory = File("/Users/me"),
                onCreate = { created += it },
                onCancel = {},
            )
        }
        onNode(hasText("My Game")).assertIsDisplayed()
        onNode(hasText("io.github.me.game")).assertIsDisplayed()
        onNode(hasText(form.parentDirectory.path)).assertIsDisplayed()
        onNodeWithText(
            "Kubriko Creator couldn't create the project, since it couldn't reach the internet, where a new project's Kubriko and Gradle come from. " +
                "Check the connection, and try again.",
        ).assertIsDisplayed()
        saveSnapshot("new-project-failed")
        onNodeWithText("Create").performClick()
        assertEquals(listOf(form), created)
    }
}
