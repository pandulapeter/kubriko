/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.project.ResourceGroup

/**
 * The keys of what the sidebar's [SidebarOutline] holds, built from the module and group each is in, so that two modules'
 * folders of the same path, or the Kotlin files' packages and a group's folders, are never confused.
 */
internal object SidebarKeys {
    fun module(module: String) = "module:$module"

    fun folderGroup(module: String, group: FolderGroup) = "$module|folders:${group.name}"

    fun resourceGroup(module: String, group: ResourceGroup) = "$module|files:${group.name}"

    /**
     * Where the documents of [module] are listed, whose folder rows are [folder]s of it.
     */
    fun documents(module: String) = "$module|documents"

    /**
     * A folder of the group [groupKey]: its path, or the Kotlin tree's own key for its rows ([KotlinTreeRow.key]).
     */
    fun folder(groupKey: String, path: String) = "$groupKey|$path"
}
