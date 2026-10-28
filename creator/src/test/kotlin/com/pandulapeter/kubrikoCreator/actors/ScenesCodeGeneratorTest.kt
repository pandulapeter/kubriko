/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class ScenesCodeGeneratorTest {
    private val code = ScenesCodeGenerator.generate("com.example.game")

    @Test
    fun theManagerReadsScenesFromTheProjectsOwnResources() {
        assertTrue(GeneratedFile.isUnchanged(code))
        assertContains(code, "package com.example.game.generated\n")
        assertContains(code, "import com.example.game.resources.Res\n")
        assertContains(code, "class GeneratedScenes(\n")
        assertContains(code, "Res.readBytes(\"files/scenes/\$scene.json\")")
        assertContains(code, "source = \"GeneratedScenes\"")
    }

    @Test
    fun onlyLaterScenesRemoveTheActorsBeforeThem() {
        assertContains(code, "if (!isFirst) actorManager.removeAll()\n            actorManager.add(actors)\n")
        assertContains(code, "if (!isInSceneEditor && firstScene != null) load(firstScene, isFirst = true)")
        assertContains(code, "if (!isInSceneEditor) load(scene, isFirst = false)")
    }
}
