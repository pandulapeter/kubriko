/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.scenes

import androidx.compose.ui.test.ExperimentalTestApi
import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.manager.Manager
import com.pandulapeter.kubriko.sceneEditor.Editable
import com.pandulapeter.kubriko.sceneEditor.EditableMetadata
import com.pandulapeter.kubriko.serialization.SerializationManager
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import com.pandulapeter.kubrikoCreator.sceneRuntime.SceneEditingState
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class RememberSceneViewTest {

    @Test
    fun whatTheProjectsManagersThrowIsReportedRatherThanCrashingTheWindow() = uiTest {
        val failures = mutableListOf<Pair<Throwable, Boolean>>()
        val state = SceneEditingState.Open(
            scene = ProjectResource(ResourceGroup.SCENES, File("main.json"), "main.json"),
            entryPoint = ThrowingEntryPoint,
            actors = emptyList(),
            revision = 1,
        )
        showing {
            rememberSceneView(state) { exception, isShowingOlderClasses -> failures += exception to isShowingOlderClasses }
        }
        waitUntil(timeoutMillis = 5_000) { failures.isNotEmpty() }
        assertEquals(listOf<Pair<String?, Boolean>>(PROBLEM to false), failures.map { (exception, isShowingOlderClasses) -> exception.message to isShowingOlderClasses })
    }

    @Test
    fun anErrorWhileCreatingTheViewIsReportedToo() = uiTest {
        val failures = mutableListOf<Pair<Throwable, Boolean>>()
        val state = SceneEditingState.Open(
            scene = ProjectResource(ResourceGroup.SCENES, File("main.json"), "main.json"),
            entryPoint = ErrorEntryPoint,
            actors = emptyList(),
            revision = 1,
        )
        showing {
            rememberSceneView(state) { exception, isShowingOlderClasses -> failures += exception to isShowingOlderClasses }
        }
        waitUntil(timeoutMillis = 5_000) { failures.isNotEmpty() }
        assertEquals(listOf<Pair<String?, Boolean>>(PROBLEM to false), failures.map { (exception, isShowing) -> exception.message to isShowing })
    }

    private object ErrorEntryPoint : CreatorEntryPoint {
        override fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>> = EditableMetadata.newSerializationManagerInstance()

        override fun createCustomManagers(): List<Manager> = throw AssertionError(PROBLEM)
    }

    private object ThrowingEntryPoint : CreatorEntryPoint {
        override fun createSerializationManager(): SerializationManager<EditableMetadata<*>, Editable<*>> = EditableMetadata.newSerializationManagerInstance()

        override fun createCustomManagers(): List<Manager> = throw IllegalStateException(PROBLEM)
    }

    private companion object {
        const val PROBLEM = "The Lives Manager couldn't read its settings."
    }
}
