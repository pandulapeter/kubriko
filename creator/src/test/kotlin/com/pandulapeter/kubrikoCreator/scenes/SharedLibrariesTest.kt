/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.scenes.SharedLibraries.NewerLibrary
import com.pandulapeter.kubrikoCreator.scenes.SharedLibraries.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLibrariesTest {

    @Test
    fun sameVersionsAreCompatible() {
        assertEquals(Verdict.Compatible, SharedLibraries.check(mapOf(STDLIB to "2.4.21", UI to "1.12.2"), mapOf(STDLIB to "2.4.21", UI to "1.12.2")))
    }

    @Test
    fun olderProjectVersionsAreCompatible() {
        assertEquals(Verdict.Compatible, SharedLibraries.check(mapOf(STDLIB to "2.3.0", COROUTINES to "1.10.0"), mapOf(STDLIB to "2.4.21", COROUTINES to "1.11.0")))
    }

    @Test
    fun modulesOnlyOneSideHasAreIgnored() {
        assertEquals(Verdict.Compatible, SharedLibraries.check(mapOf("com.example:game-library" to "9.0.0"), mapOf(STDLIB to "2.4.21")))
    }

    @Test
    fun noProjectLibrariesAreCompatible() {
        assertEquals(Verdict.Compatible, SharedLibraries.check(emptyMap(), mapOf(STDLIB to "2.4.21")))
    }

    @Test
    fun aNewerMinorOfComposeIsRefused() {
        assertEquals(Verdict.Refused(NewerLibrary(UI, "1.13.0", "1.12.2")), SharedLibraries.check(mapOf(UI to "1.13.0"), mapOf(UI to "1.12.2")))
        assertEquals(Verdict.Refused(NewerLibrary(UI, "1.13.0-alpha01", "1.12.2")), SharedLibraries.check(mapOf(UI to "1.13.0-alpha01"), mapOf(UI to "1.12.2")))
    }

    @Test
    fun aNewerMinorOfKotlinIsRefused() {
        assertEquals(Verdict.Refused(NewerLibrary(STDLIB, "2.5.0", "2.4.21")), SharedLibraries.check(mapOf(STDLIB to "2.5.0"), mapOf(STDLIB to "2.4.21")))
    }

    @Test
    fun aNewerPatchOfComposeOrKotlinOnlyWarns() {
        assertEquals(
            Verdict.Newer(listOf(NewerLibrary(UI, "1.12.3", "1.12.2"))),
            SharedLibraries.check(mapOf(UI to "1.12.3"), mapOf(UI to "1.12.2")),
        )
        assertEquals(
            Verdict.Newer(listOf(NewerLibrary(STDLIB, "2.4.22", "2.4.21"))),
            SharedLibraries.check(mapOf(STDLIB to "2.4.22"), mapOf(STDLIB to "2.4.21")),
        )
    }

    @Test
    fun aNewerVersionOfAnotherLibraryOnlyWarns() {
        assertEquals(
            Verdict.Newer(listOf(NewerLibrary(REFLECT, "2.5.0", "2.4.21"), NewerLibrary(COROUTINES, "1.12.0", "1.11.0"))),
            SharedLibraries.check(mapOf(COROUTINES to "1.12.0", REFLECT to "2.5.0"), mapOf(COROUTINES to "1.11.0", REFLECT to "2.4.21")),
        )
    }

    @Test
    fun kubrikoModulesAreLeftToTheKubrikoVersionCheck() {
        assertEquals(Verdict.Compatible, SharedLibraries.check(mapOf(ENGINE to "2.0.0"), mapOf(ENGINE to "1.0.0")))
    }

    private companion object {
        const val STDLIB = "org.jetbrains.kotlin:kotlin-stdlib"
        const val REFLECT = "org.jetbrains.kotlin:kotlin-reflect"
        const val UI = "org.jetbrains.compose.ui:ui-desktop"
        const val COROUTINES = "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm"
        const val ENGINE = "io.github.pandulapeter.kubriko:engine"
    }
}
