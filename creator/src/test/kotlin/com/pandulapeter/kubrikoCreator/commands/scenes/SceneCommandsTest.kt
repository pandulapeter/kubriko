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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.commands.Invocation
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.commands.actorTypes.RenameActorType
import com.pandulapeter.kubrikoCreator.commands.assertRoundTrip
import com.pandulapeter.kubrikoCreator.commands.commandContext
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_command_actor_id_invalid
import kubriko.creator.generated.resources.error_command_actor_id_taken
import kubriko.creator.generated.resources.error_command_actor_type_missing
import kubriko.creator.generated.resources.error_command_index_invalid
import kubriko.creator.generated.resources.error_command_music_missing
import kubriko.creator.generated.resources.error_command_number_invalid
import kubriko.creator.generated.resources.error_command_scene_actor_missing
import kubriko.creator.generated.resources.error_command_scene_actor_without_position
import kubriko.creator.generated.resources.error_command_scene_content_invalid
import kubriko.creator.generated.resources.error_command_scene_invalid
import kubriko.creator.generated.resources.error_command_scene_missing
import kubriko.creator.generated.resources.error_command_size_invalid
import kubriko.creator.generated.resources.error_scene_name_invalid
import kubriko.creator.generated.resources.error_scene_name_taken
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SceneCommandsTest {
    private val ball = ActorTypeDefinition(name = "Ball", width = 30f, height = 40f)
    private val wall = SceneActorEntry("Wall", """{"body":{"position":{"x":1.0,"y":2.0}}}""", "wall")
    private val music = SceneActorEntry("Music", """{"volume":1.0}""", "music")
    private val changedScenes = mutableListOf<String>()

    @OptIn(EditorMutation::class)
    private fun TestScope.context(vararg actors: SceneActorEntry, audioFiles: AudioFiles? = null) = commandContext(ball, audioFiles = audioFiles, onScenesChanged = { changedScenes += it }).apply {
        scenes.write("main", SceneDocument.format(actors.toList()))
    }

    private fun CommandContext.main() = scenes.read("main")?.let(SceneDocument::parse)

    @Test
    fun scenesAreCreatedAndDeleted() = runTest {
        val context = context()
        context.assertRoundTrip(Invocation(CreateScene, CreateScene.Input("level-1"))) { context.scenes.names() }
        assertEquals(listOf("level-1", "main"), context.scenes.names())
        assertEquals("[]", context.scenes.read("level-1"))
        assertEquals(listOf("main"), context.assertRoundTrip(Invocation(DeleteScene, DeleteScene.Input("level-1"))) { context.scenes.names() })
        assertEquals(List(6) { "level-1" }, changedScenes)
    }

    @Test
    fun deletingASceneIsUndoneWithItsContent() = runTest {
        val context = context(wall, music)
        val before = context.scenes.read("main")
        DeleteScene.apply(context, DeleteScene.Input("main")).inverse!!.apply(context)
        assertEquals(before, context.scenes.read("main"))
    }

    @Test
    fun newScenesNeedAFreeValidNameAndValidContent() = runTest {
        val context = context()
        assertEquals(Rejection(Res.string.error_scene_name_invalid), CreateScene.validate(context, CreateScene.Input("my level")))
        assertEquals(Rejection(Res.string.error_scene_name_taken), CreateScene.validate(context, CreateScene.Input("MAIN")))
        assertEquals(Rejection(Res.string.error_command_scene_content_invalid), CreateScene.validate(context, CreateScene.Input("other", "{}")))
        assertEquals(Rejection(Res.string.error_command_scene_missing, "other"), DeleteScene.validate(context, DeleteScene.Input("other")))
    }

    @Test
    fun blockBuiltActorsArePlacedAtTheirActorTypesSize() = runTest {
        val context = context(wall)
        val actors = context.assertRoundTrip(Invocation(PlaceActor, PlaceActor.Input("main", "Ball", 5f, 6f, actorId = "ball"))) { context.main() }!!
        assertEquals(listOf(wall, SceneActorEntry("Ball", SceneDocument.blockBuiltState(5f, 6f, 30f, 40f), "ball")), actors)
        val newId = PlaceActor.apply(context, PlaceActor.Input("main", "Ball", 0f, 0f)).result.jsonObject.getValue("actorId").jsonPrimitive.content
        assertEquals(listOf("wall", "ball", newId), context.main()!!.map { it.id })
        assertEquals(Rejection(Res.string.error_command_actor_id_taken, "wall"), PlaceActor.validate(context, PlaceActor.Input("main", "Ball", 0f, 0f, actorId = "wall")))
        assertEquals(Rejection(Res.string.error_command_actor_id_invalid, "a b"), PlaceActor.validate(context, PlaceActor.Input("main", "Ball", 0f, 0f, actorId = "a b")))
        assertEquals(Rejection(Res.string.error_command_actor_type_missing, "Wall"), PlaceActor.validate(context, PlaceActor.Input("main", "Wall", 0f, 0f)))
        assertEquals(Rejection(Res.string.error_command_number_invalid), PlaceActor.validate(context, PlaceActor.Input("main", "Ball", Float.NaN, 0f)))
        assertEquals(Rejection(Res.string.error_command_scene_missing, "other"), PlaceActor.validate(context, PlaceActor.Input("other", "Ball", 0f, 0f)))
    }

    @Test
    fun actorsAreMovedAndRemovedAndComeBackWithTheirWholeState() = runTest {
        val context = context(wall, music)
        val moved = context.assertRoundTrip(Invocation(MoveActor, MoveActor.Input("main", "wall", 10f, 20f))) { context.main() }!!
        assertEquals(10f to 20f, SceneDocument.positionOf(moved[0].state))
        assertEquals(listOf(moved[0]), context.assertRoundTrip(Invocation(RemoveActor, RemoveActor.Input("main", "music"))) { context.main() })
        assertEquals(listOf(music, moved[0]), context.assertRoundTrip(Invocation(RestoreActor, RestoreActor.Input("main", 0, music.typeId, music.state, "music"))) { context.main() })
    }

    @Test
    fun actorsAreReorderedAndPutBackInTheirPlace() = runTest {
        val context = context(wall, music)
        assertEquals(listOf(music, wall), context.assertRoundTrip(Invocation(MoveActorOrder, MoveActorOrder.Input("main", "wall", 1))) { context.main() })
        assertEquals(listOf(wall, music), context.assertRoundTrip(Invocation(MoveActorOrder, MoveActorOrder.Input("main", "music", 1))) { context.main() })
        assertEquals(Rejection(Res.string.error_command_index_invalid, "2"), MoveActorOrder.validate(context, MoveActorOrder.Input("main", "wall", 2)))
        assertEquals(Rejection(Res.string.error_command_index_invalid, "-1"), MoveActorOrder.validate(context, MoveActorOrder.Input("main", "wall", -1)))
        assertEquals(Rejection(Res.string.error_command_scene_actor_missing, "main", "ghost"), MoveActorOrder.validate(context, MoveActorOrder.Input("main", "ghost", 0)))
    }

    @Test
    fun actorChangesAreValidated() = runTest {
        val context = context(wall, music)
        assertEquals(Rejection(Res.string.error_command_scene_actor_missing, "main", "ghost"), RemoveActor.validate(context, RemoveActor.Input("main", "ghost")))
        assertEquals(Rejection(Res.string.error_command_scene_actor_without_position, "music"), MoveActor.validate(context, MoveActor.Input("main", "music", 0f, 0f)))
        assertEquals(Rejection(Res.string.error_command_actor_id_taken, "wall"), RestoreActor.validate(context, RestoreActor.Input("main", 0, "Wall", "{}", "wall")))
        assertEquals(Rejection(Res.string.error_command_index_invalid, "3"), RestoreActor.validate(context, RestoreActor.Input("main", 3, "Wall", "{}")))
        assertEquals(Rejection(Res.string.error_command_scene_content_invalid), RestoreActor.validate(context, RestoreActor.Input("main", 0, "Wall", "[]")))
        @OptIn(EditorMutation::class)
        context.scenes.write("main", "broken")
        assertEquals(Rejection(Res.string.error_command_scene_invalid, "main"), RemoveActor.validate(context, RemoveActor.Input("main", "wall")))
    }

    @Test
    fun sceneSettingsAreAddedAtTheEndWhenTheSceneHasNone() = runTest {
        val context = context(wall)
        val actors = context.assertRoundTrip(Invocation(SetSceneSettings, SetSceneSettings.Input("main", width = 800f, backgroundColor = 0xFF000000L))) { context.main() }!!
        assertEquals(listOf("Wall", "SceneSettings"), actors.map { it.typeId })
        assertEquals(SceneSettingsState.Values(width = 800f, backgroundColor = 0xFF000000L), SceneSettingsState.read(actors[1].state))
    }

    @Test
    fun existingSceneSettingsChangeInPlaceAndOnlyWhatIsGiven() = runTest {
        val settings = SceneActorEntry("SceneSettings", SceneSettingsState.withChanges(x = 1f, cameraX = 2f))
        val context = context(settings, wall)
        val actors = context.assertRoundTrip(Invocation(SetSceneSettings, SetSceneSettings.Input("main", y = 5f, height = 100f, cameraY = -3f))) { context.main() }!!
        assertEquals(listOf("SceneSettings", "Wall"), actors.map { it.typeId })
        assertEquals(SceneSettingsState.Values(x = 1f, y = 5f, height = 100f, cameraX = 2f, cameraY = -3f), SceneSettingsState.read(actors[0].state))
    }

    @Test
    fun sceneSettingsAreValidated() = runTest {
        val context = context()
        assertEquals(Rejection(Res.string.error_command_number_invalid), SetSceneSettings.validate(context, SetSceneSettings.Input("main", cameraX = Float.POSITIVE_INFINITY)))
        assertEquals(Rejection(Res.string.error_command_size_invalid), SetSceneSettings.validate(context, SetSceneSettings.Input("main", width = 0f)))
        assertEquals(Rejection(Res.string.error_command_scene_missing, "other"), SetSceneSettings.validate(context, SetSceneSettings.Input("other")))
    }

    @Test
    fun aScenesMusicIsSetAndClearedAndMustExist() = runTest {
        val context = context(wall, audioFiles = AudioFiles(music = listOf("files/music/theme.mp3")))
        val actors = context.assertRoundTrip(Invocation(SetSceneSettings, SetSceneSettings.Input("main", music = "files/music/theme.mp3"))) { context.main() }!!
        assertEquals("files/music/theme.mp3", SceneSettingsState.read(actors[1].state).music)
        val cleared = context.assertRoundTrip(Invocation(SetSceneSettings, SetSceneSettings.Input("main", music = ""))) { context.main() }!!
        assertEquals("", SceneSettingsState.read(cleared[1].state).music)
        assertEquals(
            Rejection(Res.string.error_command_music_missing, "files/music/ghost.mp3"),
            SetSceneSettings.validate(context, SetSceneSettings.Input("main", music = "files/music/ghost.mp3")),
        )
    }

    @Test
    fun renamingAnActorTypeRenamesItsActorsInEveryScene() = runTest {
        val context = context(SceneActorEntry("Ball", "{}"), wall)
        changedScenes.clear()
        val actors = context.assertRoundTrip(Invocation(RenameActorType, RenameActorType.Input("Ball", "Puck"))) { context.main() }!!
        assertEquals(listOf("Puck", "Wall"), actors.map { it.typeId })
        assertEquals(listOf("main", "main", "main"), changedScenes)
        assertNull(context.blockTypes!!["Ball"])
    }
}
