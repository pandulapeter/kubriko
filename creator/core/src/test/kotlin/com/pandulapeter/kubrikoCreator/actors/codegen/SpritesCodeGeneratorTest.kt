/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.codegen

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class SpritesCodeGeneratorTest {
    private val code = SpritesCodeGenerator.generate("com.example.game", sortedSetOf("brick", "ball_1"))

    @Test
    fun itIsAManagerThatDeclaresOneAtlasOfEverySprite() {
        assertTrue(GeneratedFile.isUnchanged(code))
        assertContains(code, "package com.example.game.generated\n")
        assertContains(code, "import com.example.game.resources.Res\nimport com.example.game.resources.allDrawableResources\n")
        assertContains(code, "class GeneratedSprites : Manager() {\n")
        assertContains(code, "        kubriko.get<SpriteManager>().loadAtlas(SPRITES.mapNotNull { Res.allDrawableResources[it] })\n")
    }

    @Test
    fun theSpritesAreLookedUpByName() {
        assertContains(code, "        val SPRITES = listOf(\n            \"ball_1\",\n            \"brick\",\n        )\n    }\n}\n")
    }
}
