<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's commands and AI context (`commands`, `context`, with the Kubriko overview resource) into `:creator:commands`, with the command test helpers as its test fixtures

**Challenged:** amended — the command objects become public (M02a's `ui/CommandTitles.kt` and the UI's `Invocation`s name each one, and an override of a public interface member can't be internal); the test fixtures get the real `BlockTypesController` from core (M06).

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** Modularization proposal (commands + context; blocker `commands -> workspace.BlockTypesController`, removed by K09 or M06)  ·  **Depends on:** M10
**Files:**
- `settings.gradle.kts`; new `creator/commands/build.gradle.kts`; `creator/build.gradle.kts` (the `processResources` block moves)
- moved (main and test): `commands/**`, `context/**` → `creator/commands/src/{main,test}/kotlin/com/pandulapeter/kubrikoCreator/{commands,context}/`
- test fixtures: `commands/CommandTesting.kt`, `FakeProjectRunner` (app tests' `commands/` or T01's `testing`), `context/BreakoutFixture.kt` → `creator/commands/src/testFixtures/kotlin/…` (keeping their packages)
- `commands/CommandRegistryTest.kt` (walks every module's sources)
- importers of newly public declarations (visibility only); `PackageLayeringTest.kt`
- docs: `creator/CLAUDE.md` (Definition of done's `commands/CommandTesting.kt` path; Modules list)

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

`commands/` (118 files) and `context/` (21) are one layer: `context` uses `commands.CommandContext`, `ChangeRecord`, and
`CommandTarget`, and `commands` uses `context`'s digests and notations, so they can't be two modules. Below them they use core,
`git` (`CommandContext.git: GitController`, `commands/git/*`), `template` (the upgrade commands), and `foundation`. The upward
import the verifier found, `commands -> workspace.BlockTypesController`, is gone (K09's interface or M06's move); confirm with
`PackageLayeringTest` (empty `knownExceptions`) before starting.

Two things tie them to the app's build:

```kotlin
// creator/build.gradle.kts — the engine's overview, which get_kubriko_api hands the AI.
tasks.processResources {
    from(rootProject.file("CLAUDE.md")) { rename { "kubriko-overview.md" } }
}
// context/KubrikoApi.kt:20
    val overview by lazy { KubrikoApi::class.java.getResource(OVERVIEW_RESOURCE)?.readText()?.let(::overviewOf).orEmpty() }
```

and `CommandRegistryTest.onlyCommandsChangeTheProject` scans the sources for `OptIn(EditorMutation` outside `commands` (M03 made it
walk `creatorSourceRoots()`, so it already covers every module). The command test helpers (`commandContext`, `assertRoundTrip`,
`FakeProjectRunner`) and the Breakout fixture are used by `mcp`/`ai` tests too (`McpToolServerTest`, `AssistantLiveScenariosTest`,
`EvalSetTest`, `TokenBudgetTest`).

## Fix

1. `settings.gradle.kts`: `":creator:commands",`. `creator/commands/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
   }

   dependencies {
       api(projects.creator.core)
       api(projects.creator.git)
       implementation(projects.creator.template)
       testFixturesApi(testFixtures(projects.creator.core))
       testFixturesApi(testFixtures(projects.creator.git))
       testFixturesApi(testFixtures(projects.creator.foundation))
       testFixturesApi(libs.kotlin.test)
       testFixturesApi(libs.kotlinx.coroutines.test)
   }

   // The engine's overview, which get_kubriko_api hands the AI.
   tasks.processResources {
       from(rootProject.file("CLAUDE.md")) { rename { "kubriko-overview.md" } }
   }
   ```
   (`api(projects.creator.git)` because `CommandContext`'s public constructor takes a `GitController`.) Remove the
   `processResources` block from `creator/build.gradle.kts`; the app adds `implementation(projects.creator.commands)` and
   `testImplementation(testFixtures(projects.creator.commands))`.
2. `git mv` both packages and their tests. Move `CommandTesting.kt`, `FakeProjectRunner`, and `BreakoutFixture` to
   `creator/commands/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/…` keeping their packages (so the 26 command test files
   need no import changes), and make them `public`. `blockingTest`/`testDirectory` are in foundation's fixtures (M03), `FakeBlockTypeRepository` in core's (M07), and
   `BlockTypesController`, which `CommandTesting` builds over that fake, in core since M06, so the fixtures need nothing above
   commands; if `CommandTesting` still imports `workspace`, M06 was not finished: stop and say so.
3. **API.** This is the second-largest visibility change: `EditorCommand`, `CommandExecutor`, `CommandRegistry`, `CommandContext`,
   `Invocation`, `CommandOutcome`, `Rejection`, `CommandProblem`, `ChangeRecord`, `AiStep`, `ProjectRunner`, the input classes the UI
   builds (`PlaceActor.Input`, …), and the `context` functions `mcp` uses become `public`. Every command object is `public` too: the app's `ui/CommandTitles.kt` (M02a) maps
   each by `<Command>.name`, and the UI builds `Invocation(PlaceActor, …)`. Their `name`, `validate`, `apply`, and other overrides are
   public through the interface (an override can't narrow it); keep `internal` only the helpers outside the interface (the
   `ActorTypeValidation.kt` extensions, `PropertySceneValues`, K02b's `CompositeCommand`, which stays internal by decision, …). The sealed
   `EditorCommand`'s sub-interfaces stay in package `commands` in this module, so sealing still holds.
4. `KubrikoApiTest` (moved) still reads the overview from the classpath; `context/KubrikoApi.kt`'s heading check (EXTRA-5) still
   matches the root `CLAUDE.md`'s headings.
5. `PackageLayeringTest`: the commands layer's files must all be under `creator/commands/src`.
6. `creator/CLAUDE.md`: Definition of done's "in command tests `commands/CommandTesting.kt` (`commandContext`, `assertRoundTrip`)"
   → "…`CommandTesting.kt` in `:creator:commands`' test fixtures…"; add `commands` to the Modules list.

## Tests

- `:creator:commands:test` runs the moved tests, `CommandRegistryTest.onlyCommandsChangeTheProject` included (it must still find
  the app's and the other modules' sources: temporarily add `@OptIn(EditorMutation::class)` to a UI file and see it fail).
- `KubrikoApiTest` passes (the overview resource is in the commands jar).
- `./gradlew :creator:allTests` with the same test count as before.
