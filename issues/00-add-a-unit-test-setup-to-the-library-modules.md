# Add a unit test setup to the library modules so the sweep's plans can land tests

**Challenged:** amended — the Wasm browser and iOS simulator test tasks are disabled, so `./gradlew build` does not start running tests that need a browser or a simulator; `commonTest` must still compile on every target.

**Kind:** build  ·  **Severity:** high (blocks every other plan's Tests section)  ·  **Platforms:** build only
**Files:** `gradle/libs.versions.toml`, `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/extensions/KotlinMultiplatform.kt`, `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/TestSetupTest.kt` (new), `CLAUDE.md`, `.claude/skills/codebase-review/SKILL.md`

**Lands before the lanes are cut** — the orchestrator applies this in the main checkout right after committing the plans, and `START` is the commit after it, so every lane's worktree can compile tests.

## Problem

At 0008d027 no module has a test source set or a test dependency. `gradle/libs.versions.toml` has no `kotlin-test` entry and `configureKotlinMultiplatform` (`KotlinMultiplatform.kt`) wires no `commonTest` dependencies. `CLAUDE.md` and the `codebase-review` skill both say to verify with `./gradlew test`, which finds no test task. The live stress run had to inject `kotlin-test-junit` through an untracked Gradle init script to run at all.

## Fix

1. `gradle/libs.versions.toml` — add, under `[libraries]`:
   ```toml
   kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlin" }
   kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
   ```
2. `configureKotlinMultiplatform` — after the targets are declared, add to `sourceSets.commonTest.dependencies`: `implementation(libs.findLibrary("kotlin-test").get())` and `implementation(libs.findLibrary("kotlinx-coroutines-test").get())`. The Kotlin Gradle plugin resolves `kotlin-test` to the JUnit flavour for the `desktop` JVM target automatically. Because every published and example module applies `kubriko-library` (directly or via `kubriko-compose-library`), this covers all of them; the dependency is test-scoped and does not enter published POMs/Gradle metadata — confirm by diffing `./gradlew :engine:generateMetadataFileForKotlinMultiplatformPublication` output before/after (drop the extra step if the task name differs; the check is "no `kotlin-test` in the published module file").

   In the same function, disable the test *run* tasks of the non-JVM targets. Without this, `./gradlew build`
   (`check` → `allTests`) would try to run every `commonTest` in a browser through Karma (no browser is configured)
   and on an iOS simulator:
   ```kotlin
   tasks.withType<KotlinJsTest>().configureEach { enabled = false }
   tasks.withType<KotlinNativeTest>().configureEach { enabled = false }
   ```
   The imports are `org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest` and
   `org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest`. The function extends `Project`, so `tasks`
   resolves to the project's tasks. Add one comment line saying why.

   The test *compilations* for those targets still run under `build`, because a disabled task's dependencies are still
   executed. That is intended: `commonTest` code must compile on every target, so JVM-only APIs (`Thread`,
   `java.util.concurrent.*`) belong in `desktopTest`.
3. Add `engine/src/commonTest/kotlin/com/pandulapeter/kubriko/TestSetupTest.kt` (MPL header) with one test that creates `Kubriko.newInstance(tickSource = TickSource.manual())`, starts it, ticks once, disposes it, and asserts `get<ActorManager>()` then throws `IllegalStateException`. It exists so the setup is proven and later lanes have a pattern to copy.
4. The canonical test command becomes `./gradlew desktopTest` (runs the JVM tests of every module; the Wasm browser and iOS simulator test tasks need a browser / Xcode and are not part of verification). Update `CLAUDE.md` → "Build, run, test": replace `./gradlew test` with `./gradlew desktopTest`, and replace the single-test example with `./gradlew :engine:desktopTest --tests "com.pandulapeter.kubriko.TestSetupTest"`. Update the skill's "Tests:" bullet the same way, and add to `CLAUDE.md` one sentence: "`commonTest` runs on the
   desktop JVM only (the Wasm and iOS test tasks are disabled); JVM-only APIs go in `desktopTest`." 

## Tests

The new `TestSetupTest` is the test. Run `./gradlew :engine:desktopTest`, then `./gradlew desktopTest`, then
`./gradlew :engine:build` — the last must pass without a browser and must not report a Wasm or iOS test run.

## Manual check

None.
