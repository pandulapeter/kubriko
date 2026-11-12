/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.pandulapeter.kubrikoCreator.context.SearchResults
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * How far searching the project's texts for [query] got.
 */
internal sealed interface TextSearchState {
    val query: String

    /**
     * Nothing is typed.
     */
    data object Idle : TextSearchState {
        override val query: String = ""
    }

    data class Searching(
        override val query: String,
    ) : TextSearchState

    data class Found(
        override val query: String,
        val results: SearchResults,
    ) : TextSearchState

    data class Failed(
        override val query: String,
        val message: String,
    ) : TextSearchState
}

/**
 * What [search] finds for [query] (trimmed), once typing pauses for a moment, searched again whenever [revision] changes
 * (the project changed), still showing what the same query found meanwhile but never another query's.
 */
@Composable
internal fun rememberTextSearch(query: String, revision: Any?, search: suspend (String) -> SearchResults): TextSearchState {
    val trimmed = query.trim()
    val state by produceState<TextSearchState>(TextSearchState.Idle, trimmed, revision) {
        if (trimmed.isEmpty()) {
            value = TextSearchState.Idle
            return@produceState
        }
        if (value.query != trimmed) delay(TYPING_PAUSE_IN_MILLISECONDS)
        value = try {
            TextSearchState.Found(trimmed, search(trimmed))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            TextSearchState.Failed(trimmed, exception.message ?: exception.toString())
        }
    }
    return when {
        trimmed.isEmpty() -> TextSearchState.Idle
        state.query == trimmed -> state
        else -> TextSearchState.Searching(trimmed)
    }
}

/**
 * How long typing has to pause before the project is searched, so a word typed out searches once.
 */
private const val TYPING_PAUSE_IN_MILLISECONDS = 200L
