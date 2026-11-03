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

import com.pandulapeter.kubrikoCreator.commands.PlayTarget
import com.pandulapeter.kubrikoCreator.commands.PlayTargets
import com.pandulapeter.kubrikoCreator.foundation.JsonFileStore
import com.pandulapeter.kubrikoCreator.foundation.LatestRequests
import com.pandulapeter.kubrikoCreator.foundation.valueOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Where the game can be played, and the place the user chose for Play, kept for the project on this computer in
 * [FILE], and looked for as soon as the project opens. A chosen place that isn't there now (a device unplugged) stays chosen, and Play uses the desktop until it is back.
 *
 * @param sources Each finds the places of one kind the game can be played on now, besides the desktop, and may block.
 */
public class PlayTargetChoice internal constructor(
    rootDirectory: File,
    private val sources: List<suspend () -> FoundPlaces>,
    private val scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val store = JsonFileStore(File(rootDirectory, FILE), LocalPlaySettings.serializer())
    private val chosenId = MutableStateFlow<String?>(null)

    @Volatile
    private var configured = setOf(PlayTarget.Kind.DESKTOP)
    private val saves = Channel<String>(Channel.CONFLATED)
    private val refreshes = LatestRequests()
    private val _targets = MutableStateFlow(PlayTargets(available = listOf(PlayTarget.DESKTOP), chosen = PlayTarget.DESKTOP))

    /**
     * Where the game could be played when [refresh] last looked, and the one Play uses.
     */
    public val targets: StateFlow<PlayTargets> = _targets.asStateFlow()

    init {
        scope.launch {
            val saved = withContext(ioDispatcher) { store.read().valueOrNull()?.target }
            chosenId.compareAndSet(null, saved)
            refresh()
        }
        scope.launch(ioDispatcher) {
            for (id in saves) store.write(LocalPlaySettings(target = id.takeUnless { it == PlayTarget.DESKTOP.id }))
        }
    }

    /**
     * Makes [target] the place Play uses, and keeps it.
     */
    public fun choose(target: PlayTarget) {
        chosenId.value = target.id
        update(_targets.value.available)
        saves.trySend(target.id)
    }

    /**
     * Starts looking again for where the game can be played, as [refresh] does.
     */
    public fun startRefresh() {
        scope.launch { refresh() }
    }

    /**
     * Looks again for where the game can be played, which a source that fails leaves out, kind and all.
     */
    public suspend fun refresh(): PlayTargets {
        val request = refreshes.start()
        val found = coroutineScope {
            sources.map { source ->
                async {
                    try {
                        withContext(ioDispatcher) { source() }
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        FoundPlaces.NONE
                    }
                }
            }.awaitAll()
        }
        val available = listOf(PlayTarget.DESKTOP) + found.flatMap { it.targets }.filter { it != PlayTarget.DESKTOP }
        val foundConfigured = setOf(PlayTarget.Kind.DESKTOP) + found.mapNotNull { it.kind }
        return if (refreshes.isLatest(request)) {
            configured = foundConfigured
            update(available)
        } else {
            PlayTargets(available, chosenAmong(available), foundConfigured)
        }
    }

    private fun update(available: List<PlayTarget>) = PlayTargets(available, chosenAmong(available), configured).also { _targets.value = it }

    private fun chosenAmong(available: List<PlayTarget>) = available.firstOrNull { it.id == chosenId.value } ?: PlayTarget.DESKTOP

    internal companion object {
        const val FILE = ".kubriko/local/play.json"
    }
}
