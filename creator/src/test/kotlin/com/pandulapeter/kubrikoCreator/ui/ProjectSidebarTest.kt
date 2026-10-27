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

import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectSidebarTest {

    @Test
    fun kindsOfAssetsNothingUsesYetAreOnlyListedWhenTheProjectHasSome() {
        assertEquals(listOf(ResourceGroup.SCENES, ResourceGroup.KOTLIN), shownResourceGroups(emptyMap()))
        val sound = ProjectResource(ResourceGroup.SOUNDS, File("hit.wav"), "hit.wav")
        assertEquals(listOf(ResourceGroup.SCENES, ResourceGroup.SOUNDS, ResourceGroup.KOTLIN), shownResourceGroups(mapOf(ResourceGroup.SOUNDS to listOf(sound))))
    }
}
