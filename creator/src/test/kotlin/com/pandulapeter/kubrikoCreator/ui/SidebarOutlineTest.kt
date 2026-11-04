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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SidebarOutlineTest {
    private val outline = SidebarOutline(
        listOf(
            SidebarOutline.Node("module", parent = null, isFolder = false),
            SidebarOutline.Node("types", parent = "module", isFolder = false, isCollapsible = false),
            SidebarOutline.Node("enemies", parent = "types", isFolder = true),
            SidebarOutline.Node("enemies/flying", parent = "enemies", isFolder = true),
            SidebarOutline.Node("enemies/flying/bats", parent = "enemies/flying", isFolder = true),
            SidebarOutline.Node("kotlin", parent = "module", isFolder = false, isExpandedByDefault = false),
            SidebarOutline.Node("android", parent = "kotlin", isFolder = true, isExpandedByDefault = false),
            SidebarOutline.Node("sprites", parent = "module", isFolder = false),
        ),
    )

    @Test
    fun closingClosesOnlyItWhileOpeningClosesEveryFolderInItAtAnyDepth() {
        assertEquals(mapOf("enemies" to false), outline.toggled("enemies", isExpanded = true))
        assertEquals(mapOf("enemies" to true, "enemies/flying" to false, "enemies/flying/bats" to false), outline.toggled("enemies", isExpanded = false))
        assertEquals(
            mapOf("module" to true, "enemies" to false, "enemies/flying" to false, "enemies/flying/bats" to false, "android" to false),
            outline.toggled("module", isExpanded = false),
            "A module opens with its groups as they were",
        )
    }

    @Test
    fun everythingInOneIsOpenedOrClosedWithItButAGroupWhoseHeaderAlwaysShows() {
        assertEquals(mapOf("enemies/flying" to true, "enemies/flying/bats" to true), outline.allSet("enemies/flying", isExpanded = true))
        assertEquals(mapOf("enemies" to false, "enemies/flying" to false, "enemies/flying/bats" to false), outline.allSet("types", isExpanded = false))
        assertEquals(setOf("module", "enemies", "enemies/flying", "enemies/flying/bats", "kotlin", "android", "sprites"), outline.allSet("module", isExpanded = false).keys)
    }

    @Test
    fun onlyWhatHoldsSomethingThatOpensOffersToOpenOrCloseEverything() {
        listOf("module", "types", "enemies", "enemies/flying", "kotlin").forEach { assertTrue(outline.hasSubfolders(it), it) }
        listOf("enemies/flying/bats", "android", "sprites", "unknown").forEach { assertFalse(outline.hasSubfolders(it), it) }
        assertFalse(outline.isExpandedByDefault("kotlin"))
        assertTrue(outline.isExpandedByDefault("unknown"))
    }
}
