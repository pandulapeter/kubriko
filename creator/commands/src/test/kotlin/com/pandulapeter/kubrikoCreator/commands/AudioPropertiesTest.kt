/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SoundValue
import com.pandulapeter.kubrikoCreator.commands.actorTypes.AddProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RemoveProperty
import com.pandulapeter.kubrikoCreator.commands.resources.DeleteResource
import com.pandulapeter.kubrikoCreator.context.BlockNotation
import com.pandulapeter.kubrikoCreator.context.RelationKind
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.UsageEffect
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Sound and music Properties, which name a file: what deleting the file and the Property does, and what may start as one.
 */
class AudioPropertiesTest {
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Sound("cry", "files/sounds/hit.wav")),
        events = listOf(EventDefinition(EventType.ADDED_TO_SCENE, blocks = listOf(BlockDefinition.PlaySound(SoundValue.Property("cry"))))),
    )

    private fun TestScope.context() = commandContext(enemy, audioFiles = AudioFiles(sounds = listOf("files/sounds/hit.wav"))).apply {
        File(project.rootDirectory, "src/commonMain/composeResources/files/sounds/hit.wav").apply { parentFile.mkdirs() }.writeBytes(byteArrayOf(1))
        writeScene("level", "[{\"typeId\":\"Enemy\",\"state\":\"{\\\"cry\\\":\\\"files/sounds/hit.wav\\\"}\"}]")
    }

    @Test
    fun deletingTheFileClearsWhatStartsAsItAndWhatScenesSave() = runTest {
        val context = context()
        assertEquals(
            listOf(RelationKind.STARTS_AS to UsageEffect.STARTING_VALUE_CLEARED, RelationKind.SAVES_REFERENCE_TO to UsageEffect.SAVED_REFERENCE_CLEARED),
            context.usagesOf(RelationSubject.Resource("files/sounds/hit.wav")).map { it.kind to it.effect },
        )
        val after = context.assertRoundTrip(Invocation(DeleteResource, DeleteResource.Input("files/sounds/hit.wav"))) { context.snapshot() }.first
        assertEquals(listOf(PropertyDefinition.Sound("cry")), after.single().properties)
        assertEquals(listOf("""{"cry":""}"""), SceneDocument.parse(context.sceneFile("level").readText()).map { it.state })
    }

    @Test
    fun deletingThePropertyPlaysWhatItStartedAs() = runTest {
        val context = context()
        val after = context.assertRoundTrip(Invocation(RemoveProperty, RemoveProperty.Input("Enemy", "cry"))) { context.blockTypes!!.types.value }
        assertEquals(listOf(BlockDefinition.PlaySound(SoundValue.Fixed("files/sounds/hit.wav"))), after.single().events.single().blocks)
    }

    @Test
    fun aPropertyStartsAsAFileOfItsKindThatExistsOrAsNone() = runTest {
        val context = context()
        fun rejection(property: PropertyDefinition) = AddProperty.validate(context, AddProperty.Input("Enemy", property))?.reason
        assertEquals(LookupProblem.SOUND_MISSING, rejection(PropertyDefinition.Sound("boom", "files/sounds/boom.wav")))
        assertEquals(null, rejection(PropertyDefinition.Sound("boom")))
        assertEquals("Play sound the sound in cry", BlockNotation.of(enemy.events.single().blocks.single()))
    }

    private fun CommandContext.sceneFile(name: String) = File(project.rootDirectory, "src/commonMain/composeResources/files/scenes/$name.json")

    private fun CommandContext.writeScene(name: String, text: String) {
        sceneFile(name).apply { parentFile.mkdirs() }.writeText(text)
        scenes.refresh()
    }

    private fun CommandContext.snapshot() = blockTypes!!.types.value to project.rootDirectory.walkTopDown().filter { it.isFile }.associate { it.path to it.readText() }
}
