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

/**
 * What a file Kubriko Creator adds, renames, and deletes for the game is: a sprite, a sound, or music, each named by its
 * path from `composeResources` (`drawable/ball.png`, `files/sounds/hit.wav`), which is how the game and its types name it.
 *
 * @param extensions The extensions its files can have, lowercase.
 */
public enum class ResourceKind(
    public val extensions: Set<String>,
) {
    /**
     * An image right in the `drawable` folder, whose name makes a resource accessor ([SpriteFiles.accessorOf]).
     */
    SPRITE(setOf("png", "jpg", "jpeg", "webp")),

    /**
     * A sound effect anywhere in the `files` folder but its Scenes.
     */
    SOUND(setOf("wav")),

    /**
     * Music anywhere in the `files` folder but its Scenes.
     */
    MUSIC(setOf("mp3", "ogg")),
    ;

    public companion object {

        /**
         * What a file at [path] (from `composeResources`) is, told by its folder and extension alone, so that a path
         * whose name breaks the naming rules ([validate]) still has a kind; or null if Kubriko Creator keeps no such file.
         */
        public fun of(path: String): ResourceKind? {
            val extension = path.substringAfterLast('.', "").lowercase()
            val segments = path.split('/')
            return when {
                segments.size == 2 && segments[0] == DRAWABLE -> SPRITE.takeIf { extension in it.extensions }
                segments.size < 2 || segments[0] != FILES || segments[1] == SCENES -> null
                extension in SOUND.extensions -> SOUND
                extension in MUSIC.extensions -> MUSIC
                else -> null
            }
        }

        /**
         * Why [path] (from `composeResources`, of a file [of] knows) can't be a file's: a name, or a folder in it, that
         * doesn't start with a letter or a number, or that has anything but letters, numbers, dashes, and underscores
         * before its extension. A sprite's name always makes a resource accessor this way.
         */
        public fun validate(path: String): NameProblem? = validateParts(path.substringAfter('/').substringBeforeLast('.').split('/'))

        /**
         * Why [folder] (an audio folder's path from `composeResources/files`, [audioFolderOf]) can't be a folder's, by the
         * rules [validate] holds a file's folders to.
         */
        public fun validateAudioFolder(folder: String): NameProblem? = validateParts(folder.split('/'))

        private fun validateParts(parts: List<String>): NameProblem? = parts.firstNotNullOfOrNull { part ->
            when (NameFlaw.of(part, NameFlaw::isLetterOrDigitAToZ) { NameFlaw.isLetterOrDigitAToZ(it) || it in "_-" }) {
                NameFlaw.EMPTY -> if (parts.size > 1) NameProblem.PATH_PART_EMPTY else NameProblem.NAME_EMPTY
                NameFlaw.LETTER_BEYOND_A_TO_Z -> NameProblem.NAME_LETTER_BEYOND_A_TO_Z
                NameFlaw.FIRST_CHARACTER -> NameProblem.RESOURCE_FIRST_CHARACTER
                NameFlaw.CHARACTER -> NameProblem.RESOURCE_CHARACTERS
                null -> null
            }
        }

        /**
         * The path [path]'s file would have as [name] (without folders or extension), in the same folder, keeping its
         * extension.
         */
        public fun renamed(path: String, name: String): String = "${path.substringBeforeLast('/')}/$name.${path.substringAfterLast('.')}"

        /**
         * The name of [path]'s file, without folders or extension, as the sidebar shows it.
         */
        public fun nameOf(path: String): String = path.substringAfterLast('/').substringBeforeLast('.')

        /**
         * The path from `composeResources` a new file of [kind] called [name] (without extension) gets, with [extension]:
         * a sprite right in `drawable`, a sound in `files/sounds`, and music in `files/music`, as the templates keep them.
         */
        public fun pathOf(kind: ResourceKind, name: String, extension: String): String = when (kind) {
            SPRITE -> "$DRAWABLE/$name.$extension"
            SOUND, MUSIC -> "$FILES/${checkNotNull(defaultAudioFolderOf(kind))}/$name.$extension"
        }

        /**
         * The folder new files of [kind] go to, from `composeResources/files` (`sounds`), or null for sprites, which
         * have no folders there.
         */
        public fun defaultAudioFolderOf(kind: ResourceKind): String? = when (kind) {
            SPRITE -> null
            SOUND -> "sounds"
            MUSIC -> "music"
        }

        /**
         * The folder the audio file at [path] (from `composeResources`) is in, from `composeResources/files`, as audio
         * folders are named: `sounds/enemies` for `files/sounds/enemies/hit.wav`, empty for one right in `files`.
         */
        public fun audioFolderOf(path: String): String = path.removePrefix("$FILES/").substringBeforeLast('/', "")

        /**
         * The path from `composeResources` the audio file at [path] gets in the audio [folder] ([audioFolderOf]),
         * keeping its name.
         */
        public fun inAudioFolder(path: String, folder: String): String = listOf(FILES, folder, path.substringAfterLast('/')).filter { it.isNotEmpty() }.joinToString("/")

        /**
         * Whether the audio folder at [folder] ([audioFolderOf]) is the Scenes folder or one in it, which hold no audio,
         * in any capitals, since a Mac or Windows takes `Scenes` for the same folder.
         */
        public fun isScenesFolder(folder: String): Boolean = folder.substringBefore('/').equals(SCENES, ignoreCase = true)

        /**
         * The kind a file with [extension] would be added as, or null if it can't be one.
         */
        public fun ofExtension(extension: String): ResourceKind? = entries.firstOrNull { extension.lowercase() in it.extensions }

        /**
         * Whether [bytes] hold what a file with [extension] holds, told by the signature such files start with, so that
         * a file named for another format isn't added for the game to fail on.
         */
        public fun isContentOf(bytes: ByteArray, extension: String): Boolean {
            fun startsWith(offset: Int, vararg expected: Int) = bytes.size >= offset + expected.size && expected.indices.all { bytes[offset + it].toInt() and 0xFF == expected[it] }
            fun startsWith(offset: Int, text: String) = startsWith(offset, *text.map { it.code }.toIntArray())
            return when (extension.lowercase()) {
                "png" -> startsWith(0, 0x89, 'P'.code, 'N'.code, 'G'.code)
                "jpg", "jpeg" -> startsWith(0, 0xFF, 0xD8, 0xFF)
                "webp" -> startsWith(0, "RIFF") && startsWith(8, "WEBP")
                "wav" -> startsWith(0, "RIFF") && startsWith(8, "WAVE")
                "mp3" -> startsWith(0, "ID3") || (bytes.size >= 2 && bytes[0].toInt() and 0xFF == 0xFF && bytes[1].toInt() and 0xE0 == 0xE0)
                "ogg" -> startsWith(0, "OggS")
                else -> false
            }
        }

        /**
         * The name of the folder of every `composeResources` path.
         */
        public const val COMPOSE_RESOURCES: String = "composeResources"
        private const val DRAWABLE = "drawable"
        private const val FILES = "files"
        private const val SCENES = "scenes"
    }
}
