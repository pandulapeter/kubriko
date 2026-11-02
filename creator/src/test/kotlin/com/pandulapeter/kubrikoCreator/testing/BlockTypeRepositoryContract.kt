/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubrikoCreator.testing

import com.pandulapeter.kubrikoCreator.actors.ActorTypeDefinition
import com.pandulapeter.kubrikoCreator.actors.BlockTypeKind
import com.pandulapeter.kubrikoCreator.actors.BlockTypeRepository
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What every [BlockTypeRepository] does, run against the real one and every fake of it.
 */
internal abstract class BlockTypeRepositoryContract {
    private val ball = ActorTypeDefinition(name = "Ball")
    private val wall = ActorTypeDefinition(name = "Wall")
    private val rules = ManagerTypeDefinition(name = "Rules")

    /**
     * A new repository without any types or folders.
     */
    protected abstract fun repository(): BlockTypeRepository

    @Test
    fun savedTypesLoadBackSortedByNameWithTheirFolders() {
        val repository = repository()
        repository.save(wall)
        repository.save(ball.withFolder("toys"))
        val loaded = repository.load()
        assertEquals(listOf(ball.withFolder("toys"), wall), loaded.types)
        assertTrue(loaded.unreadableFiles.isEmpty())
    }

    @Test
    fun savingATypeIntoAnotherFolderMovesIt() {
        val repository = repository()
        repository.save(ball.withFolder("toys/round"))
        repository.save(ball.withFolder("toys"))
        assertEquals(listOf(ball.withFolder("toys")), repository.load().types)
    }

    @Test
    fun aSavedTypesFolderAndItsParentsAreListedForItsKindOnly() {
        val repository = repository()
        repository.save(ball.withFolder("toys/round"))
        assertEquals(setOf("toys", "toys/round"), repository.folders(BlockTypeKind.ACTOR))
        assertEquals(emptySet(), repository.folders(BlockTypeKind.MANAGER))
    }

    @Test
    fun createdFoldersAreListedPerKind() {
        val repository = repository()
        repository.createFolder(BlockTypeKind.MANAGER, "rules")
        assertEquals(setOf("rules"), repository.folders(BlockTypeKind.MANAGER))
        assertEquals(emptySet(), repository.folders(BlockTypeKind.ACTOR))
    }

    @Test
    fun aCreatedNestedFolderListsItsParents() {
        val repository = repository()
        repository.createFolder(BlockTypeKind.ACTOR, "toys/round")
        assertEquals(setOf("toys", "toys/round"), repository.folders(BlockTypeKind.ACTOR))
    }

    @Test
    fun aFolderThatHoldsATypeIsNotDeleted() {
        val repository = repository()
        repository.save(ball.withFolder("toys/round"))
        repository.deleteFolder(BlockTypeKind.ACTOR, "toys")
        assertEquals(setOf("toys", "toys/round"), repository.folders(BlockTypeKind.ACTOR))
        assertEquals(listOf(ball.withFolder("toys/round")), repository.load().types)
    }

    @Test
    fun deletingATypeKeepsItsFolderUntilTheFolderIsDeleted() {
        val repository = repository()
        repository.save(ball.withFolder("toys"))
        repository.delete("Ball")
        assertEquals(setOf("toys"), repository.folders(BlockTypeKind.ACTOR))
        repository.deleteFolder(BlockTypeKind.ACTOR, "toys")
        assertEquals(emptySet(), repository.folders(BlockTypeKind.ACTOR))
    }

    @Test
    fun anEmptyFolderGoesWithItsEmptySubfoldersButNotItsNamesakeSibling() {
        val repository = repository()
        repository.createFolder(BlockTypeKind.ACTOR, "toys/round")
        repository.createFolder(BlockTypeKind.ACTOR, "toys2")
        repository.deleteFolder(BlockTypeKind.ACTOR, "toys")
        assertEquals(setOf("toys2"), repository.folders(BlockTypeKind.ACTOR))
    }

    @Test
    fun deletingANestedFolderKeepsItsParent() {
        val repository = repository()
        repository.createFolder(BlockTypeKind.ACTOR, "toys/round")
        repository.deleteFolder(BlockTypeKind.ACTOR, "toys/round")
        assertEquals(setOf("toys"), repository.folders(BlockTypeKind.ACTOR))
    }

    @Test
    fun deletingAnUnknownTypeChangesNothing() {
        val repository = repository()
        repository.save(ball)
        repository.delete("Nobody")
        assertEquals(listOf(ball), repository.load().types)
    }

    @Test
    fun aManagerTypeIsListedUnderItsOwnKind() {
        val repository = repository()
        repository.save(rules.withFolder("game"))
        assertEquals(setOf("game"), repository.folders(BlockTypeKind.MANAGER))
        assertEquals(listOf(rules.withFolder("game")), repository.load().types)
    }

    @Test
    fun aReplacingSaveTakesTheNameFromATypeOfTheOtherKind() {
        val repository = repository()
        repository.save(ManagerTypeDefinition(name = "Ball").withFolder("game"))
        repository.save(ball, isReplacing = true)
        assertEquals(listOf(ball), repository.load().types)
    }
}
