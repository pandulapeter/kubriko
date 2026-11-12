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
import com.pandulapeter.kubrikoCreator.context.RelationSubject
import com.pandulapeter.kubrikoCreator.context.Usage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * How far reading the usages of [subject] got.
 */
internal sealed interface UsagesState {
    val subject: RelationSubject?

    data class Reading(
        override val subject: RelationSubject?,
    ) : UsagesState

    data class Read(
        override val subject: RelationSubject,
        val usages: List<Usage>,
    ) : UsagesState

    /**
     * It is no longer in the project: renamed or deleted since it was asked about.
     */
    data class Missing(
        override val subject: RelationSubject,
    ) : UsagesState

    data class Failed(
        override val subject: RelationSubject,
        val message: String,
    ) : UsagesState
}

/**
 * The usages of [subject] as [findUsages] reads them (null for a subject no longer in the project), read again a moment
 * after [revision] changes (the project changed), so a burst of changes (typing into a field) reads once, still showing
 * the last ones of the same subject meanwhile, so the list doesn't blink.
 */
@Composable
internal fun rememberUsages(subject: RelationSubject?, revision: Any?, findUsages: suspend (RelationSubject) -> List<Usage>?): UsagesState {
    val state by produceState<UsagesState>(UsagesState.Reading(subject), subject, revision) {
        if (subject == null) return@produceState
        if (value.subject == subject && value !is UsagesState.Reading) delay(REFRESH_PAUSE_IN_MILLISECONDS)
        value = try {
            findUsages(subject)?.let { UsagesState.Read(subject, it) } ?: UsagesState.Missing(subject)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            UsagesState.Failed(subject, exception.message ?: exception.toString())
        }
    }
    return state.takeIf { it.subject == subject } ?: UsagesState.Reading(subject)
}

/**
 * How long a search waits after the project changed before reading again, so the changes of one burst read once.
 */
internal const val REFRESH_PAUSE_IN_MILLISECONDS = 300L
