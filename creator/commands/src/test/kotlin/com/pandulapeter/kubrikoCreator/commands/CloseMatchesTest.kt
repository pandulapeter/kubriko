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
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.KeyChoice
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.NumberValue
import com.pandulapeter.kubrikoCreator.actors.PlacementProblem
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.commands.actorTypes.SetEventKey
import com.pandulapeter.kubrikoCreator.commands.actorTypes.scopeOf
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateActorType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateBlock
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateEvent
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateManagerType
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateProperty
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateSprite
import com.pandulapeter.kubrikoCreator.commands.actorTypes.validateType
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.commands.folders.validateFolder
import com.pandulapeter.kubrikoCreator.commands.game.unknownKeyName
import com.pandulapeter.kubrikoCreator.commands.managerTypes.validateTextIndex
import com.pandulapeter.kubrikoCreator.commands.scenes.validateScene
import com.pandulapeter.kubrikoCreator.commands.scenes.validateSceneActor
import com.pandulapeter.kubrikoCreator.commands.screenplay.ReadPages
import com.pandulapeter.kubrikoCreator.commands.screenplay.missingSection
import com.pandulapeter.kubrikoCreator.commands.screenplay.validateDocument
import com.pandulapeter.kubrikoCreator.commands.screenplay.validatePage
import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.scenes.SceneActorEntry
import com.pandulapeter.kubrikoCreator.scenes.SceneDocument
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class CloseMatchesTest {
    private val paddle = ActorTypeDefinition(
        name = "Paddle",
        properties = listOf(PropertyDefinition.Number("speed"), PropertyDefinition.Text("label")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME)),
    )
    private val score = ManagerTypeDefinition(name = "Score", properties = listOf(PropertyDefinition.Number("points")))

    @OptIn(EditorMutation::class)
    private fun TestScope.context() = commandContext(
        paddle,
        score,
        spriteFiles = SpriteFiles(mapOf("drawable/paddle.png" to File("paddle.png"))),
        documents = listOf("docs/combat.md"),
    ).apply {
        scenes.write("levels/first", SceneDocument.format(listOf(SceneActorEntry("Paddle", "{}", "leftPaddle"))))
        scenes.takeChanges()
        File(project.rootDirectory, "screenplay/heroes.md").apply { parentFile.mkdirs() }.writeText("# Heroes\n")
        screenplay.refresh()
    }

    @Test
    fun typesOfferTheCloseNamesOrSayWhichKindTheNameIs() = runTest {
        val context = context()
        assertEquals(Rejection(LookupProblem.TYPE_MISSING_SIMILAR, "paddle", "Paddle"), context.validateType("paddle"))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_MISSING_SIMILAR, "Padle", "Paddle"), context.validateActorType("Padle"))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_IS_MANAGER_TYPE, "Score"), context.validateActorType("Score"))
        assertEquals(Rejection(LookupProblem.MANAGER_TYPE_MISSING_SIMILAR, "Scores", "Score"), context.validateManagerType("Scores"))
        assertEquals(Rejection(LookupProblem.MANAGER_TYPE_IS_ACTOR_TYPE, "Paddle"), context.validateManagerType("Paddle"))
        assertEquals(Rejection(LookupProblem.ACTOR_TYPE_MISSING, "Ghost"), context.validateActorType("Ghost"))
    }

    @Test
    fun whatATypeHoldsOffersTheCloseNamesOrTheIndexesThereAre() = runTest {
        val context = context()
        assertEquals(Rejection(LookupProblem.PROPERTY_UNKNOWN_SIMILAR, "Paddle", "sped", "speed"), context.validateProperty("Paddle", "sped"))
        assertEquals(Rejection(LookupProblem.EVENT_MISSING, "Paddle", "1", "0"), context.validateEvent("Paddle", 1))
        assertEquals(Rejection(LookupProblem.EVENT_MISSING_NONE, "Score", "0"), context.validateEvent("Score", 0))
        assertEquals(Rejection(LookupProblem.TEXT_MISSING_NONE, "Score", "0"), context.validateTextIndex("Score", 0))
    }

    @Test
    fun blocksAndEventsOfferTheCloseNamesOfWhatTheyName() = runTest {
        val context = context()
        val scope = context.scopeOf(paddle)
        assertEquals(
            Rejection(PlacementProblem.NUMBER_PROPERTY_MISSING_SIMILAR, "sped", "Paddle", "speed"),
            validateBlock(BlockDefinition.SetNumber("sped", NumberValue.Fixed(1f)), EventType.EVERY_FRAME, scope),
        )
        assertEquals(
            Rejection(PlacementProblem.TEXT_PROPERTY_OF_OTHER_KIND, "speed", "Paddle"),
            validateBlock(BlockDefinition.SetText("speed", ""), EventType.EVERY_FRAME, scope),
        )
        assertEquals(
            Rejection(LookupProblem.SCENE_MISSING_SIMILAR, "first", "levels/first"),
            validateBlock(BlockDefinition.GoToScene("first"), EventType.EVERY_FRAME, scope),
        )
        assertEquals(
            Rejection(LookupProblem.GAME_KEY_UNKNOWN_SIMILAR, "Space", "Spacebar, Backspace"),
            validateEvent(EventDefinition(EventType.KEY_PRESSED, key = KeyChoice("Space")), scope),
        )
        assertEquals(
            Rejection(LookupProblem.ACTOR_TYPE_IS_MANAGER_TYPE, "Score"),
            validateEvent(EventDefinition(EventType.COLLISION, otherActorType = "Score"), scope),
        )
        val keyPressed = commandContext(paddle.copy(events = listOf(EventDefinition(EventType.KEY_PRESSED, key = KeyChoice.A))))
        assertEquals(
            Rejection(LookupProblem.GAME_KEY_UNKNOWN_SIMILAR, "Space", "Spacebar, Backspace"),
            SetEventKey.validate(keyPressed, SetEventKey.Input("Paddle", 0, KeyChoice("Space"))),
        )
    }

    @Test
    fun filesScenesAndActorsOfferTheCloseNames() = runTest {
        val context = context()
        assertEquals(
            Rejection(LookupProblem.SCENE_MISSING_SIMILAR, "first", "levels/first"),
            ReplaceScenes.validate(context, ReplaceScenes.Input(mapOf("first" to "[]"))),
        )
        assertEquals(Rejection(LookupProblem.SCENE_MISSING_SIMILAR, "first", "levels/first"), context.validateScene("first"))
        assertEquals(
            Rejection(LookupProblem.SCENE_ACTOR_MISSING_SIMILAR, "levels/first", "leftpaddle", "leftPaddle"),
            context.validateSceneActor("levels/first", "leftpaddle"),
        )
        assertEquals(Rejection(LookupProblem.SPRITE_MISSING_SIMILAR, "drawable/paddle", "drawable/paddle.png"), context.validateSprite("drawable/paddle"))
        assertEquals(Rejection(LookupProblem.PAGE_MISSING_SIMILAR, "Heroes", "heroes"), context.validatePage("Heroes"))
        assertEquals(
            CommandOutcome.Rejected(Rejection(LookupProblem.PAGE_MISSING_SIMILAR, "heros", "heroes")),
            ReadPages.read(context, ReadPages.Input(listOf("heros"))),
        )
        assertEquals(Rejection(LookupProblem.DOCUMENT_MISSING_SIMILAR, "combat.md", "docs/combat.md"), context.validateDocument("combat.md"))
        assertEquals(Rejection(LookupProblem.FOLDER_MISSING_SIMILAR, "level", "levels"), context.validateFolder(FolderGroup.SCENES, "level", setOf("levels")))
    }

    @Test
    fun toolsAndKeysOfferTheCloseNames() {
        assertEquals(Rejection(LookupProblem.UNKNOWN_SIMILAR, "place_actr", "place_actor, place_actors"), CommandRegistry.editor.missing("place_actr"))
        assertEquals(Rejection(LookupProblem.GAME_KEY_UNKNOWN_SIMILAR, "SPACEBAR", "SPACE"), unknownKeyName("SPACEBAR"))
    }

    @Test
    fun aMissingSectionNamesTheSectionsOrOnlyTheCloseOnesOfALongDocument() {
        assertEquals(
            Rejection(LookupProblem.PAGE_SECTION_MISSING_LISTED, "hero", "Enemy", "\"Enemies, bosses\", \"Planned\""),
            missingSection("hero", "Enemy", "# Hero\n\n## Enemies, bosses\n\n## Planned\n"),
        )
        val long = (1..30).joinToString("\n\n") { "## Chapter $it" } + "\n\n## Ending notes\n"
        assertEquals(Rejection(LookupProblem.PAGE_SECTION_MISSING_SIMILAR, "book.md", "Ending note", "\"Ending notes\""), missingSection("book.md", "Ending note", long))
        assertEquals(Rejection(LookupProblem.PAGE_SECTION_MISSING, "book.md", "Credits"), missingSection("book.md", "Credits", long))
        assertEquals(Rejection(LookupProblem.PAGE_SECTION_MISSING, "hero", "Ending"), missingSection("hero", "Ending", "# Hero\n"))
    }
}
