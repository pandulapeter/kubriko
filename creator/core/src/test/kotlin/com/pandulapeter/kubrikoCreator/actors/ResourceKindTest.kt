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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourceKindTest {

    @Test
    fun theKindComesFromTheFolderAndTheExtension() {
        assertEquals(ResourceKind.SPRITE, ResourceKind.of("drawable/ball.PNG"))
        assertEquals(ResourceKind.SPRITE, ResourceKind.of("drawable/ball red.webp"))
        assertEquals(ResourceKind.SOUND, ResourceKind.of("files/hit.wav"))
        assertEquals(ResourceKind.SOUND, ResourceKind.of("files/sounds/enemies/hit.wav"))
        assertEquals(ResourceKind.MUSIC, ResourceKind.of("files/music/theme.ogg"))
        assertNull(ResourceKind.of("drawable/ui/ball.png"))
        assertNull(ResourceKind.of("drawable-hdpi/ball.png"))
        assertNull(ResourceKind.of("drawable/ball.svg"))
        assertNull(ResourceKind.of("files/scenes/hit.wav"))
        assertNull(ResourceKind.of("files/notes.txt"))
        assertNull(ResourceKind.of("ball.png"))
    }

    @Test
    fun namesUseLettersNumbersDashesAndUnderscores() {
        assertNull(ResourceKind.validate("drawable/ball-red_2.png"))
        assertNull(ResourceKind.validate("drawable/1up.png"))
        assertNull(ResourceKind.validate("files/sounds/level-1/hit.wav"))
        assertEquals(NameProblem.NAME_EMPTY, ResourceKind.validate("drawable/.png"))
        assertEquals(NameProblem.PATH_PART_EMPTY, ResourceKind.validate("files//hit.wav"))
        assertEquals(NameProblem.RESOURCE_FIRST_CHARACTER, ResourceKind.validate("files/-sounds/hit.wav"))
        assertEquals(NameProblem.RESOURCE_CHARACTERS, ResourceKind.validate("files/sounds/big hit.wav"))
        assertEquals(NameProblem.RESOURCE_CHARACTERS, ResourceKind.validate("drawable/ball.old.png"))
        assertEquals(NameProblem.NAME_LETTER_BEYOND_A_TO_Z, ResourceKind.validate("drawable/labda-ö.png"))
    }

    @Test
    fun aValidSpriteNameMakesAnAccessor() {
        listOf("drawable/ball-red.png", "drawable/1up.png", "drawable/Ball_2.jpeg").forEach { path ->
            assertNull(ResourceKind.validate(path))
            assertTrue(SpriteFiles.accessorOf(path) != null, path)
        }
    }

    @Test
    fun newFilesGoWhereTheTemplatesKeepThem() {
        assertEquals("drawable/ball.png", ResourceKind.pathOf(ResourceKind.SPRITE, "ball", "png"))
        assertEquals("files/sounds/hit.wav", ResourceKind.pathOf(ResourceKind.SOUND, "hit", "wav"))
        assertEquals("files/music/theme.ogg", ResourceKind.pathOf(ResourceKind.MUSIC, "theme", "ogg"))
        assertEquals(ResourceKind.MUSIC, ResourceKind.ofExtension("MP3"))
        assertNull(ResourceKind.ofExtension("gif"))
        assertEquals("files/sounds/bounce.wav", ResourceKind.renamed("files/sounds/hit.wav", "bounce"))
        assertEquals("hit", ResourceKind.nameOf("files/sounds/hit.wav"))
    }

    @Test
    fun theContentIsToldByItsSignature() {
        fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }
        fun text(value: String) = value.toByteArray(Charsets.ISO_8859_1)
        assertTrue(ResourceKind.isContentOf(bytes(0x89, 'P'.code, 'N'.code, 'G'.code, 13, 10), "PNG"))
        assertTrue(ResourceKind.isContentOf(bytes(0xFF, 0xD8, 0xFF, 0xE0), "jpg"))
        assertTrue(ResourceKind.isContentOf(text("RIFF\u0000\u0000\u0000\u0000WEBPVP8 "), "webp"))
        assertTrue(ResourceKind.isContentOf(text("RIFF\u0000\u0000\u0000\u0000WAVEfmt "), "wav"))
        assertTrue(ResourceKind.isContentOf(text("ID3\u0004"), "mp3"))
        assertTrue(ResourceKind.isContentOf(bytes(0xFF, 0xFB, 0x90), "mp3"))
        assertTrue(ResourceKind.isContentOf(text("OggS\u0000"), "ogg"))
        assertFalse(ResourceKind.isContentOf(text("RIFF\u0000\u0000\u0000\u0000WAVEfmt "), "webp"))
        assertFalse(ResourceKind.isContentOf(text("hello"), "png"))
        assertFalse(ResourceKind.isContentOf(ByteArray(0), "wav"))
        assertFalse(ResourceKind.isContentOf(bytes(0x89, 'P'.code), "png"))
    }
}
