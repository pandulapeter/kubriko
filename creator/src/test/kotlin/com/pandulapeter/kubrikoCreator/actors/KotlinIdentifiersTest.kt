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

class KotlinIdentifiersTest {

    private fun rename(code: String, receiver: String? = null) = KotlinIdentifiers.withRenamed(code, "score", "points", receiver)

    @Test
    fun receiversAreThePlainIdentifiersBeforeADot() {
        val receivers = KotlinIdentifiers.find("a.b.c\nthis.d\nf()?.g\nh\n    .i\n1.5f..j").associate { it.name to it.receiver }
        assertEquals(mapOf("a" to null, "b" to "a", "c" to "", "this" to null, "d" to "this", "f" to null, "g" to "", "h" to null, "i" to "h", "j" to null), receivers)
    }

    @Test
    fun ownIdentifiersAreRenamedButNotMembersOfSomethingElse() {
        assertEquals("points += 1f; this.points; other.score; scores; _score", rename("score += 1f; this.score; other.score; scores; _score"))
    }

    @Test
    fun membersOfTheReceiverAreRenamed() {
        assertEquals("game.points + score + other.score + x.game.score", rename("game.score + score + other.score + x.game.score", receiver = "game"))
    }

    @Test
    fun stringsAndCommentsAreLeftAloneExceptForTheirTemplates() {
        assertEquals(
            "\"score \$points \${points + 1} \${\"score\"}\" // score\n/* score /* score */ score */ points 'c' \"\"\"score \$points\"\"\"",
            rename("\"score \$score \${score + 1} \${\"score\"}\" // score\n/* score /* score */ score */ score 'c' \"\"\"score \$score\"\"\""),
        )
        assertEquals("\"a\\\"score\" + points + '\\'' + points", rename("\"a\\\"score\" + score + '\\'' + score"))
    }

    @Test
    fun bracesInsideTemplatesDoNotEndThem() {
        assertEquals("\"\${list.map { points }} score\" + points", rename("\"\${list.map { score }} score\" + score"))
    }
}
