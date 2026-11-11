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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.context.HealthFinding
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import com.pandulapeter.kubrikoCreator.ui.actorTypes.BlockTypeEditor
import com.pandulapeter.kubrikoCreator.ui.actorTypes.Inheritance
import com.pandulapeter.kubrikoCreator.ui.actorTypes.SpriteChoices
import com.pandulapeter.kubrikoCreator.ui.screenplay.PageEditor
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.ic_actor_type
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HealthHintsTest {
    private val unmentionedBall = HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Type("Ball"))

    @Test
    fun theScreenplaysFindingsGoToTheRowsTheyAreAboutAndTheOthersDont() {
        val hints = healthHintsOf(
            findings = listOf(
                unmentionedBall,
                HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Type("Game")),
                HealthFinding(HealthFinding.Kind.UNMENTIONED, RelationSubject.Scene("main")),
                HealthFinding(HealthFinding.Kind.PAGE_WITHOUT_TITLE, RelationSubject.Page("hero")),
                HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, RelationSubject.Property("Ball", "speed")),
                HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type("Ball")),
                HealthFinding(HealthFinding.Kind.UNUSED_PAGE_IMAGE, RelationSubject.Image("screenplay/a.png")),
            ),
            types = listOf(ActorTypeDefinition(name = "Ball"), ManagerTypeDefinition(name = "Game")),
        )
        assertEquals(listOf(unmentionedBall), hints.ofRow(FolderGroup.ACTOR_TYPES, "Ball"))
        assertEquals(1, hints.ofRow(FolderGroup.MANAGER_TYPES, "Game").size)
        assertEquals(1, hints.ofRow(FolderGroup.SCENES, "main").size)
        assertEquals(1, hints.ofRow(FolderGroup.PAGES, "hero").size)
        assertEquals(emptyList(), hints.ofRow(FolderGroup.ACTOR_TYPES, "Game"))
    }

    @Test
    fun aRowWithAHintShowsAQuietMarker() = uiTest(width = 360, height = 80) {
        showing {
            CompositionLocalProvider(LocalHealthHints provides HealthHints()) {
                SidebarItem(icon = Res.drawable.ic_actor_type, text = "Ball", onClick = {}, hints = listOf(unmentionedBall))
            }
        }
        onNodeWithText("Ball").assertExists()
        saveSnapshot("sidebar-hint")
    }

    @Test
    fun everyFindingGoesToTheEditorOfWhatItIsAboutAPropertysToItsType() {
        val unreadSpeed = HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, RelationSubject.Property("Ball", "speed"))
        val unusedBall = HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type("Ball"))
        val unreachedBonus = HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene("bonus"), "main")
        val untitledHero = HealthFinding(HealthFinding.Kind.PAGE_WITHOUT_TITLE, RelationSubject.Page("hero"))
        val hints = healthHintsOf(
            findings = listOf(unmentionedBall, unreadSpeed, unusedBall, unreachedBonus, untitledHero, HealthFinding(HealthFinding.Kind.UNUSED_PAGE_IMAGE, RelationSubject.Image("screenplay/a.png"))),
            types = listOf(ActorTypeDefinition(name = "Ball")),
        )
        assertEquals(listOf(unmentionedBall, unreadSpeed, unusedBall), hints.ofPlace(HealthFindingPlace.Type("Ball")))
        assertEquals(listOf(unreachedBonus), hints.ofPlace(HealthFindingPlace.Scene("bonus")))
        assertEquals(listOf(untitledHero), hints.ofPlace(HealthFindingPlace.Page("hero")))
        assertEquals(emptyList(), hints.ofPlace(HealthFindingPlace.Scene("main")))
        assertEquals(emptyList(), hints.ofRow(FolderGroup.SCENES, "bonus"), "Only the screenplay's findings mark the sidebar's rows.")
    }

    @Test
    fun aMissingFirstSceneGoesToTheGameSettingsAndAnUnreachedSceneThatMayBeTheFirstNowhere() {
        val missing = HealthFinding(HealthFinding.Kind.FIRST_SCENE_MISSING, RelationSubject.Scene("intro"))
        val hints = healthHintsOf(listOf(missing, HealthFinding(HealthFinding.Kind.UNREACHED_SCENE, RelationSubject.Scene("main"))), emptyList())
        assertEquals(listOf(missing), hints.ofPlace(HealthFindingPlace.GameSettings))
        assertEquals(emptyList(), hints.ofPlace(HealthFindingPlace.Scene("intro")))
        assertEquals(emptyList(), hints.ofPlace(HealthFindingPlace.Scene("main")), "Without a known first Scene, main may well be it.")
    }

    @Test
    fun aMissingFirstSceneIsANoticeOpeningTheGameSettings() = uiTest(width = 900, height = 80) {
        var opened = 0
        showing { GameSettingsNotice(findings = listOf(HealthFinding(HealthFinding.Kind.FIRST_SCENE_MISSING, RelationSubject.Scene("intro"))), onOpenSettings = { opened++ }) }
        onNodeWithText("The game starts with intro, which doesn't exist. Choose another in Project settings.").assertExists()
        onNodeWithText("Project settings").performClick()
        assertEquals(1, opened)
        saveSnapshot("game-settings-notice")
    }

    @Test
    fun anEditorShowsTheFirstFewHintsAndTheRestOnAClick() = uiTest(width = 480, height = 220) {
        val findings = listOf("speed", "lives", "combo", "bonus").map { HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, RelationSubject.Property("Game", it)) }
        showing {
            CompositionLocalProvider(LocalHealthHints provides healthHintsOf(findings, listOf(ManagerTypeDefinition(name = "Game")))) {
                HealthHintsList(place = HealthFindingPlace.Type("Game"))
            }
        }
        onNodeWithText("Worth a look").assertExists()
        onNodeWithText("combo", substring = true).assertExists()
        onNodeWithText("bonus", substring = true).assertDoesNotExist()
        saveSnapshot("health-hints-collapsed")
        onNodeWithText("Show 1 more").performClick()
        onNodeWithText("bonus", substring = true).assertExists()
        onNodeWithText("Show fewer").assertExists()
        saveSnapshot("health-hints-expanded")
    }

    @Test
    fun anEditorWithNothingWorthALookShowsNothing() = uiTest(width = 360, height = 80) {
        showing {
            CompositionLocalProvider(LocalHealthHints provides healthHintsOf(listOf(unmentionedBall), emptyList())) {
                HealthHintsList(place = HealthFindingPlace.Type("Paddle"))
            }
        }
        onNodeWithText("Worth a look").assertDoesNotExist()
    }

    @Test
    fun theTypeEditorShowsWhatAboutTheTypeAndItsPropertiesIsWorthALook() = uiTest(width = 1200, height = 500) {
        val ball = ActorTypeDefinition(name = "Ball")
        val findings = listOf(
            HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type("Ball")),
            HealthFinding(HealthFinding.Kind.UNREAD_PROPERTY, RelationSubject.Property("Ball", "speed")),
            HealthFinding(HealthFinding.Kind.UNUSED_ACTOR_TYPE, RelationSubject.Type("Brick")),
        )
        showing {
            CompositionLocalProvider(LocalHealthHints provides healthHintsOf(findings, listOf(ball))) {
                BlockTypeEditor(
                    definition = ball,
                    scope = BlockScope.of(ball, listOf(ball)),
                    sprites = SpriteChoices(),
                    inheritance = Inheritance(),
                    isChangedByHand = false,
                    onCommand = { _, _ -> },
                    currentDefinition = { ball },
                    onCommandIfValid = { null },
                    onTypeSelected = {},
                )
            }
        }
        onNodeWithText("Ball: no Scene places it and no Block adds it.").assertExists()
        onNodeWithText("nothing reads it", substring = true).assertExists()
        onNodeWithText("Brick", substring = true).assertDoesNotExist()
        saveSnapshot("type-editor-health-hints")
    }

    @Test
    fun aPageShowsWhatAboutItIsWorthALookButADocumentDoesnt() = uiTest(width = 900, height = 400) {
        val hints = healthHintsOf(listOf(HealthFinding(HealthFinding.Kind.PAGE_WITHOUT_SUMMARY, RelationSubject.Page("hero"))), emptyList())
        var showsWritingHints by mutableStateOf(true)
        showing {
            CompositionLocalProvider(LocalHealthHints provides hints) {
                PageEditor(name = "hero", text = "# Hero\n\n## Looks\n\nTall.\n", onTextChange = {}, showsWritingHints = showsWritingHints)
            }
        }
        onNodeWithText("hero has no one-sentence summary under its title.").assertExists()
        saveSnapshot("page-editor-health-hints")
        showsWritingHints = false
        onNodeWithText("hero has no one-sentence summary under its title.").assertDoesNotExist()
    }
}
