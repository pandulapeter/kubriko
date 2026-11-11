/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

/**
 * What a command reads or changes, so that the editor can show it while the AI works on it.
 */
public sealed interface CommandTarget {

    /**
     * An Actor type or Manager type.
     *
     * @param eventIndex The Event the command is about, if it is about one.
     * @param blockIndex The Block of that Event the command is about, if it is about one.
     */
    public data class BlockType(
        val name: String,
        val eventIndex: Int? = null,
        val blockIndex: Int? = null,
    ) : CommandTarget

    /**
     * A Scene.
     *
     * @param actorIds The Actors of it the command is about, if it is about some that are still there afterwards.
     */
    public data class Scene(
        val name: String,
        val actorIds: List<String> = emptyList(),
    ) : CommandTarget

    /**
     * A page of the screenplay, by its name (`hero`, `levels/cave`).
     */
    public data class Page(
        val name: String,
    ) : CommandTarget

    /**
     * One of the project's Markdown files that aren't pages, by its path from the project's root (`docs/combat.md`).
     */
    public data class Document(
        val path: String,
    ) : CommandTarget

    public data object VersionControl : CommandTarget

    public data object Game : CommandTarget

    /**
     * The project's own files, like its template files.
     */
    public data object Project : CommandTarget
}
