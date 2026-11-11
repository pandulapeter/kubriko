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

import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SidebarExpansionTest {
    @Test
    fun theExpansionRemembersChoicesByKeyAndHandsTheTreesTheirOwn() {
        val expansion = SidebarExpansion()
        val groupKey = SidebarKeys.resourceGroup("", ResourceGroup.KOTLIN)
        val outline = SidebarOutline(
            listOf(
                SidebarOutline.Node(groupKey, parent = null, isFolder = false, isExpandedByDefault = false),
                SidebarOutline.Node(SidebarKeys.folder(groupKey, "container:ANDROID"), parent = groupKey, isFolder = true, isExpandedByDefault = false),
                SidebarOutline.Node(SidebarKeys.folder(groupKey, "shared/ui"), parent = groupKey, isFolder = true),
            ),
        )
        assertFalse(expansion.isExpanded(outline, groupKey))
        expansion.toggle(outline, groupKey)
        assertTrue(expansion.isExpanded(outline, groupKey))
        assertEquals(setOf("shared/ui"), expansion.toggledKotlinRows(outline, groupKey), "Opening the group closed the package, while the container already starts closed")
        expansion.setAll(outline, groupKey, isExpanded = true)
        assertEquals(setOf("container:ANDROID"), expansion.toggledKotlinRows(outline, groupKey))
        assertEquals(emptySet(), expansion.collapsedFolders(groupKey))
        expansion.setAll(outline, groupKey, isExpanded = false)
        assertEquals(setOf("container:ANDROID", "shared/ui"), expansion.collapsedFolders(groupKey))
        expansion.expand(SidebarKeys.folder(groupKey, "shared/ui"))
        assertEquals(setOf("container:ANDROID"), expansion.collapsedFolders(groupKey))
    }

    @Test
    fun choicesAboutWhatIsGoneAreForgottenSoASameNamedFolderStartsAfresh() {
        val expansion = SidebarExpansion()
        val bosses = SidebarOutline(listOf(SidebarOutline.Node("scenes", parent = null, isFolder = false, isCollapsible = false), SidebarOutline.Node("scenes|bosses", parent = "scenes", isFolder = true)))
        expansion.toggle(bosses, "scenes|bosses")
        assertFalse(expansion.isExpanded(bosses, "scenes|bosses"))
        expansion.forgetAllBut(SidebarOutline(listOf(SidebarOutline.Node("scenes", parent = null, isFolder = false, isCollapsible = false))))
        assertTrue(expansion.isExpanded(bosses, "scenes|bosses"))
    }

    private val levels = SidebarKeys.folder("scenes", "levels")
    private val deep = SidebarKeys.folder("scenes", "levels/deep")

    private fun outlineOf(vararg folders: String) = SidebarOutline(
        listOf(SidebarOutline.Node("scenes", parent = null, isFolder = false, isCollapsible = false)) +
            folders.map { SidebarOutline.Node(SidebarKeys.folder("scenes", it), parent = "scenes", isFolder = true) },
    )

    @Test
    fun restoredChoicesWaitForWhatTheyAreAboutAndAreKeptUnderThoseMadeSince() {
        val expansion = SidebarExpansion()
        assertNull(expansion.keptChoices)
        val empty = outlineOf()
        expansion.forgetAllBut(empty)
        expansion.toggle(outlineOf("levels"), levels)
        expansion.restore(mapOf(levels to true, deep to false, "module:gone" to false))
        expansion.forgetAllBut(empty)
        assertEquals(mapOf(deep to false, "module:gone" to false), expansion.keptChoices, "The choice made since wins, and goes once its folder does")
        val loaded = outlineOf("levels", "levels/deep")
        expansion.forgetAllBut(loaded)
        assertFalse(expansion.isExpanded(loaded, deep))
        expansion.forgetAllBut(empty)
        assertEquals(mapOf("module:gone" to false), expansion.keptChoices)
    }

    @Test
    fun aMovedFolderTakesWhatIsOpenAndClosedInItAlong() {
        val expansion = SidebarExpansion().apply { restore(emptyMap()) }
        val before = outlineOf("levels", "levels/deep", "levels/deep/pit", "cave")
        expansion.forgetAllBut(before)
        expansion.toggle(before, deep)
        expansion.toggle(before, SidebarKeys.folder("scenes", "cave"))
        expansion.followFolder("scenes", "levels", "cave/levels")
        expansion.forgetAllBut(before)
        val after = outlineOf("cave", "cave/levels", "cave/levels/deep", "cave/levels/deep/pit")
        expansion.forgetAllBut(after)
        assertFalse(expansion.isExpanded(after, SidebarKeys.folder("scenes", "cave/levels/deep")))
        assertTrue(expansion.isExpanded(after, SidebarKeys.folder("scenes", "cave/levels/deep/pit")))
        assertEquals(setOf("cave", "cave/levels/deep"), expansion.collapsedFolders("scenes"))
    }
}
