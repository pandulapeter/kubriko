/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.pandulapeter.kubrikoCreator.commands.project.SetProjectSettings
import com.pandulapeter.kubrikoCreator.project.GameIdentity
import com.pandulapeter.kubrikoCreator.ui.saveSnapshot
import com.pandulapeter.kubrikoCreator.ui.showing
import com.pandulapeter.kubrikoCreator.ui.uiTest
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_identity_version_number_invalid
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProjectSettingsDialogTest {
    private val identity = GameIdentity("Breakout", "1.0.0", 3, "com.example.breakout", "Pandula", "")

    @Test
    fun typingAValidValueChangesItAndAnInvalidOneSaysWhy() = uiTest(width = 640, height = 900) {
        val changes = mutableListOf<SetProjectSettings.Input>()
        var shown by mutableStateOf(identity)
        showing {
            ProjectSettingsDialog(
                identity = shown,
                isLoaded = true,
                onChange = { changes += it; shown = it.appliedTo(shown) },
                onReviewUpgrade = null,
                onDismiss = {},
            )
        }
        onNodeWithText("Breakout").assertExists()
        onNodeWithText("com.example.breakout").assertExists()
        onAllNodes(hasSetTextAction())[0].performTextReplacement("Breakout!")
        onAllNodes(hasSetTextAction())[1].performTextReplacement("1.0.")
        waitForIdle()
        assertEquals(listOf(SetProjectSettings.Input(name = "Breakout!")), changes)
        onNodeWithText("isn't a version name", substring = true).assertExists()
        saveSnapshot("project-settings")
    }

    @Test
    fun aProjectWhoseBuildDoesNotReadItsSettingsOffersTheUpgrade() = uiTest(width = 640, height = 600) {
        var isReviewing = false
        showing {
            ProjectSettingsDialog(
                identity = null,
                isLoaded = true,
                onChange = {},
                onReviewUpgrade = { isReviewing = true },
                onDismiss = {},
            )
        }
        onNodeWithText("Upgrade its template files first", substring = true).assertExists()
        onNodeWithText("Review changes").performClick()
        assertEquals(true, isReviewing)
        saveSnapshot("project-settings-unavailable")
    }

    @Test
    fun aVersionNumberThatIsNotANumberIsInvalid() {
        val change = IdentityField.VERSION_NUMBER.changeOf(identity, "four")
        assertEquals(Res.string.error_identity_version_number_invalid, (change as IdentityFieldChange.Invalid).reason)
        assertEquals(IdentityFieldChange.Valid(SetProjectSettings.Input(versionNumber = 4)), IdentityField.VERSION_NUMBER.changeOf(identity, " 4 "))
        assertEquals("3", IdentityField.VERSION_NUMBER.valueOf(identity))
    }
}
