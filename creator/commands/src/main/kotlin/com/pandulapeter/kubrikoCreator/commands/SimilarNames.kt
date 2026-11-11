/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands

import com.pandulapeter.kubrikoCreator.foundation.DomainText

/**
 * The names a rejection offers in place of one that doesn't exist, so an AI that misspelled one, got its case wrong, or
 * left out its folder fixes the call at once rather than listing everything to find it.
 */
internal object SimilarNames {

    private const val LIMIT = 3

    /**
     * A name this short becomes another word with one changed letter (Wall and Ball), so it is matched only ignoring case
     * or with one letter more or fewer (sped and speed).
     */
    private const val SHORT_NAME_LENGTH = 4

    /**
     * Up to three of [candidates] that look like [name], the closest first: ignoring case, dashes, underscores, and
     * spaces, a typo or two away (only a letter more or fewer for a name of four letters or fewer), or holding
     * it whole (a Scene's name without its folder, Space for Spacebar).
     */
    fun of(name: String, candidates: Iterable<String>): List<String> {
        if (name.isBlank()) return emptyList()
        return candidates.asSequence()
            .filter { it != name }
            .distinct()
            .mapNotNull { candidate -> distanceOf(name, candidate)?.let { candidate to it } }
            .sortedWith(compareBy<Pair<String, Int>> { it.second }.thenBy { it.first })
            .take(LIMIT)
            .map { it.first }
            .toList()
    }

    /**
     * A rejection with [reason] and [arguments] if nothing looks like [name], or with [similarReason], which takes the
     * similar names after the [arguments], if something does.
     */
    fun rejection(reason: DomainText, similarReason: DomainText, name: String, candidates: Iterable<String>, vararg arguments: String): Rejection {
        val similar = of(name, candidates)
        return if (similar.isEmpty()) Rejection(reason, *arguments) else Rejection(similarReason, *arguments, similar.joinToString())
    }

    private fun normalized(name: String) = name.lowercase().filter { it != '-' && it != '_' && it != ' ' }

    /**
     * How far [candidate] is from [wanted], or null if it doesn't look like it. Typos are allowed by the length of the
     * shorter file name, without its folders and extension, so a shared folder doesn't make two names look alike.
     */
    private fun distanceOf(wanted: String, candidate: String): Int? {
        val normalizedWanted = normalized(wanted)
        val normalizedCandidate = normalized(candidate)
        val edits = editDistance(normalizedWanted, normalizedCandidate)
        val shorter = minOf(normalized(fileNameOf(wanted)).length, normalized(fileNameOf(candidate)).length)
        val lowercaseWanted = wanted.lowercase()
        val lowercaseCandidate = candidate.lowercase()
        return when {
            edits == 0 || (edits == 1 && normalizedWanted.length != normalizedCandidate.length) -> edits
            shorter > SHORT_NAME_LENGTH && edits <= maxOf(1, shorter / 4) -> edits
            minOf(wanted.length, candidate.length) >= 3 && (lowercaseCandidate.contains(lowercaseWanted) || lowercaseWanted.contains(lowercaseCandidate)) -> CONTAINED + edits
            else -> null
        }
    }

    private fun fileNameOf(name: String) = name.substringAfterLast('/').substringBeforeLast('.')

    /**
     * The optimal string alignment distance: insertions, deletions, substitutions, and swaps of neighbours count one each.
     */
    private fun editDistance(first: String, second: String): Int {
        val columns = second.length + 1
        val distances = IntArray((first.length + 1) * columns)
        for (row in 0..first.length) distances[row * columns] = row
        for (column in 0..second.length) distances[column] = column
        for (row in 1..first.length) {
            for (column in 1..second.length) {
                val cost = if (first[row - 1] == second[column - 1]) 0 else 1
                var distance = minOf(
                    distances[(row - 1) * columns + column] + 1,
                    distances[row * columns + column - 1] + 1,
                    distances[(row - 1) * columns + column - 1] + cost,
                )
                if (row > 1 && column > 1 && first[row - 1] == second[column - 2] && first[row - 2] == second[column - 1]) {
                    distance = minOf(distance, distances[(row - 2) * columns + column - 2] + 1)
                }
                distances[row * columns + column] = distance
            }
        }
        return distances[first.length * columns + second.length]
    }

    /**
     * Ranks a name that only holds the wanted one (or is held by it) after every one a few typos away.
     */
    private const val CONTAINED = 1_000
}
