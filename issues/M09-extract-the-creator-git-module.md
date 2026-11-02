<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's version control (`git`) into `:creator:git`, with the fake repository as its test fixture

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** Modularization proposal (git)  ·  **Depends on:** M08
**Files:**
- `settings.gradle.kts`; new `creator/git/build.gradle.kts`; `creator/build.gradle.kts`
- moved (main and test): `git/**` → `creator/git/src/{main,test}/kotlin/com/pandulapeter/kubrikoCreator/git/`
- `FakeGitRepository` (app tests' `git/` or T01's `testing` package) and T01's `GitRepositoryContract` test → `creator/git/src/{testFixtures,test}/…`
- importers of newly public declarations (visibility only); `PackageLayeringTest.kt`; `creator/CLAUDE.md` (Modules list)

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/`.

## Problem

`git/` (10 files: `GitController`, `GitRepository`, `JGitRepository`, `ChangeGrouping`, …) imports only core and foundation:

```kotlin
// git/ChangeGrouping.kt — groups a change's files by what Creator generated
import com.pandulapeter.kubrikoCreator.actors.BlockTypeStore
import com.pandulapeter.kubrikoCreator.actors.ManagerTypeCodeGenerator
```

(after M05 these are `actors.store.BlockTypeStore` and `actors.codegen.…`), plus `console.ConsoleEntry` and `foundation.EditorMutation`.
`template` (`ProjectGenerator` uses `JGitRepository`), `commands` (`CommandContext.git: GitController`, `commands/git/*`),
`workspace`, and the UI's version control panel use it.

## Fix

1. `settings.gradle.kts`: `":creator:git",`. `creator/git/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
   }

   dependencies {
       api(projects.creator.core)
       implementation(libs.jgit)
       testImplementation(testFixtures(projects.creator.foundation))
   }
   ```
   (`api(libs.jgit)` only if a public signature exposes a JGit type; `GitRepository` is Creator's own interface, so it shouldn't.)
   The app adds `implementation(projects.creator.git)` and `testImplementation(testFixtures(projects.creator.git))`, and keeps
   `libs.jgit` only while `template` (still in the app until M10) imports `org.eclipse.jgit`.
2. `git mv` the package and its tests; move `FakeGitRepository` to `creator/git/src/testFixtures/…/testing/` (`public`) and the
   contract test that runs against both it and `JGitRepository` (T01, TST-7) to `creator/git/src/test/…`.
3. Make public what the app, `commands`, and `template` use; keep JGit wiring internal.
4. `PackageLayeringTest`: the git layer's files must all be under `creator/git/src`.
5. `creator/CLAUDE.md`: add `git` to the Modules list.

## Tests

- `:creator:git:test` runs the moved tests and the contract test against real temporary repositories (JGit needs no installed git).
- `./gradlew :creator:allTests` with the same test count as before.
