/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.ui

import com.pandulapeter.kubrikoCreator.actors.NameProblem
import com.pandulapeter.kubrikoCreator.commands.CommandProblem
import com.pandulapeter.kubrikoCreator.commands.Rejection
import com.pandulapeter.kubrikoCreator.foundation.DomainText
import com.pandulapeter.kubrikoCreator.git.GitStatus
import com.pandulapeter.kubrikoCreator.project.IdentityProblem
import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.scenes.SceneProblem
import com.pandulapeter.kubrikoCreator.scenes.SceneStatus
import com.pandulapeter.kubrikoCreator.template.TemplateProblem
import com.pandulapeter.kubrikoCreator.ui.components.FieldError
import com.pandulapeter.kubrikoCreator.workspace.WorkspaceStatus
import kubriko.creator.generated.resources.Res
import kubriko.creator.generated.resources.error_folder_exists
import java.io.File
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomainTextsTest {
    private val enums = listOf<Pair<KClass<*>, List<DomainText>>>(
        NameProblem::class to NameProblem.entries,
        IdentityProblem::class to IdentityProblem.entries,
        ProjectProblem::class to ProjectProblem.entries,
        TemplateProblem::class to TemplateProblem.entries,
        SceneProblem::class to SceneProblem.entries,
        SceneStatus::class to SceneStatus.entries,
        GitStatus::class to GitStatus.entries,
        CommandProblem::class to CommandProblem.entries,
        WorkspaceStatus::class to WorkspaceStatus.entries,
    )
    private val entries = enums.flatMap { it.second }

    @Test
    fun everyDomainTextHasAString() {
        entries.forEach { it.resource }
    }

    @Test
    fun everyDomainTextEnumIsListed() {
        val sources = File("src/main/kotlin")
        assertTrue(sources.isDirectory)
        val declared = sources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file -> Regex("""enum class (\w+)\([^)]*\)\s*:\s*DomainText\b""").findAll(file.readText()).map { it.groupValues[1] } }
            .toSet()
        assertTrue(declared.isNotEmpty())
        assertEquals(declared, enums.map { it.first.simpleName }.toSet())
    }

    @Test
    fun englishHasTheStringsPlaceholders() {
        val strings = Regex("""<string name="([a-z0-9_]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(File("src/main/composeResources/values/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }
        entries.forEach { entry ->
            val string = strings[entry.resource.key]
            assertTrue(string != null, "${entry::class.simpleName}.$entry has no string called ${entry.resource.key}")
            assertEquals(placeholdersOf(string), placeholdersOf(entry.english), "${entry::class.simpleName}.$entry")
        }
    }

    @Test
    fun aRejectionShownUnderAFieldKeepsItsArguments() {
        assertEquals(FieldError(Res.string.error_folder_exists, listOf("enemies")), Rejection(CommandProblem.FOLDER_EXISTS, "enemies").toFieldError())
    }

    private fun placeholdersOf(text: String) = Regex("""%\d+\$[a-z]""").findAll(text).map { it.value }.toSet()
}
