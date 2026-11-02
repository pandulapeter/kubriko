<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Start a shared `testing` package for Kubriko Creator's tests with `testDirectory()` and `FakeGitRepository`, and run one contract test against each fake and its real implementation so the fakes behave like what they replace

**Challenged:** amended — the fake also misses the parents of a nested created folder (`mkdirs` lists them); and deleting a nested folder keeps its parents on disk; added both to its fix and two contract tests.

**Kind:** tests  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** TST-6, TST-7  ·  **Depends on:** none
**Files:** (T = `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator`)
- New: `T/testing/TestDirectories.kt`, `T/testing/FakeGitRepository.kt` (moved), `T/testing/BlockTypeRepositoryContract.kt`, `T/testing/GitRepositoryContract.kt`, `T/testing/FakeBlockTypeRepositoryContractTest.kt`, `T/testing/FakeGitRepositoryContractTest.kt`, `T/actors/BlockTypeStoreContractTest.kt`, `T/git/JGitRepositoryContractTest.kt`
- Deleted (moved): `T/git/FakeGitRepository.kt`
- Fixed in place (not moved, see the overlap rule): `T/workspace/FakeBlockTypeRepository.kt`
- Rewritten, imports only: `T/commands/CommandTesting.kt` (also loses `testDirectory()` and its queue), `T/commands/ReadCommandsTest.kt`, `T/commands/git/GitCommandsTest.kt`, `T/commands/project/ProjectUpgradeCommandsTest.kt`, `T/commands/scenes/SceneRenamingCommandsTest.kt`, `T/git/GitControllerTest.kt`
- Possibly rewritten, expectations only, when the fixed fake shows a test relied on its divergence: `T/commands/folders/FolderCommandsTest.kt`, `T/commands/actorTypes/ActorTypeCommandsTest.kt`, `T/workspace/BlockTypesControllerTest.kt`
- Docs: `creator/CLAUDE.md` (Running and testing → the fakes bullet; Definition of done → shared building blocks)

## Problem

Kubriko Creator's tests have no shared fixture layer. The fakes are spread across the packages of the code they stand in for: `T/workspace/{FakeBlockTypeRepository,FakeProjectBuild,FakeSessionChannel}.kt`, `T/commands/FakeProjectRunner.kt`, `T/git/FakeGitRepository.kt`, `T/mcp/clients/FakeProcessRunner.kt`, and `T/ai/assistant/Fake*.kt`. The one temporary-directory helper lives in `T/commands/CommandTesting.kt`:

```kotlin
private val testDirectories = ConcurrentLinkedQueue<File>().also { directories ->
    Runtime.getRuntime().addShutdownHook(Thread { directories.forEach(File::deleteRecursively) })
}

/**
 * A new temporary directory, deleted when the tests' JVM exits.
 */
internal fun testDirectory(): File = createTempDirectory("kubriko-creator-test").toFile().also(testDirectories::add)
```

so a test outside `commands` that wants it has to import it from the commands package.

Nothing checks that a fake behaves like the real implementation (TST-7). `FakeBlockTypeRepository` deletes a folder whatever is in it, and lists only the folders that were created explicitly:

```kotlin
val folders = mutableMapOf<BlockTypeKind, MutableSet<String>>()

override fun folders(kind: BlockTypeKind): Set<String> = folders[kind].orEmpty()
...
override fun deleteFolder(kind: BlockTypeKind, path: String) {
    writes += "delete folder $path"
    folders[kind]?.removeAll { it == path || it.startsWith("$path/") }
}
```

The real `BlockTypeStore` (`M/actors/BlockTypeStore.kt`, M = `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator`) reads the folders from disk, so a type saved into `toys/round` makes both `toys` and `toys/round` appear, and the folder stays after the type is deleted. It also refuses to delete a folder that still holds a type:

```kotlin
override fun folders(kind: BlockTypeKind): Set<String> = definitionsDirectory(kind).walkTopDown()
    .filter { it.isDirectory && it != definitionsDirectory(kind) }
    .mapTo(sortedSetOf()) { it.relativeTo(definitionsDirectory(kind)).invariantSeparatorsPath }
...
override fun deleteFolder(kind: BlockTypeKind, path: String) {
    val folder = File(definitionsDirectory(kind), path)
    if (folder.walkTopDown().none { it.isFile }) folder.deleteRecursively()
}
```

The interface (`M/actors/BlockTypeRepository.kt`) promises the real behaviour: "Every folder that holds types of [kind], or held them, as paths (`enemies/flying`), parents included" and "Deletes the folder at [path] if nothing is in it any more." A controller or command test that passes against the fake can therefore fail against a real project. For example, deleting a non-empty folder looks like it worked. `FakeGitRepository` (`T/git/FakeGitRepository.kt`) and `JGitRepository` (`M/git/JGitRepository.kt`) are never compared either.

**Overlap rule for this plan:** it runs in phase 1 alongside lanes S, A, and G. So it moves only the fixtures whose callers are outside those lanes' folders (`scenes/`, `session/`, `ai/`, `mcp/`, `ui/assistant/`, `ui/mcp/`, `actors/`, `template/`), and only adds new files there. Several fixtures are left where they are on purpose:
- `FakeBlockTypeRepository`, `FakeProjectBuild`, and `FakeSessionChannel`: T03 moves them once lane S has merged. `FakeBlockTypeRepository` is fixed in place here, and moving it would add an import to `workspace/WorkspaceControllerTest.kt`, which S03 and S04 edit. S04 also edits `FakeSessionChannel`.
- `FakeProcessRunner`: T02b moves it.
- `ai/assistant/Fake*`: lane A edits them in A01.
- `FakeProjectRunner`, `testScope()`, `commandContext()`, `blockTypesController()`, `gitController()`, and `assertRoundTrip()`: they are imported by `mcp/` and `ai/` tests, and they are command-test helpers. The modularization lane (java-test-fixtures) moves them.
- `context/BreakoutFixture.kt`: it is used by `ai/assistant/AssistantLiveScenariosTest.kt`.

## Fix

1. Create the package `com.pandulapeter.kubrikoCreator.testing` in `T/testing/`. Every file gets the MPL header, and every declaration is `internal`.
   - `TestDirectories.kt`: move `testDirectory()`, its KDoc, and the private `testDirectories` queue with its shutdown hook verbatim from `commands/CommandTesting.kt`. `CommandTesting.kt` keeps using it through `import com.pandulapeter.kubrikoCreator.testing.testDirectory`, including as the default of `commandContext(rootDirectory = testDirectory())`.
   - `FakeGitRepository.kt`: move the class and change only its `package` line.
2. Update the imports in the files listed as rewritten under **Files**. `git/GitControllerTest.kt`, which is in the fake's old package, gains an import. The others change theirs, and the two `commands` subpackage tests change `com.pandulapeter.kubrikoCreator.commands.testDirectory` to `com.pandulapeter.kubrikoCreator.testing.testDirectory`. Afterwards `grep -rn "git.FakeGitRepository\|commands.testDirectory" creator/src/test` must print nothing. Run `./gradlew spotlessApply` (ktlint orders imports lexicographically).
3. Contract tests. These are abstract classes in `testing/` whose `@Test` functions JUnit 4 runs in every concrete subclass:
   - `BlockTypeRepositoryContract` has `protected abstract fun repository(): BlockTypeRepository`, which returns an empty repository. Its subclasses:
     - `actors/BlockTypeStoreContractTest`: a `testDirectory()` with `.kubriko/project.json` = `{ "packageName": "com.example.game" }`, then `BlockTypeStore.open(root)!!`.
     - `testing/FakeBlockTypeRepositoryContractTest`: `FakeBlockTypeRepository()`.

     The tests use `ActorTypeDefinition(name = "Ball")`, `ActorTypeDefinition(name = "Wall")`, and `ManagerTypeDefinition(name = "Rules")` (check the constructor's required parameters at HEAD), with `withFolder(...)`. They call only `load`, `save`, `delete`, `folders`, `createFolder`, and `deleteFolder`, never `regenerate`, whose results the fake makes up.
   - `GitRepositoryContract` has `protected abstract fun repository(): GitRepository`, which returns a repository on `main` holding one commit, `"Created."`, of the files `kept.txt`, `changed.txt`, and `deleted.txt`. It also has `protected abstract fun change(repository: GitRepository, path: String, kind: ChangeKind)`, which makes `path` show as `kind`. Its subclasses:
     - `git/JGitRepositoryContractTest`: writes the three files into a `testDirectory()`, then `JGitRepository.create(root, "Created.")`. `change` writes a new file for `ADDED`, rewrites the file for `MODIFIED`, and deletes it for `DELETED`.
     - `testing/FakeGitRepositoryContractTest`: `FakeGitRepository().apply { commitAll("Created.") }`. `change` adds `FileChange(path, kind)` to `status.changes`, keeping them sorted by path.
   - Run both contracts. Whatever fails only for the fake is a divergence: fix the fake, never the contract. The expected fix for `FakeBlockTypeRepository`:
     - `folders(kind)` returns every explicit folder in `folders[kind]` and every folder of the stored definitions of that kind, each with its parents (`"a/b"` gives `"a"` and `"a/b"`; `""` gives nothing), since the real `createFolder` is a `mkdirs`.
     - `delete(name)` first adds the deleted type's folder and its parents to the explicit set, because the folder stays on disk.
     - `save` of a type that moved does the same for its old folder.
     - `deleteFolder` first adds the parents of `path` to the explicit set (they stay on disk), then removes `path` and its descendants from it only when no stored definition of `kind` has `folder == path` or a folder starting with `"$path/"`. It keeps logging `"delete folder $path"` either way, since the real call happens either way.

     Keep the public `folders` map, which tests assign directly (`folders[BlockTypeKind.ACTOR] = mutableSetOf("empty")`), and keep `writes`, `isFailing`, `loadFailure`, and the other failure switches, which go beyond the contract.
   - If an existing test fails after the fake is fixed, it relied on the divergence: change its expectation to the real store's behaviour and name it in the commit message. `commands/folders/FolderCommandsTest.kt` and `workspace/BlockTypesControllerTest.kt` are the likely ones. Do not change production code in this plan. If the contract shows that `BlockTypeStore` or `JGitRepository` contradicts its own interface KDoc, stop and report it instead.
4. `creator/CLAUDE.md`:
   - In the "Controllers are tested against fakes (…)" bullet, add: "shared fixtures live in `testing/` (`testDirectory()`, the fakes several packages use); a fake of an interface with a real implementation runs the same contract test (`testing/*Contract`) as the real one."
   - In Definition of done, change "in command tests `commands/CommandTesting.kt` (`commandContext`, `assertRoundTrip`)" to also name `testing/` (`testDirectory()`, the fakes and their contracts).

## Tests

`BlockTypeRepositoryContract` (each test runs for both implementations):
- `savedTypesLoadBackSortedByNameWithTheirFolders`: save Wall in `""` and Ball in `toys`. `load().types == listOf(ball.withFolder("toys"), wall)` and `unreadableFiles` is empty.
- `savingATypeIntoAnotherFolderMovesIt`: save Ball in `toys/round`, then in `toys`. `load().types == listOf(ball.withFolder("toys"))`.
- `aSavedTypesFolderAndItsParentsAreListedForItsKindOnly`: save Ball in `toys/round`. `folders(ACTOR) == setOf("toys", "toys/round")` and `folders(MANAGER)` is empty. **Fails for the fake before the fix.**
- `createdFoldersAreListedPerKind`: `createFolder(MANAGER, "rules")`. `folders(MANAGER) == setOf("rules")` and `folders(ACTOR)` is empty.
- `aCreatedNestedFolderListsItsParents`: `createFolder(ACTOR, "toys/round")`. `folders(ACTOR) == setOf("toys", "toys/round")`. **Fails for the fake before the fix.**
- `aFolderThatHoldsATypeIsNotDeleted`: save Ball in `toys/round`, then `deleteFolder(ACTOR, "toys")`. The folders and `load().types` are unchanged. **Fails for the fake before the fix.**
- `deletingATypeKeepsItsFolderUntilTheFolderIsDeleted`: save Ball in `toys`, then `delete("Ball")`. `folders(ACTOR) == setOf("toys")`. Then `deleteFolder(ACTOR, "toys")` gives an empty set. **Fails for the fake before the fix.**
- `anEmptyFolderGoesWithItsEmptySubfoldersButNotItsNamesakeSibling`: create `toys/round` and `toys2`, then `deleteFolder(ACTOR, "toys")`. `folders(ACTOR) == setOf("toys2")`.
- `deletingANestedFolderKeepsItsParent`: create `toys/round`, then `deleteFolder(ACTOR, "toys/round")`. `folders(ACTOR) == setOf("toys")`. **Fails for the fake before the fix.**
- `deletingAnUnknownTypeChangesNothing`: save Ball, then `delete("Nobody")`. `load().types == listOf(ball)`.

`GitRepositoryContract`:
- `aNewRepositoryIsOnMainWithNothingChanged`: `status() == RepositoryStatus(branch = "main", changes = emptyList())` and `history(10).map { it.message } == listOf("Created.")`.
- `committingRecordsEveryChangeAndHistoryIsNewestFirst`: make `new.txt` ADDED, `changed.txt` MODIFIED, and `deleted.txt` DELETED, then `commitAll("Second")`. `status().changes` is empty, `history(10).map { it.message } == listOf("Second", "Created.")`, and `history(1).size == 1`.
- `commitIdsAreDistinctAndNotBlank`: after two commits, the ids are distinct and not blank.
- `discardingRemovesOnlyTheGivenPathsFromTheChanges`: make `new.txt` ADDED, `changed.txt` MODIFIED, and `deleted.txt` DELETED, then `discard(listOf("new.txt", "deleted.txt"))`. `status().changes == listOf(FileChange("changed.txt", ChangeKind.MODIFIED))`.
- `committingWithoutChangesStillRecordsACommit`: `commitAll("Empty")` right after creation. `history(10).first().message == "Empty"`. JGit's `CommitCommand` allows empty commits unless told otherwise. If `JGitRepositoryContractTest` shows it doesn't, assert what JGit does in this test, align the fake, and note it in the commit.
