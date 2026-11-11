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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.pandulapeter.kubrikoCreator.actors.FolderPaths
import com.pandulapeter.kubrikoCreator.commands.folders.FolderGroup
import com.pandulapeter.kubrikoCreator.workspace.SidebarFolderExpansion
import kotlinx.coroutines.flow.filterNotNull

/**
 * Which of the sidebar's modules, groups, and folders the user opened or closed, by their [SidebarKeys]; the rest are as
 * the [SidebarOutline] starts them. Kept by key rather than by row, so a folder keeps its state while the rows around it
 * come and go, until it is gone itself ([forgetAllBut]), so one made later under the same name starts afresh.
 *
 * The choices of earlier sessions ([restore]), and those a moved folder takes along ([followFolder]), are about what
 * the sidebar may not show yet (the types are read after the files), so until the whole project was read once, each is
 * forgotten only once it was shown and is gone again.
 */
@Stable
internal class SidebarExpansion {
    private var choices by mutableStateOf(emptyMap<String, Boolean>())
    private var isRestored by mutableStateOf(false)

    /**
     * The keys of the choices about what the sidebar showed since they were made.
     */
    private val shown = HashSet<String>()

    /**
     * Every choice, to keep for the next session, or null until the earlier one's are [restore]d.
     */
    val keptChoices: Map<String, Boolean>? get() = choices.takeIf { isRestored }

    /**
     * Takes the choices of an earlier session, under those made since.
     */
    fun restore(saved: Map<String, Boolean>) {
        if (isRestored) return
        choices = saved + choices
        isRestored = true
    }

    fun isExpanded(outline: SidebarOutline, key: String) = choices[key] ?: outline.isExpandedByDefault(key)

    fun toggle(outline: SidebarOutline, key: String) = choose(outline.toggled(key, isExpanded(outline, key)))

    fun setAll(outline: SidebarOutline, key: String, isExpanded: Boolean) = choose(outline.allSet(key, isExpanded))

    /**
     * Opens [key] alone, leaving what is in it as it was, for showing a name being typed inside it.
     */
    fun expand(key: String) = choose(mapOf(key to true))

    /**
     * Makes the folder at [newPath] of the group [groupKey], and those in it, open or closed as those at [path] are, for a
     * folder moved or renamed; the choices about [path] are forgotten with it once it is gone ([forgetAllBut]).
     */
    fun followFolder(groupKey: String, path: String, newPath: String) {
        val prefix = SidebarKeys.folder(groupKey, "")
        val followed = choices.mapNotNull { (key, isExpanded) ->
            key.removePrefix(prefix).takeIf { key.startsWith(prefix) && FolderPaths.isWithin(it, path) }?.let { folder ->
                SidebarKeys.folder(groupKey, newPath + folder.removePrefix(path)) to isExpanded
            }
        }
        if (followed.isNotEmpty()) choose(followed.toMap(), isShown = false)
    }

    /**
     * The paths of the folders of the group [groupKey] that were closed, which start open ([folderTreeRows]'s `collapsed`).
     */
    fun collapsedFolders(groupKey: String): Set<String> = foldersOf(groupKey) { _, isExpanded -> !isExpanded }

    /**
     * The keys of the Kotlin tree rows of the group [groupKey] that aren't as they start ([kotlinTreeRows]'s `toggled`).
     */
    fun toggledKotlinRows(outline: SidebarOutline, groupKey: String): Set<String> = foldersOf(groupKey) { key, isExpanded ->
        isExpanded != outline.isExpandedByDefault(key)
    }

    private inline fun foldersOf(groupKey: String, isIncluded: (key: String, isExpanded: Boolean) -> Boolean): Set<String> {
        val prefix = SidebarKeys.folder(groupKey, "")
        return choices.entries.mapNotNullTo(HashSet()) { (key, isExpanded) -> key.removePrefix(prefix).takeIf { key.startsWith(prefix) && isIncluded(key, isExpanded) } }
    }

    /**
     * Forgets the choices about what is no longer in [outline]: every one once [isProjectRead], and before that only
     * those about what an outline held since they were made.
     */
    fun forgetAllBut(outline: SidebarOutline, isProjectRead: Boolean) {
        choices.keys.filterTo(shown) { it in outline }
        val gone = choices.keys.filter { it !in outline && (isProjectRead || it in shown) }
        if (gone.isEmpty()) return
        shown.removeAll(gone.toSet())
        choices = choices - gone.toSet()
    }

    private fun choose(changes: Map<String, Boolean>, isShown: Boolean = true) {
        if (isShown) shown += changes.keys
        choices = choices + changes
    }
}

/**
 * The sidebar's [SidebarExpansion] over [outline], restored from [memory] once it is read and kept there as it changes
 * (with no [memory], as in a preview, it starts afresh and is kept nowhere), whose open folders follow every folder a
 * command moves, in each of the trees [treesOf] names for the folder's group.
 */
@Composable
internal fun rememberSidebarExpansion(
    memory: SidebarFolderExpansion?,
    outline: SidebarOutline,
    treesOf: (FolderGroup) -> List<String>,
): SidebarExpansion {
    val expansion = remember(memory) { SidebarExpansion() }
    val saved = memory?.saved?.collectAsState()?.value ?: if (memory == null) emptyMap() else null
    val isProjectRead = memory?.isProjectRead?.collectAsState()?.value ?: true
    LaunchedEffect(expansion, saved) { saved?.let(expansion::restore) }
    LaunchedEffect(expansion, memory) {
        if (memory != null) snapshotFlow { expansion.keptChoices }.filterNotNull().collect(memory::save)
    }
    val isRestored = expansion.keptChoices != null
    LaunchedEffect(expansion, outline, isProjectRead, isRestored) { expansion.forgetAllBut(outline, isProjectRead) }
    val currentTreesOf by rememberUpdatedState(treesOf)
    DisposableEffect(expansion, memory) {
        val listening = memory?.folderMoves?.listen { move -> currentTreesOf(move.group).forEach { expansion.followFolder(it, move.path, move.newPath) } }
        onDispose { listening?.close() }
    }
    return expansion
}
