/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(EditorMutation::class)

package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameSettingsTest {
    private val root = createTempDirectory().toFile()
    private val settings = GameSettings("levels/1-1", AspectRatio.FIT_VERTICAL, 1280f, 720.5f, GameSettings.EVERY_DISPLAY_FRAME)

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    @Test
    fun whatTheBuildCantUseIsRejected() {
        assertNull(settings.validate())
        assertNull(GameSettings().validate())
        assertNull(settings.copy(viewportWidth = 1f, viewportHeight = 100_000f, frameRate = 1000).validate())
        listOf("", "levels/", "my scene", "-main").forEach { scene ->
            assertEquals(GameSettingsProblem.FIRST_SCENE_INVALID to listOf(scene), settings.copy(firstScene = scene).validate(), scene)
        }
        listOf(0.5f, 0f, -1f, 100_001f, Float.NaN, Float.POSITIVE_INFINITY).forEach { size ->
            assertEquals(GameSettingsProblem.VIEWPORT_SIZE_INVALID to listOf("100000"), settings.copy(viewportWidth = size).validate(), "$size")
            assertEquals(GameSettingsProblem.VIEWPORT_SIZE_INVALID, settings.copy(viewportHeight = size).validate()?.first, "$size")
        }
        listOf(-1, 1001).forEach { frameRate ->
            assertEquals(GameSettingsProblem.FRAME_RATE_INVALID to listOf("1000"), settings.copy(frameRate = frameRate).validate(), "$frameRate")
        }
    }

    @Test
    fun writingChangesOnlyTheNamedKeysLines() {
        val before = "kotlin.code.style=official\ngame.firstScene=main\n# a comment\ngame.frameRate=30\n"
        assertEquals(
            "kotlin.code.style=official\ngame.firstScene=levels/1-1\n# a comment\ngame.frameRate=display\n" +
                "game.aspectRatio=fitVertical\ngame.viewportWidth=1280\ngame.viewportHeight=720.5\n",
            settings.writtenTo(before),
        )
        assertEquals(
            "kotlin.code.style=official\ngame.firstScene=main\n# a comment\ngame.frameRate=display\n",
            settings.writtenTo(before, listOf(GameSettings.FRAME_RATE)),
        )
        assertEquals(GameSettingsReading(settings), GameSettings.of(settings.writtenTo(before)))
    }

    @Test
    fun missingKeysAreTheirDefaultsAndUnreadableOnesAreNamed() {
        assertEquals(GameSettingsReading(GameSettings()), GameSettings.of(""))
        assertEquals(GameSettingsReading(GameSettings(frameRate = 144, viewportWidth = 800f)), GameSettings.of("game.frameRate= 144 \ngame.viewportWidth=8e2\n"))
        assertEquals(
            GameSettingsReading(GameSettings(), listOf(GameSettings.FIRST_SCENE, GameSettings.ASPECT_RATIO, GameSettings.FRAME_RATE)),
            GameSettings.of("game.firstScene=my scene\ngame.aspectRatio=Dynamic\ngame.viewportWidth=NaN\ngame.viewportHeight=0\ngame.frameRate=60.0\n"),
        )
        assertEquals(
            GameSettingsReading(GameSettings(aspectRatio = AspectRatio.FIXED), listOf(GameSettings.VIEWPORT_WIDTH, GameSettings.VIEWPORT_HEIGHT)),
            GameSettings.of("game.aspectRatio=fixed\ngame.viewportWidth=NaN\ngame.viewportHeight=0\n"),
        )
        assertEquals(
            GameSettingsReading(GameSettings(aspectRatio = AspectRatio.FIT_VERTICAL)),
            GameSettings.of("game.aspectRatio=fitVertical\ngame.viewportWidth=wide\n"),
            "the width a mode doesn't use, which the build doesn't read either",
        )
    }

    @Test
    fun settingsAreReadOnlyWhereTheBuildReadsThem() {
        GameSettings.write(root, settings)
        assertNull(GameSettings.read(root), "no build file")
        File(root, "build.gradle.kts").writeText("val gameName = providers.gradleProperty(\"game.name\")\n")
        assertNull(GameSettings.read(root), "a build of template version 9")
        File(root, "build.gradle.kts").writeText("val gameFirstScene = providers.gradleProperty(\"game.firstScene\")\n")
        assertEquals(GameSettingsReading(settings), GameSettings.read(root))
        File(root, "gradle.properties").delete()
        assertEquals(GameSettingsReading(GameSettings()), GameSettings.read(root), "no gradle.properties")
    }

    @Test
    fun aPropertiesFileWithWindowsLineBreaksKeepsThem() {
        val properties = File(root, "gradle.properties").apply { writeText("kotlin.code.style=official\r\ngame.firstScene=main\r\n") }
        GameSettings.write(root, settings, listOf(GameSettings.FIRST_SCENE))
        assertEquals("kotlin.code.style=official\r\ngame.firstScene=levels/1-1\r\n", properties.readText())
    }

    @Test
    fun aspectRatiosAreNamedAsKubrikosModes() {
        assertEquals(listOf("dynamic", "fitHorizontal", "fitVertical", "fixed", "stretched"), AspectRatio.entries.map { it.key })
        assertEquals(AspectRatio.FIXED, AspectRatio.ofKey("fixed"))
        assertNull(AspectRatio.ofKey("Fixed"))
    }
}
