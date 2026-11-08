/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.screenplay

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pandulapeter.kubrikoCreator.screenplay.Mention
import com.pandulapeter.kubrikoCreator.screenplay.PageLinks
import com.pandulapeter.kubrikoCreator.ui.components.Accent
import com.pandulapeter.kubrikoCreator.ui.components.MarkdownChip
import com.pandulapeter.kubrikoCreator.ui.components.MarkdownLinks
import java.io.File

/**
 * What the links of the page or document the page editor shows point at: its mentions, drawn as chips in their kind's
 * accent that open what they name ([onOpen]), and its web addresses, shown first ([pendingAddress]) and only then
 * opened in the browser.
 *
 * @param pagePath The path of the page or document from the project's root, which its links are written from.
 * @param rootDirectory The project's folder, which images are read from.
 */
internal class PageLinksState(
    val pagePath: String,
    private val rootDirectory: File,
    private val onOpen: (Mention) -> Unit,
) : MarkdownLinks {

    /**
     * What the page can mention, which typing `@` offers.
     */
    var candidates: List<MentionCandidate> by mutableStateOf(emptyList())

    /**
     * What each destination of the page names, as last read ([mentionLooksOf]).
     */
    var looks: Map<String, MentionLook> by mutableStateOf(emptyMap())

    /**
     * The web address a click asked to open, until it is opened or dismissed.
     */
    var pendingAddress: String? by mutableStateOf(null)

    override fun chipOf(destination: String): MarkdownChip? = looks[destination]?.let { chipOf(it, onOpen) }

    override fun imageFileOf(destination: String): File? = looks[destination]
        ?.takeIf { it.exists && it.mention.path.substringAfterLast('.').lowercase() in imageExtensions }
        ?.let { File(rootDirectory, it.mention.path) }

    override fun onLinkClick(destination: String) {
        if (looks[destination] == null && isWebAddress(destination)) pendingAddress = destination
    }
}

/**
 * What a mention names, and whether its file is there.
 */
internal data class MentionLook(
    val mention: Mention,
    val exists: Boolean,
)

/**
 * The chip a mention is drawn as: in the accent of its kind, opening what it names if Kubriko Creator can show that
 * and it is there, or null for a file of no kind Kubriko Creator knows, which stays an ordinary link.
 */
internal fun chipOf(look: MentionLook, onOpen: (Mention) -> Unit): MarkdownChip? {
    val mention = look.mention
    val accent = accentOf(mention) ?: return null
    val canOpen = look.exists && when (mention) {
        is Mention.ActorType, is Mention.ManagerType, is Mention.Scene, is Mention.Page, is Mention.Document -> true
        else -> false
    }
    return MarkdownChip(accent = accent, isMissing = !look.exists, onClick = if (canOpen) ({ onOpen(mention) }) else null)
}

/**
 * The accent of [mention]'s kind, as the sidebar shows it, or null for a file of no kind Kubriko Creator knows.
 */
internal fun accentOf(mention: Mention): Accent? = when (mention) {
    is Mention.ActorType -> Accent.CAST
    is Mention.ManagerType -> Accent.CREW
    is Mention.Scene -> Accent.SCENE
    is Mention.Page -> Accent.SCREENPLAY
    is Mention.Audio -> Accent.AUDIO
    is Mention.Document, is Mention.Sprite, is Mention.Kotlin -> Accent.CODE
    is Mention.OtherFile -> null
}

/**
 * What each link and image of [text], the page or document at [pagePath] (from the project in [rootDirectory]), names
 * in the project, by its destination as written. Reads the disk, so it runs off the main thread.
 */
internal fun mentionLooksOf(rootDirectory: File, pagePath: String, text: String): Map<String, MentionLook> = PageLinks.linksOf(text)
    .mapNotNull { link -> PageLinks.resolve(pagePath, link.destination)?.let { link.destination to it } }
    .distinct()
    .associate { (destination, path) -> destination to MentionLook(Mention.of(path), File(rootDirectory, path).exists()) }

/**
 * Whether [destination] is an address the browser opens.
 */
internal fun isWebAddress(destination: String): Boolean = webAddress.containsMatchIn(destination)

private val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")
private val webAddress = Regex("^(https?://|mailto:)", RegexOption.IGNORE_CASE)
