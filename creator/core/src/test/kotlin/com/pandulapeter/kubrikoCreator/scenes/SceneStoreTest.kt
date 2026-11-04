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

import com.pandulapeter.kubrikoCreator.foundation.EditorMutation
import com.pandulapeter.kubrikoCreator.foundation.writeTextAtomically
import com.pandulapeter.kubrikoCreator.project.ProjectResource
import com.pandulapeter.kubrikoCreator.project.ResourceGroup
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assume.assumeFalse
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
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

    @Test
    fun writeAllPutsBackWhatItWroteWhenOneSceneFails() {
        val failing = SceneStore(root, writeText = { file, text -> if (file.name == "b.json") throw IOException("Locked.") else file.writeTextAtomically(text) }) { scanned }
        levels.mkdirs()
        val files = listOf("a", "b", "c").map { name -> File(levels, "$name.json").apply { writeText(SceneDocument.format(listOf(SceneActorEntry("Ball", "{}", name)))) } }
        scan(*files.toTypedArray())
        val originals = files.map { it.readBytes().toList() }
        val actors = failing.actors("a")
        assertFailsWith<IOException> {
            failing.writeAll(listOf("a", "b", "c").associateWith { name -> listOf(SceneActorEntry("Puck", "{}", name)) })
        }
        assertEquals(originals, files.map { it.readBytes().toList() })
        assertEquals(actors, failing.actors("a"))
    }

    @Test
    fun mutationsRecordTheirChanges() {
        store.write("main", "[]")
        store.write("main", SceneDocument.format(listOf(SceneActorEntry("Ball", "{}", "ball"))))
        store.move("main", "levels/first")
        store.createFolder("empty")
        store.changeStates(setOf("Ball")) { JsonObject(it + ("speed" to JsonPrimitive(2))) }
        store.delete("levels/first")
        store.deleteFolder("empty")
        assertEquals(
            listOf(
                SceneChange.Written("main", isNew = true),
                SceneChange.Written("main", isNew = false),
                SceneChange.Moved("main", "levels/first"),
                SceneChange.FoldersChanged,
                SceneChange.Written("levels/first", isNew = false),
                SceneChange.Deleted("levels/first"),
                SceneChange.FoldersChanged,
            ),
            store.takeChanges(),
        )
        assertEquals(emptyList(), store.takeChanges())
    }

    @Test
    fun aFailingWriteAllRecordsNothing() {
        val failing = SceneStore(root, writeText = { file, text -> if (file.name == "b.json") throw IOException("Locked.") else file.writeTextAtomically(text) }) { scanned }
        failing.write("a", "[]")
        failing.takeChanges()
        assertFailsWith<IOException> {
            failing.writeAll(listOf("a", "b").associateWith { name -> listOf(SceneActorEntry("Puck", "{}", name)) })
        }
        assertEquals(emptyList(), failing.takeChanges())
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
    fun aSceneOfAnotherModuleStaysInItsOwnScenesFolderWhenRenamed() {
        levels.mkdirs()
        val main = File(levels, "main.json").apply { writeText("[]") }
        val tools = File(root, "tools/src/commonMain/composeResources/files/scenes/sandbox.json").apply { parentFile.mkdirs(); writeText("[]") }
        scan(main, tools)
        assertTrue(store.move("sandbox", "tests/sandbox"))
        assertTrue(File(root, "tools/src/commonMain/composeResources/files/scenes/tests/sandbox.json").isFile)
        assertFalse(File(levels, "tests/sandbox.json").exists())
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

    @Test
    fun aSceneThatCantBeReadSaysWhy() {
        store.write("main", SceneDocument.format(listOf(SceneActorEntry("Ball", "{}", "a"))))
        assertNull(store.problemOf("main"))
        store.write("main", SceneDocument.format(listOf(SceneActorEntry("Ball", "{}", "a"), SceneActorEntry("Ball", "[1]", "b"))))
        assertEquals(SceneFileProblem.StateNotAnObject("b"), store.problemOf("main"))
        assertFalse(store.isValid("main"))
        store.write("main", "not a Scene")
        assertTrue(assertIs<SceneFileProblem.Unparsable>(store.problemOf("main")).reason.isNotEmpty())
        assertFalse("\n" in (store.problemOf("main") as SceneFileProblem.Unparsable).reason)
        assertNull(store.problemOf("missing"))
    }

    @Test
    fun aSceneFileThatCantBeReadCountsAsNoScene() {
        store.write("main", "[]")
        val file = File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/main.json")
        file.setLastModified(file.lastModified() + 2_000)
        file.setReadable(false)
        assumeFalse(file.canRead())
        assertNull(store.read("main"))
        assertFalse(store.isValid("main"))
    }

    @Test
    fun namesComeFromTheIndexAfterARefresh() {
        val scenes = File(root, SceneStore.DEFAULT_SCENE_DIRECTORY).apply { mkdirs() }
        store.refresh()
        File(scenes, "hand.json").writeText("[]")
        assertEquals(emptyList(), store.names())
        assertFalse(store.exists("hand"))
        store.write("b", "[]")
        assertEquals(listOf("b"), store.names())
        assertTrue(store.move("b", "c"))
        assertEquals(listOf("c"), store.names())
        store.delete("c")
        assertEquals(emptyList(), store.names())
        store.refresh()
        assertEquals(listOf("hand"), store.names())
        assertTrue(store.exists("hand"))
    }

    @Test
    fun changesReachScenesTheIndexDoesntKnowYet() {
        store.refresh()
        val file = File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/hand.json").apply { parentFile.mkdirs() }
        file.writeText(SceneDocument.format(listOf(SceneActorEntry("Ball", "{}", "a"))))
        assertEquals(listOf("hand"), store.renameTypeId("Ball", "Puck"))
        assertEquals(listOf("Puck"), SceneDocument.parse(file.readText()).map { it.typeId })
    }

    @Test
    fun aRefreshKeepsANameWrittenWhileItWalked() {
        lateinit var walking: SceneStore
        walking = SceneStore(root, beforeIndexing = { if (!walking.exists("b")) walking.write("b", "[]") }) { scanned }
        walking.refresh()
        assertEquals(listOf("b"), walking.names())
    }

    @Test
    fun namesBeforeTheFirstRefreshWalkTheFolder() {
        File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/main.json").apply { parentFile.mkdirs() }.writeText("[]")
        assertEquals(listOf("main"), store.names())
        assertTrue(store.exists("main"))
    }

    @Test
    fun refreshReadsChangedScenesIntoTheCache() {
        var readCount = 0
        val counting = SceneStore(root, readText = { file -> readCount++; file.readText() }) { scanned }
        val file = File(root, "${SceneStore.DEFAULT_SCENE_DIRECTORY}/main.json").apply { parentFile.mkdirs() }
        file.writeText(SceneDocument.format(listOf(SceneActorEntry("Ball", "{}", "a"))))
        counting.refresh()
        val readsAfterTheFirstRefresh = readCount
        file.writeText(SceneDocument.format(listOf(SceneActorEntry("Ball", "{}", "a"), SceneActorEntry("Wall", "{}", "b"))))
        file.setLastModified(file.lastModified() + 2_000)
        counting.refresh()
        assertEquals(readsAfterTheFirstRefresh + 1, readCount)
        assertEquals(listOf("a", "b"), counting.actors("main")?.map { it.id })
        assertEquals(readsAfterTheFirstRefresh + 1, readCount)
    }
}
