/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.template

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NewProjectFormTest {
    private val parentDirectory = File("/home/me")
    private val emptyForm = NewProjectForm(parentDirectory = parentDirectory)

    @Test
    fun anEmptyFormShowsNoErrorsButCantBeSubmitted() {
        assertNull(emptyForm.nameError)
        assertNull(emptyForm.packageNameError)
        assertNull(emptyForm.request)
    }

    @Test
    fun thePackageFollowsTheNameUntilItIsEdited() {
        val named = emptyForm.withName("My Game")
        assertEquals("com.example.mygame", named.packageName)
        val edited = named.withPackageName("io.github.me.game")
        assertEquals("io.github.me.game", edited.withName("Another Game").packageName)
    }

    @Test
    fun aValidFormBecomesARequestWithATrimmedName() {
        assertEquals(
            NewProjectRequest(name = "My Game", packageName = "com.example.mygame", parentDirectory = parentDirectory),
            emptyForm.withName(" My Game ").request,
        )
    }

    @Test
    fun invalidFieldsShowTheirErrorsAndBlockTheRequest() {
        val invalidName = emptyForm.withName("2048")
        assertEquals(TemplateProblem.NAME_INVALID, invalidName.nameError)
        assertNull(invalidName.request)
        val invalidPackage = emptyForm.withName("Game").withPackageName("Game")
        assertEquals(TemplateProblem.PACKAGE_NAME_INVALID, invalidPackage.packageNameError)
        assertNull(invalidPackage.request)
    }

    @Test
    fun versionControlIsOnUnlessTurnedOff() {
        assertEquals(true, emptyForm.withName("Game").request?.isUnderVersionControl)
        assertEquals(false, emptyForm.withName("Game").copy(isUnderVersionControl = false).request?.isUnderVersionControl)
    }
}
