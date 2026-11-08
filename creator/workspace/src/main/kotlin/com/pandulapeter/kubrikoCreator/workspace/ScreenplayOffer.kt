/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.workspace

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * The assistant's offer to write the screenplay of a game that has none, made once: taking it or turning it down is kept
 * on this computer (`.kubriko/local/`), so it isn't made again.
 *
 * @param pageNames The screenplay's pages as last known; the offer stands only while there are none.
 * @param pagesRevision Raised by every read of the pages: the offer waits for the first, since there are no pages known
 *   before it.
 */
public class ScreenplayOffer internal constructor(
    rootDirectory: File,
    pageNames: StateFlow<List<String>>,
    pagesRevision: StateFlow<Int>,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val store = LocalProjectStore(rootDirectory, FILE_NAME, LocalScreenplayOffer.serializer())

    /**
     * Whether it was answered, or null until that is read.
     */
    private val isAnswered = MutableStateFlow<Boolean?>(null)

    /**
     * Whether the offer is to be shown now.
     */
    public val isShown: StateFlow<Boolean> = combine(pageNames, pagesRevision, isAnswered) { pages, revision, answered -> revision > 0 && pages.isEmpty() && answered == false }
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch(ioDispatcher) { isAnswered.value = runCatching { store.read()?.isAnswered }.getOrNull() ?: false }
    }

    /**
     * Takes the offer or turns it down; either way it isn't made again.
     */
    public fun answer() {
        isAnswered.value = true
        scope.launch(ioDispatcher) { runCatching { store.write(LocalScreenplayOffer(isAnswered = true)) } }
    }

    private companion object {
        const val FILE_NAME = "screenplay-offer.json"
    }
}

/**
 * Whether the assistant's offer to write the screenplay was answered, kept in `.kubriko/local/`.
 */
@Serializable
internal data class LocalScreenplayOffer(
    @SerialName("isAnswered") val isAnswered: Boolean = false,
)
