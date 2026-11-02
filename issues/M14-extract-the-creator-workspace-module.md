<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's open-project wiring (`workspace`) into `:creator:workspace`, leave only the app's UI and settings in `:creator`, and finish the docs of the split

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** Modularization proposal (workspace; last extraction)  ·  **Depends on:** M13
**Files:**
- `settings.gradle.kts`; new `creator/workspace/build.gradle.kts`; `creator/build.gradle.kts` (final dependency list)
- moved (main and test): `workspace/**` → `creator/workspace/src/{main,test}/kotlin/com/pandulapeter/kubrikoCreator/workspace/`
- test fixtures the app's tests need from workspace, if any (T03's collaborator fakes)
- importers of newly public declarations (visibility only)
- `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/PackageLayeringTest.kt`
- docs: `creator/CLAUDE.md` (Modules; Running and testing), `CLAUDE.md` (Module layout → `creator/`), `gradle/build-logic/CLAUDE.md`

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

`workspace/` (the open project: `WorkspaceController`, `SessionController`, `GameBuilds`, `CreatorTaskRunner`, `GradleWorkspaceOpener`,
K09's change router, …) is the last layer below the app: it uses commands, git, template, gradle-session, scene-runtime, core, and
foundation (`PackageLayeringTest` confirms nothing above). After it, `:creator` holds only the root package (`KubrikoCreatorApp`,
`AppController`, `AppScreen`, H03's `AppGraph`), `ui/**`, `settings/`, `recent/`, `window/`, and the benchmark tests, which is the
app the decision kept at `:creator` so `./gradlew :creator:run` keeps working.

## Fix

1. `settings.gradle.kts`: `":creator:workspace",`. `creator/workspace/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
   }

   dependencies {
       api(projects.creator.commands)
       api(projects.creator.sceneRuntime)
       implementation(projects.creator.template)
       testImplementation(testFixtures(projects.creator.commands))
       testImplementation(testFixtures(projects.creator.gradleSession))
   }
   ```
   (`api` for what the UI reaches through the workspace's public types: `WorkspaceController` exposes the command executor and the
   Scene editing controller.)
2. `git mv` the package and its tests (T03's split `WorkspaceControllerTest` and the collaborators' tests). A workspace fake that the
   app's tests use (for example a `WorkspaceOpener` fake in `AppControllerTest`) goes to `creator/workspace/src/testFixtures/…`.
3. **API.** Public: what the app uses (`WorkspaceController` and its state flows, `WorkspaceOpener`, `GradleWorkspaceOpener`,
   `SessionState`, `ConsoleLog`'s read side, …); keep the collaborators (`GameBuilds`, `CreatorTaskRunner`, `PendingWrite` if it is
   still here, `DesktopModuleLookup`) `internal`.
4. **The app's build** ends with only what the app's own code imports: the Compose plugins and `compose.desktop.application`,
   `compose.desktop.currentOs`, `libs.compose.resources`, `libs.jbr.api` (the title bar), `libs.kotlinx.coroutines.desktop`
   (`Dispatchers.Main` for every module, at run time), `libs.kotlinx.serialization` (settings and recent projects), the modules it
   uses (`workspace`, `ai`, `ui-components`, and whichever lower ones it imports directly), `testFixtures(...)` of the modules its
   tests use, `libs.kotlin.test`, `compose.desktop.uiTestJUnit4`, `libs.kotlinx.coroutines.test`, and the `kubrikoCreator.directory`
   test property. Check each remaining external library with `grep -rln "<package>" creator/src`; drop the ones nothing in the
   app imports.
5. **`PackageLayeringTest`** now mostly duplicates what Gradle enforces between modules. Keep `everyModuleHoldsOnlyItsLayer` (a file
   in the wrong module compiles as long as the module depends on what it imports), `everyPackageHasALayer`, the actors sublayer rule
   (M05), and the "no import to a higher layer" check (it still catches an upward import inside one module); make
   `knownExceptions` an empty set kept as a deliberate extension point, or remove it with its test if the user prefers. Its KDoc
   says the layer list mirrors `settings.gradle.kts`' `:creator:*` modules.
6. **Docs.**
   - `creator/CLAUDE.md` → Modules: the complete list, lowest first, a few words each: foundation (files, settings folder, typed
     texts, console entries), ui-components (shared components, theme, offscreen UI test helpers), core (Block model, code
     generation, type store, project files, Scene files), gradle-session (Gradle and game sessions), git, template, commands (commands
     and the AI's context), ai (MCP server and assistant), scene-runtime (the Scene editor's live preview), workspace (the open
     project), and the app (`:creator`: UI, settings, window). Then the rules in two sentences: a module depends only on lower ones
     (Gradle refuses the rest; `PackageLayeringTest` keeps each file in its module's layer), and what another module uses is `public`
     under explicit API mode. Remove M01's interim sentence from Running and testing.
   - `CLAUDE.md` Module layout → `creator/`: "Kubriko Creator, … split into Gradle modules under `creator/` (`creator/CLAUDE.md` →
     Modules); the app is `:creator`."
   - `gradle/build-logic/CLAUDE.md`: the `kubriko-creator-module` row lists the modules that apply it (`:creator:*`, not the app).
   - Run `check_doc_names.py --staged` and `check_license_headers.py`.

## Tests

- `:creator:workspace:test` runs the moved tests; the app's tests pass with the fixtures.
- `./gradlew :creator:allTests spotlessCheck` with the same test count as before the split (compare with the count recorded in
  M03's report).
- `./gradlew :creator:dependencies --configuration runtimeClasspath` lists every `:creator:*` module (the app reaches them all).

## Manual check

`./gradlew :creator:run`: open the scratch Breakout copy, edit a type, Play, open a Scene, commit in the version control panel,
and ask the assistant one read-only question. Then `./gradlew :creator:createDistributable` and start the packaged app once: its
`lib/` holds one `kubriko-creator-<module>.jar` per module, and the New project screen and a Scene open work (resources and
classpath sharing from the packaged jars).
