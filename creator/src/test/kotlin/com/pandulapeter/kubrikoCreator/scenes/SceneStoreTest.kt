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
    fun scenesInFoldersAreNamedByTheirPathAndMoveBetweenFolders() {
        levels.mkdirs()
        val forest = File(levels, "levels/forest/1-1.json").apply { parentFile.mkdirs(); writeText("[]") }
        val main = File(levels, "main.json").apply { writeText("[]") }
        // Found through the scan, so the Scenes folder is the one above the nested Scene too.
        scan(forest, main)
        assertEquals(listOf("levels/forest/1-1", "main"), store.names())
        assertEquals("levels/forest/1-1", SceneStore.nameOf(forest))
        store.createFolder("levels/cave")
        assertEquals(setOf("levels", "levels/cave", "levels/forest"), store.folders())
        store.move("levels/forest/1-1", "levels/cave/1-1")
        assertTrue(File(levels, "levels/cave/1-1.json").isFile)
        scan(File(levels, "levels/cave/1-1.json"), main)
        store.deleteFolder("levels/forest")
        store.deleteFolder("levels/cave")
        assertEquals(setOf("levels", "levels/cave"), store.folders())
        store.write("levels/castle/boss", "[]")
        assertEquals(listOf("levels/castle/boss", "levels/cave/1-1", "main"), store.names())
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

    @Test
    fun aSceneIsParsedOnceUntilItsFileChangesElsewhere() {
        val actors = listOf(SceneActorEntry("Ball", SceneDocument.blockBuiltState(1f, 2f, 3f, 4f), "a"))
        store.write("main", actors)
        assertTrue(store.actors("main") === actors, "what was written is kept, not parsed again")
        assertTrue(store.isValid("main"))
        val file = File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/main.json")
        file.writeText(SceneDocument.format(actors + SceneActorEntry("Ball", SceneDocument.blockBuiltState(5f, 6f, 3f, 4f), "b")))
        file.setLastModified(file.lastModified() + 2_000)
        assertEquals(listOf("a", "b"), store.actors("main")?.map { it.id })
        file.writeText("not a Scene")
        file.setLastModified(file.lastModified() + 4_000)
        assertNull(store.actors("main"))
        assertFalse(store.isValid("main"))
        assertFalse(store.isValid("missing"))
    }
}
