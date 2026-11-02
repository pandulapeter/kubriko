<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Add a test that holds Kubriko Creator's packages to the layers of the planned Gradle modules, with a shrinking list of the known exceptions

**Challenged:** amended — K09a's `scenes/SceneChange` joins the core Scene files; the `BlockTypesController` edges go to M06 whatever K09b did; the template integration test's edge is listed.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** ARCH-1 (rest), Modularization proposal ("cheap first step: package layering test"), decisions.md "layering test first"  ·  **Depends on:** every phase 1 and phase 2 plan (T, S, A, G, K, H lanes) having landed
**Files:**
- new `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/PackageLayeringTest.kt`
- `creator/CLAUDE.md` (one sentence under Running and testing)

Paths below are relative to `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

The user decided a full staged split of `:creator` into Gradle modules (decisions.md: "full staged split (~10 modules, app stays
:creator), layering test first"). Until the split lands, nothing stops a change from adding an import that runs upward (a model
type importing a workspace class), and each such import becomes a blocker the split has to remove first. The review's moves
(`foundation`, `console`, `schema` packages) already broke most cycles; at `ecf76faf6` the package graph's only two-way edges
are `(root) <-> ui` (app-level, fine), `actors <-> project`, `actors <-> scenes` (all inside the planned `core`), `commands <->
context` (one module), and `commands <-> workspace` through `workspace.BlockTypesController`:

```kotlin
// commands/CommandContext.kt
import com.pandulapeter.kubrikoCreator.workspace.BlockTypesController
…
    val blockTypes: BlockTypesController?,
```

Mapped to the planned modules, the imports that run upward at `ecf76faf6` are exactly these (main sources):

| Import | Removed by |
|---|---|
| `ui/components/ColorField.kt`, `ui/components/ColorPickerValue.kt` → `actors.ColorHex` | M04 |
| `ui/theme/CreatorTheme.kt` → `settings.ThemeChoice` | M04 |
| `project/ProjectInspector.kt` → `gradle.GradleProjectConnection` | M06 |
| `commands/CommandContext.kt`, `commands/actorTypes/ActorTypeValidation.kt` → `workspace.BlockTypesController` (after K09b: `workspace` types its `commands.BlockTypes` interface uses, such as K01a's `FailedWrite`) | M06 (K09b's interface removes the main edge, but `CommandTesting` still builds the real controller, so M06 moves it into core either way) |

and in test sources:

| Import | Removed by |
|---|---|
| `ui/components/{BufferedTextFieldTest,ColorFieldTest,NumberFieldTest}.kt` → `ui.uiTest`, `ui.showing`, `ui.saveSnapshot` (`ui/UiSnapshots.kt`) | M04 |
| `actors/KotlinIdentifiersTest.kt` → `template.ProjectNaming` | M06 |
| `session/GameLaunchTest.kt`, `session/HotReloadLaunchTest.kt` → `scenes.ProjectClasspath` | M06 |
| `template/ProjectTemplateIntegrationTest.kt` → `scenes.ProjectClasspath` | M06 |
| `commands/**` tests → `workspace.FakeBlockTypeRepository`, `workspace.BlockTypesController` | T01 (moves the fakes to `testing`) and M06 |

## Fix

Add `PackageLayeringTest` (package `com.pandulapeter.kubrikoCreator`, beside `AppControllerTest`). It reads sources, so it needs no
production change.

**The layers**, lowest first, as a literal list in the test (the module each becomes in brackets, in the order M03–M14 extract
them):

```kotlin
private val layers = listOf(
    Layer("foundation", "foundation", "console"),
    Layer("ui-components", "ui.components", "ui.theme"),
    Layer("core", "schema", "actors", "project", *coreSceneFiles),
    Layer("gradle-session", "gradle", "session"),
    Layer("git", "git"),
    Layer("template", "template"),
    Layer("commands", "commands", "context"),
    Layer("ai", "ai", "mcp"),
    Layer("scene-runtime", "scenes"),
    Layer("workspace", "workspace"),
    Layer("app", "", "ui", "settings", "recent", "window", "benchmark"),
)
private val coreSceneFiles = arrayOf("scenes/SceneStore", "scenes/SceneDocument", "scenes/SceneActorEntry", "scenes/SceneSettingsState",
    "scenes/SceneDescriptions", "scenes/SceneNaming", "scenes/ActorStateEdits", "scenes/SceneFileProblem", "scenes/ScenePatch",
    "scenes/SceneChange")
```

`scenes/SceneChange` is K09a's typed change that `SceneStore` records: core, although no import shows it (same package). Add any other
file phase 1–3 plans created in `scenes/` that `SceneStore` or another core Scene file uses (`grep` each new `scenes/` file's
top-level names in the core files); S01's `SharedLibraries` and S02's leak test are runtime.

A file's unit is its package below `com.pandulapeter.kubrikoCreator` (from its `package` line), matched by the longest entry that
equals it or is a dot-prefix of it (`ui.components.x` → `ui.components`; `ui.scenes` → `ui`); a `scenes/<Name>` entry matches the
file `scenes/<Name>.kt` before the `scenes` package does. An import `com.pandulapeter.kubrikoCreator.a.b.Name` (or `.a.b.Name.Nested`,
or a top-level function `a.b.name`) has the package `a.b` (drop trailing segments that start with an upper-case letter, then the
last segment), and for `scenes` the imported class name picks a `scenes/<Name>` entry if there is one. Skip
`kubriko.creator.generated.resources.*` and every non-Creator import.

Tests:
- `noImportRunsToAHigherLayer`: for every `.kt` file under `creator/src/main/kotlin` and `creator/src/test/kotlin`, every Creator
  import goes to the same or a lower layer, or is in `knownExceptions`. The failure lists each offending `file -> import` with the
  two layers. Test sources of T01's shared `testing` package (`creator/src/test/kotlin/.../testing/`) are skipped, both as
  importer and as target, until the extraction plans move each fixture into the `testFixtures` of its module.
- `everyKnownExceptionStillOccurs`: each entry of `knownExceptions` is still an import of that file, so the list only shrinks
  (an entry left after the import is gone fails).
- `everyPackageHasALayer`: every package found in the sources has a layer (a new package, say from a later plan, must be placed
  deliberately).
- `everyLayerHasSources`: each layer entry matches at least one file (catches a renamed package or file).
- Both source roots must exist: `assertTrue(root.isDirectory, "Run from the creator folder: ${root.absolutePath}")` (the TST-13
  pattern that `StringResourcesTest` and `CommandRegistryTest` use).

`knownExceptions` is a `Map<String, String>` from `"<path below kotlin/com/pandulapeter/kubrikoCreator> -> <import below
com.pandulapeter.kubrikoCreator>"` to the plan ID that removes it, so a failure message can say which plan owes the fix. At
execution, run the test once with an empty map: the phase 1 and 2 plans moved and added files (T01's `testing` package, K09's
change router, H03's `AppGraph`, A02's split of `AssistantController`, G02's Block descriptor, …), so the list may differ from the
tables above. Every upward import it reports goes into the map with its plan ID from the tables; one that isn't in the tables is
either fixed in this commit when it is a one-file move (say so in the commit's report) or added with the ID of the extraction
plan whose module it blocks, and named in the report so that plan's executor knows.

Keep the file under 500 lines and every function under 120 (`check_sizes.py`); a `Layer` class and the parsing helpers can live
in the same file while it stays small.

`creator/CLAUDE.md` → Running and testing: one sentence, "`PackageLayeringTest` keeps each package in the layer of the Gradle
module it belongs to (lowest first: foundation, ui-components, core, gradle-session, git, template, commands, ai, scene-runtime,
workspace, the app); a new package gets a layer, and an import may only run downward." (The extraction plans later turn layers
into modules and trim this sentence.)

## Tests

The test itself. To show it bites, temporarily add `import com.pandulapeter.kubrikoCreator.workspace.ConsoleLog` to
`actors/ColorHex.kt` and see `noImportRunsToAHigherLayer` fail naming `actors/ColorHex.kt -> workspace.ConsoleLog (core -> workspace)`;
remove an entry from `knownExceptions` and see it fail; add a stale entry and see `everyKnownExceptionStillOccurs` fail. Then run
the whole `./gradlew :creator:test`.
