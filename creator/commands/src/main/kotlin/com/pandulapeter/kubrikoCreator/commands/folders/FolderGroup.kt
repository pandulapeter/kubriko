/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.folders

import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A group of the sidebar that has folders, in the order the sidebar lists them.
 *
 * @param kind The kind of type its folders hold, or null for the other groups.
 * @param resourceKind The kind of file its folders hold, for sprites, sounds, and music: a sound's or music's folders are
 *   the real ones of `composeResources/files` ([com.pandulapeter.kubrikoCreator.actors.AudioFolders]), named by their
 *   path from it ([isAudio]), and a sprite's are virtual ([com.pandulapeter.kubrikoCreator.sprites.SpriteFolderStore]).
 */
@Serializable
public enum class FolderGroup(
    public val kind: BlockTypeKind?,
    public val resourceKind: ResourceKind? = null,
) {
    @SerialName("pages")
    PAGES(null),

    @SerialName("actorTypes")
    ACTOR_TYPES(BlockTypeKind.ACTOR),

    @SerialName("managerTypes")
    MANAGER_TYPES(BlockTypeKind.MANAGER),

    @SerialName("scenes")
    SCENES(null),

    @SerialName("sprites")
    SPRITES(null, ResourceKind.SPRITE),

    @SerialName("sounds")
    SOUNDS(null, ResourceKind.SOUND),

    @SerialName("music")
    MUSIC(null, ResourceKind.MUSIC),

    ;

    /**
     * Whether its folders are the real folders of `composeResources/files`, which hold sounds and music.
     */
    public val isAudio: Boolean get() = resourceKind == ResourceKind.SOUND || resourceKind == ResourceKind.MUSIC

    public companion object {
        internal fun of(kind: BlockTypeKind) = entries.first { it.kind == kind }

        /**
         * The group whose folders hold files of [kind].
         */
        public fun of(kind: ResourceKind): FolderGroup = entries.first { it.resourceKind == kind }
    }
}
