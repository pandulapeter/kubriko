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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.pandulapeter.kubrikoCreator.WritersRoomRequest
import com.pandulapeter.kubrikoCreator.template.GameTarget
import com.pandulapeter.kubrikoCreator.template.NewProjectForm
import com.pandulapeter.kubrikoCreator.template.ProjectTemplate
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.assistant.StartingTheWritersRoom
import com.pandulapeter.kubrikoCreator.ui.components.LocalIsAiEnabled
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProjectStartChoiceTest {

    @Test
    fun aProjectStartsFromATemplateOrFromAnIdeaTypedAsAParagraph() = uiTest(width = 600, height = 360) {
        var form by mutableStateOf(NewProjectForm(name = "My Game", packageName = "com.example.mygame", parentDirectory = File("/projects")))
        showing { ProjectStartChoice(form = form, onFormChange = { form = it }) }
        onNodeWithText("Blank").performClick()
        assertEquals(ProjectTemplate.BLANK, form.template)
        onNodeWithText("Start from an idea").performClick()
        onNode(hasSetTextAction()).performTextInput("A cat who herds sheep.")
        saveSnapshot("new-project-from-idea")
        assertEquals("A cat who herds sheep.", form.request?.pitch)
        assertEquals(ProjectTemplate.BLANK, form.request?.template)
    }

    @Test
    fun withAiFeaturesOffThereIsNoStartingFromAnIdea() = uiTest(width = 600, height = 200) {
        showing {
            CompositionLocalProvider(LocalIsAiEnabled provides false) {
                ProjectStartChoice(form = NewProjectForm(parentDirectory = File("/projects")), onFormChange = {})
            }
        }
        onNodeWithText("Start from an idea").assertDoesNotExist()
    }

    @Test
    fun anIdeaChosenBeforeAiFeaturesWereTurnedOffGivesWayToTheTemplate() = uiTest(width = 600, height = 200) {
        var form by mutableStateOf(NewProjectForm(name = "My Game", packageName = "com.example.mygame", parentDirectory = File("/projects"), pitch = ""))
        var isAiEnabled by mutableStateOf(true)
        showing {
            CompositionLocalProvider(LocalIsAiEnabled provides isAiEnabled) {
                ProjectStartChoice(form = form, onFormChange = { form = it })
            }
        }
        isAiEnabled = false
        waitForIdle()
        assertEquals(null, form.pitch)
        assertEquals(form.template, form.request?.template)
    }

    @Test
    fun theWritersRoomStartsOnceFromTheIdea() = uiTest {
        val started = mutableListOf<String>()
        var shown = 0
        val request = WritersRoomRequest("A cat who herds sheep.")
        var isShown by mutableStateOf(true)
        showing { if (isShown) StartingTheWritersRoom(request, onShowAssistant = { shown++ }, onStart = { started += it }) }
        waitForIdle()
        isShown = false
        waitForIdle()
        isShown = true
        waitForIdle()
        assertEquals(listOf("Let's make a game in the writers' room. My idea: A cat who herds sheep."), started)
        assertEquals(1, shown)
    }

    @Test
    fun theTargetsThisComputerCantBuildForCantBeChosen() = uiTest(width = 600, height = 120) {
        var form by mutableStateOf(NewProjectForm(name = "My Game", packageName = "com.example.mygame", parentDirectory = File("/projects")))
        showing { NewProjectTargets(form = form, canBuild = { it != GameTarget.ANDROID }, onFormChange = { form = it }) }
        saveSnapshot("new-project-targets")
        onNodeWithText("Android").assertIsNotEnabled()
        onNodeWithText("Web").performClick()
        onNodeWithText("iOS").performClick()
        assertEquals(setOf(GameTarget.IOS), form.request?.targets)
    }
}
