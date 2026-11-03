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

import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets

/**
 * An entry of Play's menu: a place to play, or the places of a kind that has many (Android's and iOS's devices), in a
 * menu of their own.
 */
internal sealed interface PlayTargetMenuEntry {

    data class Place(val target: PlayTarget) : PlayTargetMenuEntry

    /**
     * @param targets Devices first, then emulators or simulators, each in the order they were found; none when the project
     *   is built for [kind] but nothing to play it on is there yet.
     */
    data class Group(val kind: PlayTarget.Kind, val targets: List<PlayTarget>) : PlayTargetMenuEntry

    companion object {
        private val GROUPED_KINDS = setOf(PlayTarget.Kind.ANDROID, PlayTarget.Kind.IOS)

        /**
         * The entries of [targets], by kind in the order of [PlayTarget.Kind]: a group for every configured kind that has
         * one, its places or none, and the places of the others.
         */
        fun of(targets: PlayTargets): List<PlayTargetMenuEntry> = PlayTarget.Kind.entries.flatMap { kind ->
            val ofKind = targets.available.filter { it.kind == kind }
            when {
                kind in GROUPED_KINDS -> if (kind in targets.configured || ofKind.isNotEmpty()) listOf(Group(kind, ofKind.sortedBy { it.isVirtual })) else emptyList()
                else -> ofKind.map(::Place)
            }
        }
    }
}
