/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoShowcase.implementation

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.shared.SceneEditorConnection
import com.pandulapeter.kubriko.shared.StateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ShowcaseSessionTest {

    private class FakeStateHolder : StateHolder {
        override val kubriko: Flow<Kubriko?> = emptyFlow()
        var disposeCount = 0

        override fun dispose() {
            disposeCount++
        }
    }

    private val createdStateHolders = mutableListOf<Pair<ShowcaseEntry, FakeStateHolder>>()
    private val handedConnections = mutableListOf<SceneEditorConnection>()
    private val session = ShowcaseSession { entry, sceneEditorConnection ->
        handedConnections += sceneEditorConnection
        FakeStateHolder().also { createdStateHolders += entry to it }
    }

    @Test
    fun holderForReturnsTheSameInstanceUntilReleased() {
        val first = session.holderFor(ShowcaseEntry.WALLBREAKER)
        assertSame(first, session.holderFor(ShowcaseEntry.WALLBREAKER))
        session.release(ShowcaseEntry.WALLBREAKER)
        assertNotSame(first, session.holderFor(ShowcaseEntry.WALLBREAKER))
    }

    @Test
    fun releaseDisposesExactlyThatEntrysHolder() {
        val released = session.holderFor(ShowcaseEntry.WALLBREAKER) as FakeStateHolder
        val kept = session.holderFor(ShowcaseEntry.PHYSICS) as FakeStateHolder
        session.release(ShowcaseEntry.WALLBREAKER)
        assertEquals(1, released.disposeCount)
        assertEquals(0, kept.disposeCount)
        assertSame(kept, session.holderFor(ShowcaseEntry.PHYSICS))
    }

    @Test
    fun releasingAnEntryWithoutAHolderDoesNothing() {
        session.release(ShowcaseEntry.WALLBREAKER)
        assertTrue(createdStateHolders.isEmpty())
    }

    @Test
    fun selectUpdatesTheSelectedEntryAndCreatesItsHolder() {
        assertNull(session.selectedEntry.value)
        session.select(ShowcaseEntry.PHYSICS)
        assertEquals(ShowcaseEntry.PHYSICS, session.selectedEntry.value)
        assertTrue(session.isSelected(ShowcaseEntry.PHYSICS))
        assertFalse(session.isSelected(ShowcaseEntry.WALLBREAKER))
        assertEquals(listOf(ShowcaseEntry.PHYSICS), createdStateHolders.map { it.first })
        session.select(null)
        assertNull(session.selectedEntry.value)
        assertFalse(session.isSelected(ShowcaseEntry.PHYSICS))
    }

    @Test
    fun sceneEditorConnectionOutlivesTheReleasedHolder() {
        val connection = session.sceneEditorConnectionFor(ShowcaseEntry.PHYSICS)
        connection.toggle()
        session.holderFor(ShowcaseEntry.PHYSICS)
        session.release(ShowcaseEntry.PHYSICS)
        session.holderFor(ShowcaseEntry.PHYSICS)
        assertEquals(listOf(connection, connection), handedConnections)
        assertSame(connection, session.sceneEditorConnectionFor(ShowcaseEntry.PHYSICS))
        assertTrue(connection.isVisible.value)
    }

    @Test
    fun eachEntryHasItsOwnSceneEditorConnection() {
        assertNotSame(
            session.sceneEditorConnectionFor(ShowcaseEntry.PHYSICS),
            session.sceneEditorConnectionFor(ShowcaseEntry.PERFORMANCE),
        )
    }
}