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

import com.pandulapeter.kubrikoCreator.actors.store.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.store.EdgeValuesMigration
import com.pandulapeter.kubrikoCreator.actors.store.KotlinBlockImportsMigration
import com.pandulapeter.kubrikoCreator.actors.store.ManagerFieldMigration
import com.pandulapeter.kubrikoCreator.actors.store.NumberValuesMigration
import com.pandulapeter.kubrikoCreator.actors.store.ProjectSettingsReader
import com.pandulapeter.kubrikoCreator.actors.store.ReservedPropertyMigration
import com.pandulapeter.kubrikoCreator.actors.store.SceneLeavingMigration
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * The format version of a project's Kubriko Creator files: one number in `.kubriko/project.json` for all of them (the
 * type definitions and the Scenes). It is raised by any change to what Kubriko Creator persists in the `.kubriko` type
 * files: a new or renamed field, a new Block, Event, Condition, or Property kind, a new enum value, a changed default, or a
 * new meaning for an existing field; any change an older Kubriko Creator would read and then handle wrongly. A raise that
 * needs no rewrite is an empty [FormatMigration] in [MIGRATIONS] whose KDoc names what it added: an older Kubriko Creator
 * then opens the project read-only, naming the version it needs, instead of dropping what it doesn't know. Type files are
 * still read ignoring unknown keys, so a newer Kubriko Creator reads older ones. Projects from before it was recorded have
 * version 0.
 *
 * Next to it, `minimumCreatorVersion` names the Kubriko Creator that raised it, so that an older one can say which
 * version to open the project in.
 *
 * @param creatorVersion This Kubriko Creator's version, recorded with the format version it raises a project to.
 * @param migrations The changes between format versions, the first one bringing version 0 to 1: their count is the
 *   format version this Kubriko Creator writes.
 */
public class ProjectFormat(
    private val rootDirectory: File,
    private val creatorVersion: String,
    private val migrations: List<FormatMigration> = MIGRATIONS,
) {
    private val settingsFile get() = File(rootDirectory, BlockTypeStore.SETTINGS_FILE)

    /**
     * Brings an older project up to this Kubriko Creator's format version, recording the version after each migration, so
     * that one that fails halfway is carried on from where it stopped next time.
     *
     * @return Whether this Kubriko Creator may change the project. One that hasn't opted in (it has no
     *   `.kubriko/project.json`) counts as writable.
     * @throws java.io.IOException If a migration couldn't write a file, or an [UnreadableProjectFileException] if
     *   `.kubriko/project.json` can't be read or holds no JSON object.
     */
    public fun prepare(): ProjectFormatStatus {
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

    private fun readSettings() = settingsFile.takeIf { it.isFile }?.let { file -> ProjectSettingsReader.parse(file, json) }

    private fun recordVersion(version: Int) {
        val settings = readSettings() ?: return
        val recorded = settings + mapOf(FORMAT_VERSION_KEY to JsonPrimitive(version), CREATOR_VERSION_KEY to JsonPrimitive(creatorVersion))
        settingsFile.writeTextAtomically(json.encodeToString(JsonObject.serializer(), JsonObject(recorded)) + "\n")
    }

    public companion object {
        private const val FORMAT_VERSION_KEY = "formatVersion"
        private const val CREATOR_VERSION_KEY = "minimumCreatorVersion"

        /**
         * Every change to the format so far, oldest first.
         */
        internal val MIGRATIONS = listOf(
            ManagerFieldMigration,
            KotlinBlockImportsMigration,
            ReservedPropertyMigration.LOOK,
            ReservedPropertyMigration.ANIMATION,
            NumberValuesMigration,
            EdgeValuesMigration,
            SceneLeavingMigration,
        )

        /**
         * The format version this Kubriko Creator reads and writes.
         */
        public val CURRENT_VERSION: Int = MIGRATIONS.size

        private val json = Json { prettyPrint = true }
    }
}
