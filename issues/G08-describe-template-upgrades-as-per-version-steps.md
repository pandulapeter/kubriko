<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Describe each template upgrade as a list of per-version TemplateSteps that check() folds over

**Challenged:** amended — the `template` package already has `internal class TemplateFile(path, content)` (a template's file before rendering), so the per-file list of steps is `UpgradedFile`; `CURRENT_VERSION` read from `TemplateSteps` must not make the two objects' initialization depend on each other.

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** ACT-16  ·  **Depends on:** none (lane order: after G07; do it before the next template version)
**Files:** `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/template/ProjectUpgrader.kt`, `template/TemplateStep.kt` (new), `template/TemplateSteps.kt` (new, the list), `template/GameIdentityUpgrade.kt`, `template/RunningInstancesUpgrade.kt`, new step objects for versions 2–7 if extracted (`template/SceneWiringUpgrade.kt`, `template/PluginManagersUpgrade.kt`, `template/PlaySessionUpgrade.kt`, `template/GradleCachesUpgrade.kt` — names are suggestions), `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/template/ProjectUpgraderTest.kt`, `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/template/TemplateStepsTest.kt` (new), `creator/CLAUDE.md` (Template upgrades: one sentence on how a version is added)

## Problem

`ProjectUpgrader.check()` wires every file by hand, with each file's versions folded into one predicate and one change:

```kotlin
val isGameUpgraded = propose("$packagePath/$GAME_FILE", { isGameUpToDate(it) && RunningInstancesUpgrade.isGameDone(it) }) { text ->
    upgradedGame(text, wiringImport)?.let(RunningInstancesUpgrade::game)
}
propose("$packagePath/$ENTRY_POINT_FILE", { ManagerTypeCodeGenerator.WIRING_NAME in it && RunningInstancesUpgrade.isEntryPointDone(it) }) { ... }
...
propose(mainPath, { turnsLoggingOn(it) && overridesFirstScene(it) && GameIdentityUpgrade.isMainDone(it) }) {
    withFirstSceneOverride(it, wiringImport)?.let(GameIdentityUpgrade::main)
}
...
val properties = read(GRADLE_PROPERTIES_FILE).orEmpty()
if (!setsCaches(properties) || !GameIdentityUpgrade.isPropertiesDone(properties)) {
    changes += ProposedFileChange(GRADLE_PROPERTIES_FILE, properties, GameIdentityUpgrade.properties(withCaches(properties), gameName, packageName))
}
val sceneLoaderPath = "$packagePath/$SCENE_LOADER_FILE"
read(sceneLoaderPath)?.takeIf { isGameUpgraded && it.trimEnd() == templateSceneLoader(packageName)?.trimEnd() }?.let { ... after = null ... }
```

`CURRENT_VERSION = 9` is a constant apart from the code that implements the versions, and the class KDoc narrates versions 2–7 while 8 and 9 live in `RunningInstancesUpgrade` and `GameIdentityUpgrade`. A version 10 means editing three predicates and changes in the middle of this function and remembering to raise the constant; nothing checks that each version has a step, or that a new project needs none.

## Fix

Behaviour-preserving: `ProjectUpgraderTest` passes unchanged.

1. `TemplateStep.kt`:
   ```kotlin
   /** One template version's change to one file. */
   internal interface TemplateStep {
       val version: Int
       fun isDone(text: String, context: TemplateContext): Boolean
       /** [text] with this step made, or null if the file doesn't look like the template's (left to the user). */
       fun change(text: String, context: TemplateContext): String?
   }
   /** The steps of one file, in version order, and what to do when it is missing. */
   internal class UpgradedFile(val path: (TemplateContext) -> String, val steps: List<TemplateStep>, val whenMissing: WhenMissing, val deletion: TemplateDeletion? = null)
   internal enum class WhenMissing { BY_HAND, SKIP, CREATE_EMPTY }
   ```
   `TemplateContext` holds what `check()` computes up front today: `packageName`, `sourceDirectory`, `packagePath`, `mainPath`, `pagePath`, `wiringImport`, a `read(path)` with Unix line breaks, and `gameName` (lazy, from `Main.kt`'s window title, the page title, or `ProjectInspector.nameOf(rootDirectory)`, read before any change, as now).
2. `TemplateSteps.FILES: List<UpgradedFile>` (not `TemplateFile`: that name is taken in this package by `template/TemplateFile.kt`), in today's order: `Game.kt` (v2/v3 scenes through the wiring = `upgradedGame`'s first half, v4 plugin Managers = `withoutPluginManagers`, v8 `RunningInstancesUpgrade.game`), `GameEntryPoint.kt` (v2 `withManagersHandedToTheSceneEditor`, v8 `RunningInstancesUpgrade.entryPoint`), `Main.kt` (v5 `withLoggingTurnedOn`, v7 the first-Scene override, v9 `GameIdentityUpgrade.main`), the build file (v9), `index.html` (v9, `SKIP` when missing), `gradle.properties` (v6 `withCaches`, v9 `GameIdentityUpgrade.properties`; `CREATE_EMPTY`), and `SceneLoader.kt` (v3 deletion, only when `Game.kt`'s steps are done or proposed and the file is still `templateSceneLoader(packageName)`: model it as `deletion`). Keep each version's existing quirks exactly: `RunningInstancesUpgrade` and `GameIdentityUpgrade.main`/`page` return the text unchanged (counting as done) when the file doesn't look like the template's, `upgradedGame`/`withLoggingTurnedOn` return null (by hand), and `gradle.properties` is never left to the user.
3. `check()` folds: for each file, read it; if missing, apply `whenMissing`; skip the steps whose `isDone` holds; run the others' `change` in version order on the running text; a null means the file goes to `byHand`; a changed text becomes one `ProposedFileChange` (so a file still gets one diff for several versions, as now). `CURRENT_VERSION` becomes `TemplateSteps.FILES.flatMap { it.steps }.maxOf { it.version }` (keep it a `val` in the companion; `ProjectGenerator.placeholderValues` and the tests read it, and the starter's `{{TEMPLATE_VERSION}}` placeholder follows it). Steps refer to `ProjectUpgrader`'s companion functions only inside `isDone`/`change`, never in their constructors or initializers, so `ProjectUpgrader.Companion` initializing `CURRENT_VERSION` from `TemplateSteps.FILES` can't read a half-built object.
4. Move the class KDoc's version narrative onto the steps (one KDoc each, like `GameIdentityUpgrade`'s); the class KDoc keeps what an upgrade is and the "looks like the template's" rule. `ProjectUpgrader.kt` keeps `apply`, `recordVersionIfUpToDate`, and the settings I/O; the step functions it holds today can stay in its companion where tests call them (`ProjectUpgrader.withLoggingTurnedOn`, `withScenesLoadedByWiring`, `withManagersHandedToTheSceneEditor`, `withCaches`) or move with forwarding `internal` aliases, so `ProjectUpgraderTest` doesn't change.
5. `creator/CLAUDE.md` → Template upgrades: "A new template version is a `TemplateStep` per file it changes, added to `TemplateSteps.FILES`; the version number follows from them."

## Tests

- `ProjectUpgraderTest`: unchanged, all pass.
- `TemplateStepsTest.everyVersionFromTwoToTheCurrentHasAStep`: `(2..ProjectUpgrader.CURRENT_VERSION).toSet()` equals the steps' versions, and each file's steps are in ascending version order. Fails if a version is skipped or a step is added under an old number.
- `TemplateStepsTest.everyStepIsDoneForANewProject`: a project made by `ProjectGenerator` in a temp folder (as `ProjectUpgraderTest.aNewProjectIsUpToDate` makes it, without running Gradle: use the same helper), every existing file's every step has `isDone(text, context)`; names the step and file on failure. This is what catches a template change that forgets its upgrade, or the reverse.
- `TemplateStepsTest.aFileTheUserChangedTooMuchIsLeftToThemOnce`: a `Main.kt` whose `main` isn't `fun main() = application {` at version 4 lands once in `byHand`, with no `ProposedFileChange` for it.
