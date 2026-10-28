/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
@file:OptIn(EditorMutation::class)

package com.pandulapeter.kubrikoCreator.scenes

import com.pandulapeter.kubrikoCreator.commands.EditorMutation
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneStoreTest {
    private val root = createTempDirectory().toFile()
    private val levels = File(root, "game/src/commonMain/composeResources/files/scenes")
    private var scanned = emptyList<ProjectResource>()
    private val store = SceneStore(root) { scanned }

    @AfterTest
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun scan(vararg files: File) {
        scanned = files.map { ProjectResource(ResourceGroup.SCENES, it, it.relativeTo(root).invariantSeparatorsPath) }
    }

    @Test
    fun newScenesGoWhereTheTemplatesKeepThemInAProjectWithoutScenes() {
        store.write("main", "[]")
        assertTrue(File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/main.json").isFile)
        assertEquals(listOf("main"), store.names())
    }

    @Test
    fun newScenesGoNextToTheExistingOnesAndAreFoundBeforeTheNextScan() {
        levels.mkdirs()
        val main = File(levels, "main.json").apply { writeText("[]") }
        scan(main)
        store.write("boss", "[{}]")
        assertEquals("[{}]", File(levels, "boss.json").readText())
        assertEquals(listOf("boss", "main"), store.names())
        assertEquals("[{}]", store.read("boss"))
    }

    @Test
    fun deletedScenesAreGoneEvenBeforeTheNextScan() {
        levels.mkdirs()
        val main = File(levels, "main.json").apply { writeText("[]") }
        scan(main)
        store.delete("main")
        assertFalse(main.exists())
        assertFalse(store.exists("main"))
        assertNull(store.read("main"))
        assertEquals(emptyList(), store.names())
    }

    @Test
    fun renamingATypeIdRewritesOnlyTheScenesThatUseIt() {
        store.write("one", """[{"typeId":"Ball","state":"{}"},{"typeId":"Wall","state":"{}"}]""")
        store.write("two", """[{"typeId":"Wall","state":"{}"}]""")
        store.write("broken", "nope")
        assertEquals(listOf("one"), store.renameTypeId("Ball", "Puck"))
        // The Scene it changes keeps its Actors' IDs from then on, one Actor per line; the one it doesn't change stays as it was.
        assertEquals("[\n" + """{"typeId":"Puck","state":"{}","id":"i0"},""" + "\n" + """{"typeId":"Wall","state":"{}","id":"i1"}""" + "\n]\n", store.read("one"))
        assertEquals("""[{"typeId":"Wall","state":"{}"}]""", store.read("two"))
        assertEquals("nope", store.read("broken"))
    }
}
