<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Move the types that cross the planned `core` boundary: the Scene editor's runtime into `sceneRuntime`, the classpath into `session`, the desktop-module lookup into `gradle`, and the type store's controller into `actors.store`

**Challenged:** amended — `BlockTypesController` (with `PendingWrite`, K01a's `FailedWrite`, and K09b's `BlockTypes` interface) moves to `actors.store` in every case, since `CommandTesting` builds the real controller and M11 puts it in commands' test fixtures; K09a's `SceneChange` stays core.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** Modularization proposal (blockers 1 and 2 the verifier confirmed: `core -> build` through `ProjectInspector`, `commands -> workspace` through `BlockTypesController`), ARCH-2 (the concrete type in `CommandContext`)  ·  **Depends on:** M05, K09
**Files:**
- `scenes/*.kt` (19 runtime files move to a new package `sceneRuntime`, listed below) and `creator/src/test/kotlin/…/scenes/*Test.kt` of those files
- `scenes/ProjectClasspath.kt` → `session/ProjectClasspath.kt`
- `project/ProjectInspector.kt`, `project/ModuleNode.kt`, `project/ProjectInspectionException.kt` → new `gradle/DesktopModules.kt`, `gradle/ModuleNode.kt`, `gradle/ProjectInspectionException.kt`; their tests
- `workspace/BlockTypesController.kt`, `workspace/PendingWrite.kt`, `workspace/FailedWrite.kt` (K01a), `commands/BlockTypes.kt` (K09b) → `actors/store/` (see Fix 4), `workspace/BlockTypesControllerTest.kt`, `workspace/PendingWriteTest.kt` (T03)
- `creator/src/test/kotlin/…/actors/KotlinIdentifiersTest.kt`, `…/template/ProjectNamingTest.kt`
- importers: `ui/scenes/**`, `ui/WorkspaceScreen.kt`, `workspace/{WorkspaceController,GameBuilds,GradleWorkspaceOpener,DesktopModuleLookup}.kt`, `commands/CommandContext.kt`, `commands/actorTypes/ActorTypeValidation.kt`, `template/ProjectUpgrader.kt`, tests (`grep` per moved name)
- `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/PackageLayeringTest.kt`
- `creator/CLAUDE.md` (Scene editing section: package names it cites)

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` (still the app module; M03/M04 extracted only foundation and ui-components).

## Problem

After M05, the imports that keep `core` (actors, project, schema, the Scene files) from being a module of its own are these
(re-run `PackageLayeringTest` with an empty `knownExceptions` to confirm at execution):

```kotlin
// project/ProjectInspector.kt — core importing the Gradle Tooling API connection
import com.pandulapeter.kubrikoCreator.gradle.GradleProjectConnection
    suspend fun desktopModulePathOf(connection: GradleProjectConnection) = findDesktopModulePath(ModuleNode.from(connection.model(GradleProject::class.java)))
// commands/CommandContext.kt — commands importing workspace
import com.pandulapeter.kubrikoCreator.workspace.BlockTypesController
    val blockTypes: BlockTypesController?,
// commands/actorTypes/ActorTypeValidation.kt
internal val CommandContext.requireBlockTypes: BlockTypesController get() = checkNotNull(blockTypes)
```

and the `scenes` package is half core (the Scene files the commands and the type store read and write) and half the Scene
editor's runtime (class loaders, the live `Kubriko` preview, engine imports), which can't share one package across two modules
without `internal` breaking between them. Test sources add:

```kotlin
// session/GameLaunchTest.kt, session/HotReloadLaunchTest.kt (gradle-session layer) → scenes.ProjectClasspath (scene-runtime)
// actors/KotlinIdentifiersTest.kt:26 (core) → template.ProjectNaming
        assertNotNull(ProjectNaming.validatePackageName("com.typealias"))
```

`ProjectClasspath` itself imports only `session` (`session.GameLaunch`, `session.HotReloadLaunch`), and `ModuleNode` imports the
Tooling API (`org.gradle.tooling`), which only the `gradle` package should need.

## Fix

Pure moves, one commit; no behaviour changes. Use `git mv`, fix `package` lines and imports, then `./gradlew spotlessApply`.

1. **The Scene editor's runtime → package `com.pandulapeter.kubrikoCreator.sceneRuntime`.** Core keeps in `scenes`:
   `SceneStore`, `SceneDocument`, `SceneActorEntry`, `SceneSettingsState`, `SceneDescriptions`, `SceneNaming`, `ActorStateEdits`,
   `SceneFileProblem`, `ScenePatch`, K09a's `SceneChange` (M01's `coreSceneFiles`). Everything else of `scenes/` moves: at `ecf76faf6`
   `ActorBodyValues`, `ActorTypeLook`, `ClassLoaderEntryPointLoader`, `CreateDisposingIfCancelled`, `EntryPointLoader`,
   `EntryPointLoadingException`, `LoadedEntryPoint`, `PreviewActor`, `ProjectClassLoader`, `ProjectSpriteLoader`,
   `SceneBoundsGrid`, `SceneCamera`, `SceneEditingController`, `SceneEditingState`, `SceneFraming`, `SceneHitTest`, `SceneView`,
   `SpriteLoadingEntryPoint`, and M02b's `SceneProblem`/`SceneStatus` (S-lane plans may have added files, such as S01's version gate
   and S02's leak test: they are runtime). Their tests move to `…/sceneRuntime/`. Check before moving that no core file
   references a runtime name (same-package use has no import): at `ecf76faf6` none does.
2. **`ProjectClasspath` → `session`** (it describes what a game launch puts on the classpath, and imports only `session`).
   `scenes/ProjectClasspathTest.kt` moves with it. Its importers: `sceneRuntime/{ClassLoaderEntryPointLoader,EntryPointLoader,SceneEditingController}.kt`, `workspace/GameBuilds.kt`,
   and the tests `session/{GameLaunchTest,HotReloadLaunchTest}.kt` (now same-package) and `template/ProjectTemplateIntegrationTest.kt`.
3. **The desktop-module lookup → `gradle`.** Split `ProjectInspector`: `isGradleProject` and `nameOf` (files only; used by
   `workspace/GradleWorkspaceOpener.kt:42-43` and `template/ProjectUpgrader.kt:79`) stay in `project/ProjectInspector.kt`;
   `desktopModulePathOf`, `findDesktopModulePath`, and `ModuleNode` move to `gradle/DesktopModules.kt` and `gradle/ModuleNode.kt`
   as `internal object DesktopModules { suspend fun pathOf(connection: GradleProjectConnection) …; internal fun find(root: ModuleNode) … }`
   (keep the KDoc), with `ProjectInspectionException` beside them (only the lookup throws it; check `grep -rn ProjectInspectionException`).
   Its `ProjectProblem` reason (M02b) stays in `project` (gradle-session sits above core). Move the matching cases of
   `ProjectInspectorTest`/`ModuleNodeTest` to `gradle/` tests. After this `core` has no Tooling API import:
   `grep -rln "org.gradle.tooling" actors project schema scenes` prints nothing.
4. **The type store's controller → `actors.store`, in every case.** K09b gave commands an interface (`commands/BlockTypes.kt`,
   implemented by `workspace/BlockTypesController`), which removes the main-source edge, but not the test one:
   `commands/CommandTesting.kt` builds the real `BlockTypesController` over `FakeBlockTypeRepository` (K09b: "the real controller
   over the fake repository is what the tests use"), and M11 makes `CommandTesting` a test fixture of `:creator:commands`, which
   can't see `:creator:workspace`. So move `BlockTypesController`, `PendingWrite`, and K01a's `FailedWrite` (and any other
   `workspace/` type the controller or the interface's signatures use: `grep -w` both files for the names of `workspace/`) to
   `actors/store/`. Core can't implement an interface declared in `commands`, so K09b's `BlockTypes` interface moves to
   `actors/store/BlockTypes.kt` with it (`CommandContext.blockTypes: BlockTypes?` keeps its type, only its import changes; the
   point of K09b, commands depending on an interface rather than the class, stays). Their imports at `cbf3b7140` are `actors.*`,
   `foundation.EditorMutation`, and `project.KubrikoBuildFileChanges`, and K01a, K09a, and H01 add only callbacks and state flows,
   so the move is legal in the layers; if a phase 1–3 plan made the controller import a higher layer (a `ConsoleEntry` is fine,
   it is foundation), pass that in as a function instead and name it in the report. `workspace/*` and `commands/*` gain imports;
   `BlockTypesControllerTest` and T03's `PendingWriteTest` move to `actors/store/`. `PackageLayeringTest`'s `BlockTypesController`
   entries (main and test) are gone after this.
5. **The test edge.** Move `KotlinIdentifiersTest`'s `ProjectNaming.validatePackageName("com.typealias")` assertion to
   `template/ProjectNamingTest` (as `aPackageNamedLikeAKeywordIsRejected`), so core's tests don't need template.
6. **Fixtures.** If any commands test still imports a fake from `workspace` (T01 should have moved them into `testing`), move it to
   `testing` now.
7. `PackageLayeringTest`: the layer table maps `sceneRuntime` to scene-runtime and drops the `scenes` package from it (all of
   `scenes` is core now, so `coreSceneFiles` goes away and `scenes` joins the core layer); remove every `knownExceptions` entry
   this plan resolved. After this commit `knownExceptions` should be empty; if not, name what's left in the commit's report.
8. `creator/CLAUDE.md`: the Scene editing section names the runtime classes without packages, so only fix a quoted
   `scenes/<File>.kt` path of a moved file (`check_doc_names.py --staged`).

## Tests

- `PackageLayeringTest` passes with an empty `knownExceptions` (the point of this plan).
- `DesktopModulesTest` (the moved `findDesktopModulePath` cases): a module tree whose second module has a `run` task returns that
  module's path, and one without throws `ProjectInspectionException` with `ProjectProblem.NO_DESKTOP_MODULE` (literals as today).
- `ProjectNamingTest.aPackageNamedLikeAKeywordIsRejected` (moved).
- `./gradlew :creator:allTests` with the same test count as before.
