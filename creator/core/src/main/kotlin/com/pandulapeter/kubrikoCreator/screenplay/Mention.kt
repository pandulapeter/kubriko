/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.DefinitionPaths
import com.pandulapeter.kubrikoCreator.actors.SpriteFiles
import com.pandulapeter.kubrikoCreator.scenes.SceneStore
import java.io.File

/**
 * What a link of a page into the project names, told by the path of its file from the project's root ([path]) alone, so
 * that a mention of something deleted still says what it was.
 */
public sealed interface Mention {
    public val path: String

    /**
     * A block-built Actor type, by its definition file.
     */
    public data class ActorType(
        val name: String,
        override val path: String,
    ) : Mention

    /**
     * A block-built Manager type, by its definition file.
     */
    public data class ManagerType(
        val name: String,
        override val path: String,
    ) : Mention

    public data class Scene(
        val name: String,
        override val path: String,
    ) : Mention

    public data class Page(
        val name: String,
        override val path: String,
    ) : Mention

    /**
     * A Markdown file outside the screenplay.
     */
    public data class Document(
        override val path: String,
    ) : Mention

    /**
     * An image an Actor type can be drawn as.
     */
    public data class Sprite(
        override val path: String,
    ) : Mention

    /**
     * A sound or music file the game can play.
     */
    public data class Audio(
        override val path: String,
    ) : Mention

    public data class Kotlin(
        override val path: String,
    ) : Mention

    /**
     * Any other file of the project.
     */
    public data class OtherFile(
        override val path: String,
    ) : Mention

    public companion object {

        /**
         * What the file at [path], a path from the project's root, is.
         */
        public fun of(path: String): Mention {
            val extension = path.substringAfterLast('.', "").lowercase()
            val composeResource = "/$path".indexOf("/$COMPOSE_RESOURCES/").takeIf { it >= 0 }?.let { path.substring(it + COMPOSE_RESOURCES.length + 1) }
            return when {
                extension == "json" && path.startsWith(DefinitionPaths.folderPath(BlockTypeKind.ACTOR, "") + "/") -> ActorType(nameOf(path), path)
                extension == "json" && path.startsWith(DefinitionPaths.folderPath(BlockTypeKind.MANAGER, "") + "/") -> ManagerType(nameOf(path), path)
                PageNaming.nameOf(path) != null -> Page(checkNotNull(PageNaming.nameOf(path)), path)
                extension == PageNaming.EXTENSION -> Document(path)
                extension == "json" && composeResource?.startsWith("$SCENES/") == true -> Scene(SceneStore.nameOf(File(path)), path)
                composeResource != null && SpriteFiles.accessorOf(composeResource) != null -> Sprite(path)
                composeResource?.startsWith("files/") == true && extension in audioExtensions -> Audio(path)
                extension == "kt" || extension == "kts" -> Kotlin(path)
                else -> OtherFile(path)
            }
        }

        private fun nameOf(path: String) = path.substringAfterLast('/').substringBeforeLast('.')

        private const val COMPOSE_RESOURCES = "composeResources"
        private const val SCENES = "files/scenes"
        private val audioExtensions = setOf("wav", "mp3", "ogg", "m4a", "aac", "flac")
    }
}
