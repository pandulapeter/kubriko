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

/**
 * A Kubriko library a project can depend on, which Kubriko Creator adds to the project's build when its code uses it and
 * removes when nothing does any more ([KubrikoBuildFiles]).
 *
 * @param artifactId The artifact's name on Maven Central.
 * @param catalogName What follows `kubriko-` in the version catalog's alias, and `libs.kubriko.` in the build file.
 * @param packageSegment The package that follows `com.pandulapeter.kubriko.` in the library's code, or null for the
 *   engine, which owns every package no other library does.
 */
internal enum class KubrikoArtifact(
    val artifactId: String,
    val catalogName: String,
    val packageSegment: String?,
) {
    ENGINE("engine", "engine", null),
    AUDIO_PLAYBACK("plugin-audio-playback", "audioPlayback", "audioPlayback"),
    COLLISION("plugin-collision", "collision", "collision"),
    GAMEPAD_INPUT("plugin-gamepad-input", "gamepadInput", "gamepadInput"),
    KEYBOARD_INPUT("plugin-keyboard-input", "keyboardInput", "keyboardInput"),
    PARTICLES("plugin-particles", "particles", "particles"),
    PERSISTENCE("plugin-persistence", "persistence", "persistence"),
    PHYSICS("plugin-physics", "physics", "physics"),
    POINTER_INPUT("plugin-pointer-input", "pointerInput", "pointerInput"),
    SERIALIZATION("plugin-serialization", "serialization", "serialization"),
    SHADERS("plugin-shaders", "shaders", "shaders"),
    SPRITES("plugin-sprites", "sprites", "sprites"),
    CREATOR_API("tool-creator-api", "creatorApi", "creator"),
    SCENE_EDITOR_API("tool-scene-editor-api", "sceneEditorApi", "sceneEditor"),
    ;

    companion object {
        const val GROUP = "io.github.pandulapeter.kubriko"
        private val packagePattern = Regex("""\bcom\.pandulapeter\.kubriko\.([A-Za-z_][A-Za-z0-9_]*)""")

        fun ofArtifactId(artifactId: String) = entries.firstOrNull { it.artifactId == artifactId }

        /**
         * The libraries the Kotlin [sources] use, through imports or fully qualified names. The engine and the entry
         * point Kubriko Creator relies on are always among them.
         */
        fun usedBy(sources: Sequence<String>): Set<KubrikoArtifact> {
            val used = mutableSetOf(ENGINE, CREATOR_API)
            sources.forEach { source ->
                packagePattern.findAll(source).forEach { match ->
                    entries.firstOrNull { it.packageSegment == match.groupValues[1] }?.let(used::add)
                }
            }
            return used
        }
    }
}
