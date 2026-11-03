/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.context

/**
 * A [ProjectDigest] as the text the AI reads, kept under a budget: a large project loses its types' Properties and
 * Events first, then their descriptions, and is cut short last, saying which tools have the rest.
 */
internal object DigestText {

    /**
     * In characters: about 2,000 tokens.
     */
    const val BUDGET = 8_000

    private enum class Level { FULL, DESCRIBED, NAMES }

    fun of(digest: ProjectDigest, budget: Int = BUDGET): String {
        Level.entries.forEach { level -> format(digest, level).let { if (it.length <= budget) return it } }
        return cut(format(digest, Level.NAMES), budget)
    }

    private fun format(digest: ProjectDigest, level: Level) = buildString {
        append("Project ").append(digest.projectName).append(": ")
        append(count(digest.actorTypes.size, "Actor type")).append(", ")
        append(count(digest.managerTypes.size, "Manager type")).append(", and ")
        append(count(digest.scenes.size, "Scene")).append(".\n")
        types("Actor types", digest.actorTypes, level)
        types("Manager types", digest.managerTypes, level)
        if (digest.scenes.isNotEmpty()) {
            append("\nScenes:\n")
            digest.scenes.forEach { scene ->
                append("- ").append(scene.name)
                if (level != Level.NAMES) scene.description?.let { append(": ").append(it) }
                if (scene.actorCounts.isNotEmpty()) {
                    append(". Places ")
                    append(scene.actorCounts.entries.joinToString(", ") { (type, count) -> if (count > 1) "$type ($count)" else type })
                }
                append(".\n")
            }
        }
        if (digest.relations.isNotEmpty()) {
            append("\nReferences:\n")
            digest.relations.groupBy { it.owner }.forEach { (owner, lines) ->
                append("- ").append(owner.toString()).append(' ')
                append(lines.joinToString("; ") { line -> line.kind.verb + " " + line.subjects.joinToString(", ") })
                append(".\n")
            }
        }
    }

    private fun StringBuilder.types(title: String, types: List<ProjectDigest.Type>, level: Level) {
        if (types.isEmpty()) return
        append('\n').append(title).append(":\n")
        if (level == Level.NAMES) {
            types.groupBy { it.folder }.forEach { (folder, inFolder) ->
                append("- ")
                if (folder.isNotEmpty()) append(folder).append("/: ")
                append(inFolder.joinToString(", ") { it.name }).append('\n')
            }
            return
        }
        types.forEach { type ->
            append("- ")
            if (type.folder.isNotEmpty()) append(type.folder).append('/')
            append(type.name)
            type.description?.let { append(": ").append(it) }
            type.parent?.let { append(". Parent ").append(it) }
            if (level == Level.FULL) {
                if (type.properties.isNotEmpty()) append(". Properties ").append(type.properties.joinToString(", "))
                if (type.events.isNotEmpty()) append(". Events: ").append(type.events.joinToString("; "))
            }
            append(".\n")
        }
    }

    private fun cut(text: String, budget: Int): String {
        val end = text.lastIndexOf('\n', (budget - CUT_NOTE_LENGTH).coerceAtLeast(0)).coerceAtLeast(0)
        val left = text.substring(end).count { it == '\n' }
        return text.substring(0, end) + "\n(Cut short, $left more lines: list_actor_types, list_scenes, and get_relations have the rest.)\n"
    }

    private fun count(count: Int, noun: String) = "$count $noun" + if (count == 1) "" else "s"

    private const val CUT_NOTE_LENGTH = 120
}
