/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConditionReferencesTest {
    private val gone = ConditionDefinition.Toggle("gone")
    private val kept = ConditionDefinition.Toggle("kept")
    private fun isGone(condition: ConditionDefinition) = condition == gone

    @Test
    fun aConditionThatIsGoneMeansNothingAndOneThatIsntStays() {
        assertNull(gone.without(::isGone))
        assertEquals(kept, kept.without(::isGone))
    }

    @Test
    fun anOrLosesOnlyWhatIsGoneAndMeansNothingOnceNothingIsLeft() {
        assertEquals(ConditionDefinition.Combined(listOf(kept), isAll = false), ConditionDefinition.Combined(listOf(gone, kept), isAll = false).without(::isGone))
        assertNull(ConditionDefinition.Combined(listOf(gone, gone), isAll = false).without(::isGone))
        assertEquals(ConditionDefinition.Combined(emptyList(), isAll = false), ConditionDefinition.Combined(emptyList(), isAll = false).without(::isGone))
    }

    @Test
    fun anAndWithAPartThatIsGoneMeansNothingButKeepsAnOrInsideItThatStillDoes() {
        assertNull(ConditionDefinition.Combined(listOf(gone, kept)).without(::isGone))
        val nested = ConditionDefinition.Combined(listOf(kept, ConditionDefinition.Combined(listOf(kept, gone), isAll = false)))
        assertEquals(ConditionDefinition.Combined(listOf(kept, ConditionDefinition.Combined(listOf(kept), isAll = false))), nested.without(::isGone))
    }
}
