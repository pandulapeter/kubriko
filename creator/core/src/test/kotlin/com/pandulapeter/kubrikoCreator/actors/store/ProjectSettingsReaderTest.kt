/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.actors.store

import com.pandulapeter.kubrikoCreator.project.ProjectProblem
import com.pandulapeter.kubrikoCreator.project.UnreadableProjectFileException
import com.pandulapeter.kubrikoCreator.testing.testDirectory
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class ProjectSettingsReaderTest {
    private val root = testDirectory()
    private val settingsFile = File(root, ".kubriko/project.json")

    private fun failureOf(settings: String): UnreadableProjectFileException {
        settingsFile.parentFile.mkdirs()
        settingsFile.writeText(settings)
        return assertFailsWith<UnreadableProjectFileException> { BlockTypeStore.open(root) }
    }

    @Test
    fun validSettingsOpenTheStore() {
        settingsFile.parentFile.mkdirs()
        settingsFile.writeText("{\r\n  \"packageName\": \"com.example.game\",\r\n  \"unknown\": true\r\n}\r\n")
        assertEquals("src/commonMain/kotlin/com/example/game/generated", assertNotNull(BlockTypeStore.open(root)).generatedPath)
    }

    @Test
    fun invalidJsonNamesTheFileAndTheLineOfTheMistake() {
        val exception = failureOf("{\n  \"packageName\": \"com.example.game\"\n  \"sourceDirectory\": \"src\"\n}\n")
        assertEquals(ProjectProblem.PROJECT_FILE_NOT_JSON, exception.reason)
        assertEquals(listOf(settingsFile.path, "3"), exception.arguments)
    }

    @Test
    fun anEmptyFileIsNamedAsSuch() {
        listOf("", " \n").forEach { text ->
            val exception = failureOf(text)
            assertEquals(ProjectProblem.PROJECT_FILE_EMPTY, exception.reason)
            assertEquals(listOf(settingsFile.path), exception.arguments)
        }
    }

    @Test
    fun jsonThatIsNotAnObjectIsNamed() {
        val exception = failureOf("[\"com.example.game\"]")
        assertEquals(ProjectProblem.PROJECT_FILE_NOT_AN_OBJECT, exception.reason)
        assertEquals(listOf(settingsFile.path), exception.arguments)
    }

    @Test
    fun aMissingRequiredValueIsNamed() {
        val exception = failureOf("""{ "sourceDirectory": "src" }""")
        assertEquals(ProjectProblem.PROJECT_FILE_VALUE_MISSING, exception.reason)
        assertEquals(listOf(settingsFile.path, "packageName"), exception.arguments)
    }

    @Test
    fun aRequiredValueThatIsNullIsOfTheWrongKind() {
        assertEquals(ProjectProblem.PROJECT_FILE_VALUE_NOT_TEXT, failureOf("""{ "packageName": null }""").reason)
        assertEquals(ProjectProblem.PROJECT_FILE_VALUE_NOT_NUMBER, failureOf("""{ "packageName": "com.example.game", "formatVersion": null }""").reason)
    }

    @Test
    fun aWholeNumberBetweenQuotationMarksIsReadAsTheDecoderReadsIt() {
        settingsFile.parentFile.mkdirs()
        settingsFile.writeText("""{ "packageName": "com.example.game", "templateVersion": "2" }""")
        assertNotNull(BlockTypeStore.open(root))
    }

    @Test
    fun aWholeNumberTooLargeIsNamedAsSuch() {
        listOf("3000000000", "-3000000000").forEach { value ->
            val exception = failureOf("""{ "packageName": "com.example.game", "formatVersion": $value }""")
            assertEquals(ProjectProblem.PROJECT_FILE_VALUE_TOO_LARGE, exception.reason, value)
            assertEquals(listOf(settingsFile.path, "formatVersion"), exception.arguments)
        }
    }

    @Test
    fun aParserMessageNamingNoPlaceGivesNoLine() {
        assertEquals(null, ProjectSettingsReader.lineOfMistake("{\n}", SerializationException("Broken.")))
        assertEquals(null, ProjectSettingsReader.lineOfMistake("{}", SerializationException("Unexpected JSON token at offset 40: x")))
        assertEquals(2, ProjectSettingsReader.lineOfMistake("{\n}", SerializationException("Unexpected JSON token at offset 2: x")))
    }

    @Test
    fun aNullableValueMayBeNull() {
        settingsFile.parentFile.mkdirs()
        settingsFile.writeText("""{ "packageName": "com.example.game", "minimumCreatorVersion": null }""")
        assertNotNull(BlockTypeStore.open(root))
    }

    @Test
    fun aTextValueThatIsNotTextIsNamed() {
        val exception = failureOf("""{ "packageName": 12 }""")
        assertEquals(ProjectProblem.PROJECT_FILE_VALUE_NOT_TEXT, exception.reason)
        assertEquals(listOf(settingsFile.path, "packageName"), exception.arguments)
    }

    @Test
    fun aNumberThatIsNotAWholeNumberIsNamed() {
        listOf("\"three\"", "3.5", "{}", "true").forEach { value ->
            val exception = failureOf("""{ "packageName": "com.example.game", "templateVersion": $value }""")
            assertEquals(ProjectProblem.PROJECT_FILE_VALUE_NOT_NUMBER, exception.reason, value)
            assertEquals(listOf(settingsFile.path, "templateVersion"), exception.arguments)
        }
    }

    @Test
    fun theMessageIsTheEnglishTextWithItsArguments() {
        val exception = failureOf("""{ "packageName": 12 }""")
        assertEquals("\"packageName\" in ${settingsFile.path} should be a text between quotation marks.", exception.message)
    }

    @Test
    fun aFileThatCantBeReadIsNamed() {
        settingsFile.mkdirs()
        File(settingsFile, "inside").writeText("")
        val exception = assertFailsWith<UnreadableProjectFileException> { ProjectSettingsReader.read(settingsFile, Json) }
        assertEquals(ProjectProblem.PROJECT_FILE_NOT_READ, exception.reason)
        assertEquals(settingsFile.path, exception.arguments.first())
        assertFalse(settingsFile.path in exception.arguments[1], "the path isn't named twice: ${exception.arguments[1]}")
    }
}
