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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShowcaseDeeplinkTest {

    @Test
    fun everyDeeplinkResolvesToItsEntryOnlyWhenTheEntryIsAvailable() = ShowcaseEntry.entries
        .forEach { assertEquals(it.takeIf { entry -> entry.isAvailable }, it.deeplink.processDeeplink(), it.name) }

    @Test
    fun everyEntryHasADistinctNonBlankDeeplink() {
        val deeplinks = ShowcaseEntry.entries.map { it.deeplink }
        deeplinks.forEach { assertTrue(!it.isNullOrBlank(), it) }
        assertEquals(deeplinks.size, deeplinks.toSet().size)
    }

    @Test
    fun theWelcomeScreenHasNoDeeplink() = assertNull((null as ShowcaseEntry?).deeplink)

    @Test
    fun theLastNonBlankPathSegmentIsMatchedTrimmedAndLowercased() = listOf(
        "/kubriko/physics/",
        " Physics ",
        "PHYSICS",
    ).forEach { assertEquals(ShowcaseEntry.PHYSICS, it.processDeeplink(), it) }

    @Test
    fun unknownAndEmptyDeeplinksResolveToNothing() = listOf(
        "unknown",
        "",
        "/",
        "  ",
        null,
    ).forEach { assertNull(it.processDeeplink(), it) }
}
