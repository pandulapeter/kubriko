/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.sceneEditor.implementation.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

class UniqueActorReplacementTest {

    private class A
    private class B
    private class C

    @Test
    fun onlyUniqueActorsOfTheSameClassAreReplaced() {
        val classes = listOf(A::class, B::class)
        assertEquals(1, indexOfReplacedUnique(classes, B::class, isUnique = true))
        assertEquals(-1, indexOfReplacedUnique(classes, B::class, isUnique = false))
        assertEquals(-1, indexOfReplacedUnique(classes, C::class, isUnique = true))
    }
}
