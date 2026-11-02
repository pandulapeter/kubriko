<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's Gradle connection and game sessions (`gradle`, `session`) into `:creator:gradle-session`

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** Modularization proposal ("build = gradle + session"; corrected order: it sits above core once M06 moved the desktop-module lookup)  ·  **Depends on:** M07
**Files:**
- `settings.gradle.kts`; new `creator/gradle-session/build.gradle.kts`; `creator/build.gradle.kts`
- moved (main and test): `gradle/**` (with M06's `DesktopModules`, `ModuleNode`, `ProjectInspectionException`), `session/**` (with M06's `ProjectClasspath`), `creator/src/main/resources/kubriko-creator-session.init.gradle.kts` → `creator/gradle-session/src/{main,test}/…`
- new `creator/gradle-session/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/testing/{FakeProjectBuild,FakeSessionChannel}.kt` (from the app's tests)
- importers of newly public declarations (visibility only)
- `PackageLayeringTest.kt`; `creator/CLAUDE.md` (Modules list)

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

The module name the review proposed, `build`, can't be used: `:creator:build` would live in `creator/build`, the app's own Gradle
output folder. The packages talk to Gradle (`gradle/GradleProjectConnection.kt`, the only Tooling API user after M06 besides
`template/GradleWrapperInstaller.kt`) and to the game process (`session/`), and depend on `foundation`
(`session/SessionInitScript.kt` imports `foundation.settingsDirectory`) and, since M06, on core (`ProjectProblem`). The session
init script is a classpath resource read by name:

```kotlin
// session/SessionInitScript.kt:38
        val content = SessionInitScript::class.java.classLoader.getResourceAsStream(RESOURCE_NAME)?.use { it.readBytes() }
```

Above them, `workspace/` (`SessionController`, `GameBuilds`, `GradleWorkspaceOpener`, `CreatorTaskRunner`, `WorkspaceRunner`,
`WorkspaceController`) and `sceneRuntime/` use these types, and `workspace/` and `ui/WorkspaceToolbar.kt` use the published
protocol types (`com.pandulapeter.kubriko.creatorSession.protocol.SessionMode`, `SessionMessage`, `SessionCommand`).
The workspace tests fake two interfaces of these packages: `FakeProjectBuild : ProjectBuild` and `FakeSessionChannel : SessionChannel`
(today in `creator/src/test/kotlin/…/workspace/`, or in T01's `testing` package).

## Fix

1. `settings.gradle.kts`: `":creator:gradle-session",`. `creator/gradle-session/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
   }

   dependencies {
       api(projects.creator.core)
       api(projects.tools.creatorSession)
       implementation(libs.gradle.toolingApi)
       testImplementation(testFixtures(projects.creator.foundation))
   }
   ```
   `api(projects.tools.creatorSession)` because the protocol types appear in this module's public signatures and the app uses
   them; `toolingApi` stays `implementation` if no public signature exposes a Tooling API type (check `GradleProjectConnection`'s
   public members; if `model(type: Class<T>)` stays public with `org.gradle.tooling` types in it, make it `api`). The repository
   for `org.gradle` artifacts is already declared in `settings.gradle.kts` for every project. The app replaces
   `implementation(projects.tools.creatorSession)` and `implementation(libs.gradle.toolingApi)` with
   `implementation(projects.creator.gradleSession)` only if the app itself no longer imports them (`grep -rln "org.gradle.tooling\|creatorSession.protocol" creator/src/main`);
   `template` still needs the Tooling API until M10 gives it its own dependency.
2. **Move** with `git mv`: the two packages, their tests (`gradle/*Test.kt`, `session/*Test.kt` incl. `GameProcessTestGame.kt`,
   which `GameProcessTest` starts in a child JVM from the test classpath, so it must stay in this module's test source set), and the
   init script resource to `creator/gradle-session/src/main/resources/` (same name, read from the classpath root, so
   `SessionInitScript` doesn't change). `SessionInitScriptTest` reads it the same way.
3. **API.** Make public what `workspace`, `sceneRuntime`, `template` (if it uses `ProjectClasspath` in tests only, that's a test
   dependency, see M10), and the app use; keep the rest `internal`. Note S04's session-connection rules (accept only after Hello,
   one per launch) live in `SessionServer`; keep its accepting logic `internal` and expose only what `GradleWorkspaceOpener` calls.
4. **Fixtures.** Move `FakeProjectBuild` and `FakeSessionChannel` (and any T01 contract test against `GradleProjectConnection`/
   `SessionServer`) into `creator/gradle-session/src/testFixtures/…/testing/`, `public`; the app's tests depend on
   `testFixtures(projects.creator.gradleSession)`.
5. `PackageLayeringTest`: the gradle-session layer's files must all be under `creator/gradle-session/src`.
6. `creator/CLAUDE.md`: add `gradle-session` (Gradle and game sessions) to the Modules list.

No change to `tools/creator-session` or the session protocol (published artifacts).

## Tests

- Every moved test passes in `:creator:gradle-session:test`, including `GameProcessTest` (child JVM) and `SessionServerTest`
  (localhost), on Linux and Windows in `Tests`.
- `SessionInitScriptTest` still finds the init script (the resource moved jars).
- `./gradlew :creator:allTests` with the same test count as before.

## Manual check

`./gradlew :creator:run`, open a scratch Breakout copy, press Play: the game starts through the session init script (a missing
resource would fail the launch with a console error).
