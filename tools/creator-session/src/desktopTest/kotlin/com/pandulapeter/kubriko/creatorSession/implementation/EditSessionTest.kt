/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.creatorSession.implementation

import com.pandulapeter.kubriko.creator.CreatorEntryPoint
import com.pandulapeter.kubriko.creatorSession.SessionFailure
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EditSessionTest {

    @Test
    fun theOnlyRegisteredEntryPointIsSelected() {
        val entryPoint = UnusedEntryPoint()
        assertSame(entryPoint, EditSession.selectEntryPoint(listOf(entryPoint)))
    }

    @Test
    fun aProjectWithoutAnEntryPointFailsWithAnExplanation() {
        val failure = assertFailsWith<SessionFailure> { EditSession.selectEntryPoint(emptyList()) }
        assertTrue("no CreatorEntryPoint" in failure.message)
    }

    @Test
    fun aProjectWithSeveralEntryPointsFailsNamingThem() {
        val failure = assertFailsWith<SessionFailure> { EditSession.selectEntryPoint(listOf(UnusedEntryPoint(), UnusedEntryPoint())) }
        assertTrue(UnusedEntryPoint::class.java.name in failure.message)
    }

    @Test
    fun aClasspathWithoutRegistrationsFailsInsteadOfOpeningAnEmptyEditor() {
        assertFailsWith<SessionFailure> { EditSession.run(SessionArguments.Edit(sceneFolderPath = ".", sceneFileName = null)) }
    }

    private class UnusedEntryPoint : CreatorEntryPoint {
        override fun createSerializationManager() = throw NotImplementedError()
    }
}
