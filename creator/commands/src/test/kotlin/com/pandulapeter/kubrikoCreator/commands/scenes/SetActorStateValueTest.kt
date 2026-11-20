/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.scenes

import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class SetActorStateValueTest {
    private val spawner = SceneActorEntry("Spawner", """{"body":{"position":{"x":1.0,"y":2.0}},"rate":2.0}""", "spawner")

    @OptIn(EditorMutation::class)
    private fun TestScope.context() = commandContext().apply {
        scenes.write("main", SceneDocument.format(listOf(spawner)))
        scenes.takeChanges()
    }

    @Test
    fun oneValueOfAHandWrittenActorsStateIsChangedAndUndone() = runTest {
        val context = context()
        val invocation = setActorStateValue("main", "spawner", "rate", JsonPrimitive(5))
        val state = context.assertRoundTrip(invocation) { context.scenes.read("main")?.let(SceneDocument::parse)?.single()?.state }
        assertEquals("""{"body":{"position":{"x":1.0,"y":2.0}},"rate":5}""", state)
    }

    @Test
    fun typingIntoOneFieldIsOneStepButAnotherFieldIsAnother() {
        val first = SetActorStateValue.Input("main", "spawner", "rate", JsonPrimitive(1))
        assertEquals(SetActorStateValue.mergeKey(first), SetActorStateValue.mergeKey(first.copy(value = JsonPrimitive(12))))
        assertNotEquals(SetActorStateValue.mergeKey(first), SetActorStateValue.mergeKey(first.copy(key = "speed")))
    }

    @Test
    fun theBodyIsLeftToItsOwnFieldsAndAMissingActorIsRefused() = runTest {
        val context = context()
        assertEquals(Rejection(CommandProblem.SCENE_CONTENT_INVALID), SetActorStateValue.validate(context, SetActorStateValue.Input("main", "spawner", "body", JsonPrimitive(1))))
        assertNull(SetActorStateValue.validate(context, SetActorStateValue.Input("main", "spawner", "rate", JsonPrimitive(1))))
        assertNotEquals(null, SetActorStateValue.validate(context, SetActorStateValue.Input("main", "ghost", "rate", JsonPrimitive(1))))
    }
}
