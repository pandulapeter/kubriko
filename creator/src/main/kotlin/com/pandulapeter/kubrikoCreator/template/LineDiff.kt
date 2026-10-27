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
internal object LineDiff {

    enum class Kind {
        SAME,
        REMOVED,
        ADDED,
    }

    data class Line(
        val kind: Kind,
        val text: String,
    )

    /**
     * Every line of [before] and [after], each one marked as kept, removed, or added (a longest common subsequence, which
     * is plenty for the small template files it shows).
     */
    fun of(before: String, after: String): List<Line> {
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
    fun withContext(lines: List<Line>, context: Int = 2): List<Line?> {
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
