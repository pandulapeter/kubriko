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

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.BlockTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.EventDefinition
import com.pandulapeter.kubrikoCreator.actors.EventType
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class TypeEditorContextCacheTest {
    private val enemy = ActorTypeDefinition(
        name = "Enemy",
        properties = listOf(PropertyDefinition.Number("hp")),
        events = listOf(EventDefinition(EventType.EVERY_FRAME), EventDefinition(EventType.ADDED_TO_SCENE)),
    )
    private val bat = ActorTypeDefinition(name = "Bat", parent = "Enemy", events = listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("a")))))
    private val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score")))
    private val types = listOf(enemy, bat, game)
    private val sceneNames = listOf("Level")
    private val audioFiles = AudioFiles(sounds = listOf("files/sounds/hit.wav"))
    private val cache = TypeEditorContextCache()

    @Test
    fun typingIntoABlockKeepsTheScopeAndTheInheritance() {
        val before = cache.contextOf(bat, types, sceneNames, audioFiles)
        val typed = bat.withEvents(listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.Log("ab")))))
        val after = cache.contextOf(typed, listOf(enemy, typed, game), sceneNames, audioFiles)
        assertSame(before.scope, after.scope)
        assertSame(before.inheritance, after.inheritance)
    }

    @Test
    fun theContextIsWhatTheScopeAndTheInheritanceWouldBe() {
        types.forEach { definition ->
            assertEquals(
                TypeEditorContext(BlockScope.of(definition, types, sceneNames, audioFiles), Inheritance.of(definition, types)),
                cache.contextOf(definition, types, sceneNames, audioFiles),
            )
        }
    }

    @Test
    fun aNewPropertyOrTimerBuildsTheScopeAgain() {
        val before = cache.contextOf(bat, types, sceneNames, audioFiles)
        val withProperty = enemy.withProperties(enemy.properties + PropertyDefinition.Text("cry"))
        val afterProperty = cache.contextOf(bat, listOf(withProperty, bat, game), sceneNames, audioFiles)
        assertNotSame(before.scope, afterProperty.scope)
        assertEquals(listOf("hp", "cry"), afterProperty.scope.properties.map { it.name })
        val withTimer = bat.withEvents(listOf(EventDefinition(EventType.EVERY_FRAME, blocks = listOf(BlockDefinition.StartTimer(timer = "spawn")))))
        val afterTimer = cache.contextOf(withTimer, listOf(withProperty, withTimer, game), sceneNames, audioFiles)
        assertEquals(listOf("spawn"), afterTimer.scope.timerNames)
    }

    @Test
    fun aManagerTypesPropertyBuildsTheScopeAgain() {
        cache.contextOf(bat, types, sceneNames, audioFiles)
        val renamed = game.withProperties(listOf(PropertyDefinition.Number("points")))
        val after = cache.contextOf(bat, listOf(enemy, bat, renamed), sceneNames, audioFiles)
        assertEquals(listOf("points"), after.scope.propertiesOf("Game")?.map { it.name })
    }

    @Test
    fun scenesAndAudioFilesBuildTheScopeAgain() {
        val before = cache.contextOf(bat, types, sceneNames, audioFiles)
        assertEquals(listOf("Level", "Menu"), cache.contextOf(bat, types, sceneNames + "Menu", audioFiles).scope.sceneNames)
        val afterAudio = cache.contextOf(bat, types, sceneNames + "Menu", AudioFiles())
        assertEquals(AudioFiles(), afterAudio.scope.audioFiles)
        assertSame(before.inheritance, afterAudio.inheritance)
    }

    @Test
    fun theInheritanceFollowsAnAncestorsEventsAndTheTypesOwnTriggers() {
        val before = cache.contextOf(bat, types, sceneNames, audioFiles)
        val parentChanged = enemy.withEvents(enemy.events + EventDefinition(EventType.TIMER_FINISHED, timer = "spawn"))
        val afterParent = cache.contextOf(bat, listOf(parentChanged, bat, game), sceneNames, audioFiles)
        assertEquals(Inheritance.of(bat, listOf(parentChanged, bat, game)), afterParent.inheritance)
        assertNotSame(before.inheritance, afterParent.inheritance)
        val replacing = bat.withEvents(bat.events + EventDefinition(EventType.ADDED_TO_SCENE))
        val typesNow: List<BlockTypeDefinition> = listOf(parentChanged, replacing, game)
        assertEquals(Inheritance.of(replacing, typesNow), cache.contextOf(replacing, typesNow, sceneNames, audioFiles).inheritance)
    }

    @Test
    fun anotherTypeGetsAContextOfItsOwn() {
        cache.contextOf(bat, types, sceneNames, audioFiles)
        assertEquals("Game", cache.contextOf(game, types, sceneNames, audioFiles).scope.typeName)
        assertEquals(Inheritance(), cache.contextOf(game, types, sceneNames, audioFiles).inheritance)
    }
}
