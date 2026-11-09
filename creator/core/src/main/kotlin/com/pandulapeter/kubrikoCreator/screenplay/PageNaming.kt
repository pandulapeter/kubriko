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

import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.actors.NameProblem

/**
 * How the screenplay's pages are named: by their path in `screenplay/` without `.md`, like Scenes (`hero`,
 * `levels/cave`), each part named as a folder is ([FolderPaths]), and not longer than a path stays readable.
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

    public const val MAXIMUM_LENGTH: Int = 120

    /**
     * The path of the page [name] from the project's root.
     */
    public fun pathOf(name: String): String = "$FOLDER/$name.$EXTENSION"

    /**
     * The name of the page at [path], a path from the project's root, or null if no page is there.
     */
    public fun nameOf(path: String): String? = path.removePrefix("$FOLDER/").takeIf { it != path && it.endsWith(".$EXTENSION") }?.removeSuffix(".$EXTENSION")

    public fun isValid(name: String): Boolean = name.length <= MAXIMUM_LENGTH && validate(name) == null

    /**
     * Why the parts of [name] can't make a page's name, or null if they can; how long it may be is [MAXIMUM_LENGTH].
     */
    public fun validate(name: String): NameProblem? = FolderPaths.validatePath(name)

    /**
     * The folder [name] is in, or an empty string for one at the top.
     */
    public fun folderOf(name: String): String = name.substringBeforeLast('/', missingDelimiterValue = "")
}
