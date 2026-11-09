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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import com.pandulapeter.kubrikoCreator.commands.scenes.SetSceneSettings
import com.pandulapeter.kubrikoCreator.scenes.SceneSettingsState
import com.pandulapeter.kubrikoCreator.testing.saveSnapshot
import com.pandulapeter.kubrikoCreator.testing.showing
import com.pandulapeter.kubrikoCreator.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SceneSettingsFieldsTest {
    private val music = (1..20).map { "files/music/level$it.mp3" } + "files/music/boss/final.mp3"

    @Test
    fun theMusicIsChosenFromASearchableListShowingFolders() = uiTest(width = 400, height = 600) {
        val changes = mutableListOf<SetSceneSettings.Input>()
        showing {
            SceneSettingsFields(values = SceneSettingsState.Values(), music = music, onChange = { changes += it(SetSceneSettings.Input(scene = "main")) })
        }
        onNodeWithText("None").performClick()
        onNode(hasSetTextAction() and isFocused()).performTextInput("boss")
        onNodeWithText("music/boss/final.mp3").assertExists()
        onNodeWithText("music/level1.mp3").assertDoesNotExist()
        saveSnapshot("scene-settings-music-search")
        onNode(hasSetTextAction() and isFocused()).performKeyInput { pressKey(Key.Enter) }
        assertEquals("files/music/boss/final.mp3", changes.single().music)
    }

    @Test
    fun musicThatIsGoneIsShownAsSuch() = uiTest(width = 400, height = 600) {
        showing {
            SceneSettingsFields(values = SceneSettingsState.Values(music = "files/music/deleted.mp3"), music = music, onChange = {})
        }
        onNodeWithText("music/deleted.mp3 no longer exists").assertExists()
    }

    @Test
    fun musicIsntMarkedAsGoneBeforeTheProjectIsScanned() = uiTest(width = 400, height = 600) {
        showing {
            SceneSettingsFields(values = SceneSettingsState.Values(music = "files/music/theme.mp3"), music = null, onChange = {})
        }
        onNodeWithText("music/theme.mp3").assertExists()
    }

    @Test
    fun aProjectWithoutMusicOffersNoneAndSaysHowToAddSome() = uiTest(width = 400, height = 600) {
        showing {
            SceneSettingsFields(values = SceneSettingsState.Values(), music = emptyList(), onChange = {})
        }
        onNodeWithText("None").performClick()
        onNodeWithText("No music yet. Put .mp3 or .ogg files in the files folder of composeResources, like files/music/theme.mp3, to play them.").assertExists()
        saveSnapshot("scene-settings-music-none-yet")
    }
}
