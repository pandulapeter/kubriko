/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.gradle

import kotlin.test.Test
import kotlin.test.assertEquals

class ProblemMessageTest {

    @Test
    fun aKotlinCompilerErrorShowsItsDetailsRatherThanItsGenericLabel() = assertEquals(
        "Initializer type mismatch: expected 'Int', actual 'String'.",
        problemMessageOf(
            details = "Initializer type mismatch: expected 'Int', actual 'String'.",
            contextualLabel = "Kotlin compiler error",
            displayName = "Kotlin compiler: INITIALIZER_TYPE_MISMATCH",
        ),
    )

    @Test
    fun aProblemWithoutDetailsShowsItsLabel() = assertEquals(
        "Kotlin compiler error",
        problemMessageOf(details = " ", contextualLabel = "Kotlin compiler error", displayName = "Kotlin compiler: SOMETHING"),
    )

    @Test
    fun aProblemWithoutDetailsOrLabelShowsTheNameOfItsKind() = assertEquals(
        "Kotlin compiler: SOMETHING",
        problemMessageOf(details = null, contextualLabel = null, displayName = "Kotlin compiler: SOMETHING"),
    )
}
