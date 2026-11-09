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

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.AudioFiles
import com.pandulapeter.kubrikoCreator.actors.BlockDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockNesting
import com.pandulapeter.kubrikoCreator.actors.BlockScope
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.PropertyDefinition
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BlockRowTest {
    private val ball = ActorTypeDefinition(name = "Ball")

    @Test
    fun clickingASwitchLabelTogglesIt() = uiTest(width = 520) {
        var block by mutableStateOf(BlockDefinition.StartTimer())
        showing {
            BlockRow(
                block = block,
                nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
                scope = BlockScope.of(ball, listOf(ball)),
                index = 0,
                dragState = BlockDragState(onDrop = { _, _ -> }),
                onBlockChange = { block = it as BlockDefinition.StartTimer },
                onRemove = {},
            )
        }
        onNodeWithText("Repeating").assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).performClick()
        waitForIdle()
        assertEquals(true, block.isRepeating)
        saveSnapshot("block-row-repeating")
        onNodeWithText("Repeating").performClick()
        waitForIdle()
        assertEquals(false, block.isRepeating)
    }

    @Test
    fun aBlockNamingWhatNoLongerExistsSaysSo() = uiTest(width = 520, height = 360) {
        val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score")))
        val scope = BlockScope.of(ball, listOf(ball, game), sceneNames = listOf("level"), audioFiles = AudioFiles(sounds = listOf("files/sounds/hit.wav")))
        val blocks = listOf(
            BlockDefinition.AddActor("Brick"),
            BlockDefinition.GoToScene("cave"),
            BlockDefinition.PlaySound("files/sounds/gone.wav"),
            BlockDefinition.SetNumber("lives", manager = "Game"),
            BlockDefinition.AddActor("Ball"),
            BlockDefinition.GoToScene("level"),
            BlockDefinition.SetNumber("score", manager = "Game"),
        )
        showing {
            Column {
                blocks.forEachIndexed { index, block ->
                    BlockRow(block = block, nesting = BlockNesting(0, BlockNesting.Role.PLAIN), scope = scope, index = index, dragState = BlockDragState(onDrop = { _, _ -> }), onBlockChange = {}, onRemove = {})
                }
            }
        }
        listOf("Brick", "cave", "sounds/gone.wav").forEach { onNodeWithText("$it no longer exists").assertExists() }
        onAllNodesWithText("no longer exists", substring = true).assertCountEquals(4)
        saveSnapshot("block-rows-missing")
    }

    @Test
    fun aManagerTypesOwnPropertyNamedByItsTypeIsNotMissing() = uiTest(width = 520) {
        val game = ManagerTypeDefinition(name = "Game", properties = listOf(PropertyDefinition.Number("score")))
        showing {
            BlockRow(
                block = BlockDefinition.SetNumber("score", manager = "Game"),
                nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
                scope = BlockScope.of(game, listOf(game)),
                index = 0,
                dragState = BlockDragState(onDrop = { _, _ -> }),
                onBlockChange = {},
                onRemove = {},
            )
        }
        onAllNodesWithText("no longer exists", substring = true).assertCountEquals(0)
    }

    @Test
    fun anAudioMenuOfAProjectWithoutSuchFilesSaysHowToAddSome() = uiTest(width = 520, height = 360) {
        showAudioBlock(BlockDefinition.PlaySound(), AudioFiles(music = listOf("files/music/theme.mp3")))
        onNodeWithText("Choose a sound").performClick()
        onNodeWithText(SOUNDS_NONE_YET).assertExists()
        onNodeWithText("Nothing matches.").assertDoesNotExist()
        saveSnapshot("block-sound-menu-none-yet")
    }

    @Test
    fun anAudioMenuBeforeTheProjectIsReadSaysSoRatherThanThatThereAreNone() = uiTest(width = 520, height = 360) {
        showAudioBlock(BlockDefinition.PlayMusic(), audioFiles = null)
        onNodeWithText("Choose music").performClick()
        onNodeWithText("The project's files are still being read.").assertExists()
        onNodeWithText("No music yet", substring = true).assertDoesNotExist()
    }

    @Test
    fun anAudioMenuWithFilesListsThemWithoutAHint() = uiTest(width = 520, height = 360) {
        showAudioBlock(BlockDefinition.PlaySound(), AudioFiles(sounds = listOf("files/sounds/hit.wav")))
        onNodeWithText("Choose a sound").performClick()
        onNodeWithText("sounds/hit.wav").assertExists()
        onNodeWithText(SOUNDS_NONE_YET).assertDoesNotExist()
    }

    private fun ComposeUiTest.showAudioBlock(block: BlockDefinition, audioFiles: AudioFiles?) = showing {
        BlockRow(
            block = block,
            nesting = BlockNesting(0, BlockNesting.Role.PLAIN),
            scope = BlockScope.of(ball, listOf(ball), audioFiles = audioFiles),
            index = 0,
            dragState = BlockDragState(onDrop = { _, _ -> }),
            onBlockChange = {},
            onRemove = {},
        )
    }

    private companion object {
        const val SOUNDS_NONE_YET = "No sounds yet. Put .wav files in the files folder of composeResources, like files/sounds/hit.wav, to play them."
    }
}
