/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.project

import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.ManagerFieldMigration
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * The format version of a project's Kubriko Creator files: one number in `.kubriko/project.json` for all of them (the
 * type definitions and the Scenes), raised only by a change an older Kubriko Creator can't read. Projects from before it
 * was recorded have version 0.
 *
 * Next to it, `minimumCreatorVersion` names the Kubriko Creator that raised it, so that an older one can say which
 * version to open the project in.
 *
 * @param creatorVersion This Kubriko Creator's version, recorded with the format version it raises a project to.
 * @param migrations The changes between format versions, the first one bringing version 0 to 1: their count is the
 *   format version this Kubriko Creator writes.
 */
internal class ProjectFormat(
    private val rootDirectory: File,
    private val creatorVersion: String,
    private val migrations: List<FormatMigration> = MIGRATIONS,
) {
    private val settingsFile get() = File(rootDirectory, BlockTypeStore.SETTINGS_FILE)

    /**
     * Brings an older project up to this Kubriko Creator's format version, recording the version after each migration, so
     * that one that fails halfway is carried on from where it stopped next time.
     *
     * @return Whether this Kubriko Creator may change the project. One that hasn't opted in (it has no readable
     *   `.kubriko/project.json`) counts as writable.
     * @throws java.io.IOException If a migration couldn't write a file.
     */
    fun prepare(): ProjectFormatStatus {
        val settings = readSettings() ?: return ProjectFormatStatus.Writable
        val version = settings[FORMAT_VERSION_KEY]?.jsonPrimitive?.intOrNull ?: 0
        if (version > migrations.size) {
            return ProjectFormatStatus.ReadOnly(version, settings[CREATOR_VERSION_KEY]?.jsonPrimitive?.contentOrNull)
        }
        (version until migrations.size).forEach { index ->
            migrations[index].migrate(rootDirectory)
            recordVersion(index + 1)
        }
        return ProjectFormatStatus.Writable
    }

    private fun readSettings() = settingsFile.takeIf { it.isFile }?.let { file ->
        runCatching { json.parseToJsonElement(file.readText()).jsonObject }.getOrNull()
    }

    private fun recordVersion(version: Int) {
        val settings = readSettings() ?: return
        val recorded = settings + mapOf(FORMAT_VERSION_KEY to JsonPrimitive(version), CREATOR_VERSION_KEY to JsonPrimitive(creatorVersion))
        settingsFile.writeTextAtomically(json.encodeToString(JsonObject.serializer(), JsonObject(recorded)) + "\n")
    }

    companion object {
        private const val FORMAT_VERSION_KEY = "formatVersion"
        private const val CREATOR_VERSION_KEY = "minimumCreatorVersion"

        /**
         * Every change to the format so far, oldest first.
         */
        val MIGRATIONS = listOf<FormatMigration>(ManagerFieldMigration)

        /**
         * The format version this Kubriko Creator reads and writes.
         */
        val CURRENT_VERSION = MIGRATIONS.size

        private val json = Json { prettyPrint = true }
    }
}
