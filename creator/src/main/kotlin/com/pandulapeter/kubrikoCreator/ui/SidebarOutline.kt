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

/**
 * Everything of the sidebar that holds something else (modules, groups, folders, Kotlin containers and packages), each
 * under the one holding it, as if every one of them were open: what opening and closing them changes, whatever is shown.
 * Each is told apart by a key of its own ([SidebarKeys]).
 */
internal class SidebarOutline(nodes: List<Node>) {

    /**
     * @param parent The key of the one holding it, or null at the top.
     * @param isFolder Whether it is a folder, a Kotlin container, or a package, rather than a module or a group.
     * @param isCollapsible Whether it can be closed (every one but the groups of types and Scenes, whose headers stay).
     */
    data class Node(
        val key: String,
        val parent: String?,
        val isFolder: Boolean,
        val isCollapsible: Boolean = true,
        val isExpandedByDefault: Boolean = true,
    )

    private val nodes = nodes.associateBy { it.key }
    private val children = nodes.groupBy { it.parent }

    operator fun contains(key: String) = key in nodes

    fun isExpandedByDefault(key: String) = nodes[key]?.isExpandedByDefault ?: true

    /**
     * Whether [key] holds anything else that can be opened and closed, which is when opening or closing everything in it
     * is offered.
     */
    fun hasSubfolders(key: String): Boolean = children[key].orEmpty().any { it.isCollapsible || hasSubfolders(it.key) }

    /**
     * What changes when [key] is opened or closed: closing it closes only it, while opening it opens it with every folder
     * in it closed, at any depth, so a deep tree opens one level at a time.
     */
    fun toggled(key: String, isExpanded: Boolean): Map<String, Boolean> = if (isExpanded) {
        mapOf(key to false)
    } else {
        descendantsOf(key).filter { it.isFolder }.associate { it.key to false } + (key to true)
    }

    /**
     * What changes when everything in [key] is opened ([isExpanded]) or closed, [key] itself too when it can be.
     */
    fun allSet(key: String, isExpanded: Boolean): Map<String, Boolean> = (listOfNotNull(nodes[key]) + descendantsOf(key))
        .filter { it.isCollapsible }
        .associate { it.key to isExpanded }

    private fun descendantsOf(key: String): List<Node> = children[key].orEmpty().flatMap { listOf(it) + descendantsOf(it.key) }
}
