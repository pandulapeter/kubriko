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

/**
 * How the screenplay's pages are named: by their path in `screenplay/` without `.md`, like Scenes (`hero`,
 * `levels/cave`), each part starting with a letter or a digit, then letters, digits, spaces, dashes, and underscores.
 */
public object PageNaming {

    /**
     * The folder at the project's root holding the pages.
     */
    public const val FOLDER: String = "screenplay"

    /**
     * The first page, "The game", named so that the folder reads well on GitHub and in IntelliJ.
     */
    public const val FIRST_PAGE: String = "README"

    public const val EXTENSION: String = "md"

    private const val MAXIMUM_LENGTH = 120
    private val part = Regex("[A-Za-z0-9][A-Za-z0-9 _-]*")

    public fun isValid(name: String): Boolean = name.length <= MAXIMUM_LENGTH && name.split('/').all { part.matches(it) && !it.endsWith(' ') }

    /**
     * The folder [name] is in, or an empty string for one at the top.
     */
    public fun folderOf(name: String): String = name.substringBeforeLast('/', missingDelimiterValue = "")
}
