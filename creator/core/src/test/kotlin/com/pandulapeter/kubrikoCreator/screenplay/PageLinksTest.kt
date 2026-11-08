/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.screenplay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PageLinksTest {

    @Test
    fun `links and images are read with where their destinations are`() {
        val text = "The [hero](../.kubriko/actors/Player.json \"Title\") jumps.\n![Cave](cave.png)"
        val links = PageLinks.linksOf(text)
        assertEquals(listOf("hero", "Cave"), links.map { it.label })
        assertEquals(listOf("../.kubriko/actors/Player.json", "cave.png"), links.map { it.destination })
        assertEquals(listOf(false, true), links.map { it.isImage })
        links.forEach { assertEquals(it.destination, text.substring(it.destinationRange)) }
    }

    @Test
    fun `code spans, fenced code, and escaped brackets hold no links`() {
        val text = """
            `[a](a.md)` and ``[b](b.md)``
            \[c](c.md)
            ```
            [d](d.md)
            ```
            ~~~~
            ```
            [e](e.md)
            ~~~~
            [f](f.md)
        """.trimIndent()
        assertEquals(listOf("f.md"), PageLinks.linksOf(text).map { it.destination })
    }

    @Test
    fun `angle brackets, parentheses, and a linked image are read`() {
        val text = "[a](<my file.md>) [b](wiki/Foo_(bar).md) [![logo](logo.png)](https://example.com)"
        val links = PageLinks.linksOf(text)
        assertEquals(listOf("my file.md", "wiki/Foo_(bar).md", "logo.png", "https://example.com"), links.map { it.destination })
        assertEquals(listOf(true, false, false, false), links.map { it.isInAngleBrackets })
        assertTrue(links[2].isImage)
    }

    @Test
    fun `text that only looks like a link is none`() {
        assertEquals(emptyList(), PageLinks.linksOf("[a] (b.md), [c](), [d](e.md \"unclosed), [f](g.md"))
    }

    @Test
    fun `destinations resolve from the page's folder`() {
        assertEquals(".kubriko/actors/Player.json", PageLinks.resolve("screenplay/README.md", "../.kubriko/actors/Player.json"))
        assertEquals("screenplay/levels/cave.md", PageLinks.resolve("screenplay/README.md", "./levels/cave.md#bats"))
        assertEquals("screenplay/my file.md", PageLinks.resolve("screenplay/levels/cave.md", "../my%20file.md"))
        assertEquals("screenplay/lévels.md", PageLinks.resolve("screenplay/README.md", "l%C3%A9vels.md?raw=true"))
    }

    @Test
    fun `destinations outside the project resolve to nothing`() {
        listOf("https://example.com", "mailto:a@b.c", "/etc/hosts", "#planned", "../../outside.md", "C:/game.md").forEach { destination ->
            assertNull(PageLinks.resolve("screenplay/README.md", destination), destination)
        }
    }

    @Test
    fun `a destination is written relative to the page and escaped`() {
        assertEquals("../.kubriko/actors/Player.json", PageLinks.destinationOf("screenplay/README.md", ".kubriko/actors/Player.json"))
        assertEquals("cave.md", PageLinks.destinationOf("screenplay/levels/forest.md", "screenplay/levels/cave.md"))
        assertEquals("../../my%20notes%20%28old%29.md", PageLinks.destinationOf("screenplay/levels/forest.md", "my notes (old).md"))
        assertEquals("../README.md", PageLinks.destinationOf("screenplay/levels/forest.md", "screenplay/README.md"))
    }

    @Test
    fun `links to a moved file follow it, keeping their place in it`() {
        val text = "[Hero](../.kubriko/actors/Player.json#events) and [ball](../.kubriko/actors/Ball.json)\r\n"
        val moved = PageLinks.withMovesFollowed(text, "screenplay/README.md", listOf(PathMove(".kubriko/actors/Player.json", ".kubriko/actors/heroes/Hero.json")))
        assertEquals("[Hero](../.kubriko/actors/heroes/Hero.json#events) and [ball](../.kubriko/actors/Ball.json)\r\n", moved)
    }

    @Test
    fun `links into a moved folder follow it`() {
        val text = "[Cave](levels/cave.md) [Forest](<levels/deep woods.md>)"
        val moved = PageLinks.withMovesFollowed(text, "screenplay/README.md", listOf(PathMove("screenplay/levels", "screenplay/areas")))
        assertEquals("[Cave](areas/cave.md) [Forest](<areas/deep woods.md>)", moved)
    }

    @Test
    fun `a moved page's own links are written again from where it is now`() {
        val text = "[Hero](../.kubriko/actors/Player.json), [me](hero.md#top), [web](https://example.com), [up](../../x.md)"
        val moved = PageLinks.withMovesFollowed(text, "screenplay/hero.md", listOf(PathMove("screenplay/hero.md", "screenplay/cast/hero.md")))
        assertEquals("[Hero](../../.kubriko/actors/Player.json), [me](hero.md#top), [web](https://example.com), [up](../../../x.md)", moved)
    }

    @Test
    fun `a moved page keeps its links above the project and its escaped ones pointing where they did`() {
        val text = "[x](../../shared/notes.md) [y](my\\_page.md) [z](<../a b.md>)"
        val moved = PageLinks.withMovesFollowed(text, "screenplay/a.md", listOf(PathMove("screenplay/a.md", "screenplay/deep/er/a.md")))
        assertEquals("[x](../../../../shared/notes.md) [y](../../my_page.md) [z](<../../../a b.md>)", moved)
        assertEquals("screenplay/my_page.md", PageLinks.resolve("screenplay/a.md", "my\\_page.md"))
    }

    @Test
    fun `a moved page's linked picture keeps both its links whole`() {
        val text = "[![logo](logo.png)](other.md) after"
        val moved = PageLinks.withMovesFollowed(text, "screenplay/a.md", listOf(PathMove("screenplay/a.md", "screenplay/deep/a.md")))
        assertEquals("[![logo](../logo.png)](../other.md) after", moved)
    }

    @Test
    fun `a page nothing moved for is left exactly as written`() {
        val text = "[a](./x/../a.md) [b](%62.md) `[c](c.md)`"
        assertEquals(text, PageLinks.withMovesFollowed(text, "screenplay/README.md", listOf(PathMove("screenplay/c.md", "screenplay/d.md"))))
    }

    @Test
    fun `a move touches only its own path and what is in it`() {
        val move = PathMove("screenplay/levels", "screenplay/areas")
        assertEquals("screenplay/areas/cave.md", move.applyTo("screenplay/levels/cave.md"))
        assertNull(move.applyTo("screenplay/levels2/cave.md"))
        assertEquals(move, move.reversed().reversed())
        assertFalse(PageLinks.linksOf("[x](levels/cave.md)").first().isImage)
    }

    @Test
    fun `links to a deleted path become their labels, and nothing else changes`() {
        val page = "The [ball](../.kubriko/actors/Ball.json) and the <[paddle](../.kubriko/actors/Paddle.json)>, " +
            "a [linked ![picture](ball.png)](../.kubriko/actors/Ball.json), `[code](../.kubriko/actors/Ball.json)`, and [Ball](<../.kubriko/actors/Ball.json#speed>)."
        assertEquals(
            "The ball and the <[paddle](../.kubriko/actors/Paddle.json)>, a linked ![picture](ball.png), `[code](../.kubriko/actors/Ball.json)`, and Ball.",
            PageLinks.withLinksToPathAsText(page, "screenplay/README.md", ".kubriko/actors/Ball.json"),
        )
        assertEquals("See Ball for more.", PageLinks.withLinksToPathAsText("See [](../.kubriko/actors/Ball.json) for more.", "screenplay/README.md", ".kubriko/actors/Ball.json"))
        assertEquals("Line one.\n![Ball](ball.png)\n", PageLinks.withLinksToPathAsText("Line one.\n![Ball](ball.png)\n", "screenplay/README.md", "screenplay/ball.png"))
    }
}
