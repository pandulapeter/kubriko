/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import kotlin.math.abs

/**
 * The lines of a file before and after a change, as a diff shows them.
 */
public object LineDiff {
    private const val MAXIMUM_LINES = 2_000

    /**
     * Whether a line was kept, removed, or added.
     *
     * @param prefix What a diff shows the line after.
     */
    public enum class Kind(
        public val prefix: String,
    ) {
        SAME("  "),
        REMOVED("- "),
        ADDED("+ "),
    }

    /**
     * One line of a diff.
     */
    public data class Line(
        public val kind: Kind,
        public val text: String,
    )

    /**
     * Whether [before] and [after] are small enough to compare and show at once: comparing takes memory in proportion to the
     * product of their line counts, and showing every line of a large file holds up the window. A change a user or an
     * agent makes can be any size, unlike the template files'.
     */
    public fun isSmallEnough(before: String, after: String): Boolean =
        before.lineSequence().count() + after.lineSequence().count() <= MAXIMUM_LINES

    /**
     * Every line of [before] and [after], each one marked as kept, removed, or added (a longest common subsequence, which
     * is plenty for the small template files it shows).
     */
    public fun of(before: String, after: String): List<Line> {
        val old = before.lines()
        val new = after.lines()
        val common = Array(old.size + 1) { IntArray(new.size + 1) }
        for (i in old.indices.reversed()) {
            for (j in new.indices.reversed()) {
                common[i][j] = if (old[i] == new[j]) common[i + 1][j + 1] + 1 else maxOf(common[i + 1][j], common[i][j + 1])
            }
        }
        val lines = mutableListOf<Line>()
        var i = 0
        var j = 0
        while (i < old.size || j < new.size) {
            when {
                i < old.size && j < new.size && old[i] == new[j] -> lines += Line(Kind.SAME, old[i++]).also { j++ }
                i < old.size && (j == new.size || common[i + 1][j] >= common[i][j + 1]) -> lines += Line(Kind.REMOVED, old[i++])
                else -> lines += Line(Kind.ADDED, new[j++])
            }
        }
        return lines
    }

    /**
     * [lines] without the unchanged ones farther than [context] lines from a change: each run of those becomes one null.
     */
    public fun withContext(lines: List<Line>, context: Int = 2): List<Line?> {
        val changed = lines.indices.filter { lines[it].kind != Kind.SAME }
        val result = mutableListOf<Line?>()
        var isSkipping = false
        lines.forEachIndexed { index, line ->
            if (changed.any { abs(it - index) <= context }) {
                result += line
                isSkipping = false
            } else if (!isSkipping) {
                result += null
                isSkipping = true
            }
        }
        return result
    }
}
