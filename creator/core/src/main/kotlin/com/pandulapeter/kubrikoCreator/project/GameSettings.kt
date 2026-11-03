/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.scenes.SceneNaming
import java.io.File

/**
 * How the game starts and runs, kept as `game.*` keys in `gradle.properties`, which the build file (from template
 * version 10) turns into the generated `GameSettings` object that `Game.kt` creates its Kubriko instance from. A key that
 * is missing means its default, as it does for the build.
 *
 * @param firstScene The Scene the game starts with.
 * @param viewportWidth The width, in scene units, that the [aspectRatio] fits when it uses one.
 * @param viewportHeight The height, in scene units, that the [aspectRatio] fits when it uses one.
 * @param frameRate The updates per second the game is limited to, or [EVERY_DISPLAY_FRAME].
 */
public data class GameSettings(
    val firstScene: String = DEFAULT_FIRST_SCENE,
    val aspectRatio: AspectRatio = AspectRatio.DYNAMIC,
    val viewportWidth: Float = DEFAULT_VIEWPORT_WIDTH,
    val viewportHeight: Float = DEFAULT_VIEWPORT_HEIGHT,
    val frameRate: Int = DEFAULT_FRAME_RATE,
) {

    /**
     * Why these can't be the game's settings, with the arguments of the message, or null if they can. Whether the first
     * Scene exists is left to the caller, who knows the project's Scenes.
     */
    public fun validate(): Pair<GameSettingsProblem, List<String>>? = when {
        !SceneNaming.isValid(firstScene) -> GameSettingsProblem.FIRST_SCENE_INVALID to listOf(firstScene)
        !isValidViewportSize(viewportWidth) || !isValidViewportSize(viewportHeight) -> GameSettingsProblem.VIEWPORT_SIZE_INVALID to listOf(numberText(MAXIMUM_VIEWPORT_SIZE))
        frameRate != EVERY_DISPLAY_FRAME && frameRate !in 1..MAXIMUM_FRAME_RATE -> GameSettingsProblem.FRAME_RATE_INVALID to listOf(MAXIMUM_FRAME_RATE.toString())
        else -> null
    }

    /**
     * [text] (a `gradle.properties`) holding the settings of [keys] as they are here, with every other line as it was.
     */
    public fun writtenTo(text: String, keys: Collection<String> = ALL_KEYS): String = valuesByKey()
        .filterKeys { it in keys }
        .entries
        .fold(text) { result, (key, value) -> GradleProperties.withValue(result, key, value) }

    private fun valuesByKey() = linkedMapOf(
        FIRST_SCENE to firstScene,
        ASPECT_RATIO to aspectRatio.key,
        VIEWPORT_WIDTH to numberText(viewportWidth),
        VIEWPORT_HEIGHT to numberText(viewportHeight),
        FRAME_RATE to if (frameRate == EVERY_DISPLAY_FRAME) DISPLAY_FRAME_RATE_VALUE else frameRate.toString(),
    )

    public companion object {
        public const val FIRST_SCENE: String = "game.firstScene"
        public const val ASPECT_RATIO: String = "game.aspectRatio"
        public const val VIEWPORT_WIDTH: String = "game.viewportWidth"
        public const val VIEWPORT_HEIGHT: String = "game.viewportHeight"
        public const val FRAME_RATE: String = "game.frameRate"
        public val ALL_KEYS: List<String> = listOf(FIRST_SCENE, ASPECT_RATIO, VIEWPORT_WIDTH, VIEWPORT_HEIGHT, FRAME_RATE)
        public const val DEFAULT_FIRST_SCENE: String = "main"
        public const val DEFAULT_VIEWPORT_WIDTH: Float = 1920f
        public const val DEFAULT_VIEWPORT_HEIGHT: Float = 1080f
        public const val DEFAULT_FRAME_RATE: Int = 60

        /**
         * The [frameRate] that updates the game on every frame the display shows, which `gradle.properties` calls
         * [DISPLAY_FRAME_RATE_VALUE].
         */
        public const val EVERY_DISPLAY_FRAME: Int = 0
        public const val DISPLAY_FRAME_RATE_VALUE: String = "display"
        public const val MAXIMUM_FRAME_RATE: Int = 1000
        public const val MAXIMUM_VIEWPORT_SIZE: Float = 100_000f

        private fun isValidViewportSize(value: Float) = value in 1f..MAXIMUM_VIEWPORT_SIZE

        /**
         * [value] as `gradle.properties` holds it, without a fraction when it has none.
         */
        public fun numberText(value: Float): String = if (value == value.toLong().toFloat()) value.toLong().toString() else value.toString()

        /**
         * The settings [text] (a `gradle.properties`) holds, reading a missing key as its default, and the keys whose
         * values the build can't read, as the build file's block parses them.
         */
        public fun of(text: String): GameSettingsReading {
            val invalidKeys = mutableListOf<String>()
            fun <T> value(key: String, default: T, parse: (String) -> T?): T {
                val raw = GradleProperties.valueOf(text, key) ?: return default
                return parse(raw.trim()) ?: default.also { invalidKeys += key }
            }
            val settings = GameSettings(
                firstScene = value(FIRST_SCENE, DEFAULT_FIRST_SCENE) { it.takeIf(SceneNaming::isValid) },
                aspectRatio = value(ASPECT_RATIO, AspectRatio.DYNAMIC, AspectRatio::ofKey),
                viewportWidth = value(VIEWPORT_WIDTH, DEFAULT_VIEWPORT_WIDTH) { it.toFloatOrNull()?.takeIf(::isValidViewportSize) },
                viewportHeight = value(VIEWPORT_HEIGHT, DEFAULT_VIEWPORT_HEIGHT) { it.toFloatOrNull()?.takeIf(::isValidViewportSize) },
                frameRate = value(FRAME_RATE, DEFAULT_FRAME_RATE) { raw ->
                    if (raw == DISPLAY_FRAME_RATE_VALUE) EVERY_DISPLAY_FRAME else raw.toIntOrNull()?.takeIf { it in 1..MAXIMUM_FRAME_RATE }
                },
            )
            return GameSettingsReading(settings, invalidKeys)
        }

        /**
         * The settings of the project in [rootDirectory], or null if its build doesn't read them (its template is older
         * than version 10, or it was changed by hand).
         */
        public fun read(rootDirectory: File): GameSettingsReading? {
            if (!isReadByBuild(rootDirectory)) return null
            return of(File(rootDirectory, GameIdentity.GRADLE_PROPERTIES_FILE).takeIf { it.isFile }?.readText().orEmpty())
        }

        /**
         * Writes the settings of [keys] as [settings] has them.
         */
        @EditorMutation
        public fun write(rootDirectory: File, settings: GameSettings, keys: Collection<String> = ALL_KEYS) {
            val file = File(rootDirectory, GameIdentity.GRADLE_PROPERTIES_FILE)
            val raw = if (file.isFile) file.readText() else ""
            file.writeTextAtomically(settings.writtenTo(raw.withUnixLineBreaks(), keys).withLineBreaksOf(raw))
        }

        internal fun isReadByBuild(rootDirectory: File) = File(rootDirectory, GameIdentity.BUILD_FILE).takeIf { it.isFile }?.readText()?.let { FIRST_SCENE in it } == true
    }
}

/**
 * The game's [settings] as `gradle.properties` holds them, and the keys of those it holds that the build can't read
 * ([invalidKeys]), which [settings] has at their defaults.
 */
public data class GameSettingsReading(
    val settings: GameSettings,
    val invalidKeys: List<String> = emptyList(),
)
