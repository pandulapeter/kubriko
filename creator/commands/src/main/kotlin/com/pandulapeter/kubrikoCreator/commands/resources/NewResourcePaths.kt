/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.commands.resources

import com.pandulapeter.kubrikoCreator.actors.NameFlaw
import com.pandulapeter.kubrikoCreator.actors.ResourceKind
import com.pandulapeter.kubrikoCreator.commands.CommandContext
import java.text.Normalizer

/**
 * Where a file chosen, dropped, or pasted is added ([AddResource]), named after it so that the user needn't name it first.
 */
public object NewResourcePaths {

    /**
     * The path from `composeResources` a file called [fileName] is added at, beside the files at [taken]: its kind's
     * folder ([ResourceKind.pathOf]) and its name made one the rules allow (accents dropped, every other sign a dash, so
     * `Hős lövés!.wav` becomes `files/sounds/Hos-loves.wav`), numbered (`ball-2`) while the game would mix it up with one
     * there; or null if no sprite, sound, or music has its extension.
     */
    public fun of(fileName: String, taken: Collection<String>): String? {
        val extension = fileName.substringAfterLast('.', "")
        val kind = ResourceKind.ofExtension(extension) ?: return null
        val name = allowedName(fileName.substringBeforeLast('.')).ifEmpty { kind.name.lowercase() }
        return generateSequence(1) { it + 1 }
            .map { number -> ResourceKind.pathOf(kind, if (number == 1) name else "$name-$number", extension.lowercase()) }
            .first { path -> taken.none { areMixedUp(it, path) } }
    }

    private fun allowedName(name: String) = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
        .filter { !NameFlaw.isAccent(it.code) && !NameFlaw.isSelector(it.code) }
        .map { if (NameFlaw.isLetterOrDigitAToZ(it) || it == '_' || it == '-') it else '-' }
        .joinToString("")
        .replace(dashes, "-")
        .trimStart { !NameFlaw.isLetterOrDigitAToZ(it) }
        .trimEnd('-')

    private val dashes = Regex("-{2,}")
}

/**
 * Where each of [fileNames], added together, would go among the sprites, sounds, and music the last resource scan found
 * and those added before it ([NewResourcePaths.of]), or null for one that can't be any.
 */
public fun CommandContext.newResourcePathsOf(fileNames: List<String>): List<String?> {
    val taken = knownResourcePaths().orEmpty().toMutableList()
    return fileNames.map { fileName -> NewResourcePaths.of(fileName, taken)?.also(taken::add) }
}
