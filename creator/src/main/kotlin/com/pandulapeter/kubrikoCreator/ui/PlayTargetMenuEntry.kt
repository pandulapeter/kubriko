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

/**
 * An entry of Play's menu: a place to play, or the places of a kind that has many (Android's and iOS's devices), in a
 * menu of their own.
 */
internal sealed interface PlayTargetMenuEntry {

    data class Place(val target: PlayTarget) : PlayTargetMenuEntry

    /**
     * @param targets Devices first, then emulators or simulators, each in the order they were found.
     */
    data class Group(val kind: PlayTarget.Kind, val targets: List<PlayTarget>) : PlayTargetMenuEntry

    companion object {
        private val GROUPED_KINDS = setOf(PlayTarget.Kind.ANDROID, PlayTarget.Kind.IOS)

        /**
         * The entries of [available], by kind in the order of [PlayTarget.Kind], leaving out the kinds with no places.
         */
        fun of(available: List<PlayTarget>): List<PlayTargetMenuEntry> = PlayTarget.Kind.entries.flatMap { kind ->
            val ofKind = available.filter { it.kind == kind }
            when {
                ofKind.isEmpty() -> emptyList()
                kind in GROUPED_KINDS -> listOf(Group(kind, ofKind.sortedBy { it.isVirtual }))
                else -> ofKind.map(::Place)
            }
        }
    }
}
