<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's `foundation` and `console` packages into `:creator:foundation`, with the `kubriko-creator-module` convention plugin, an aggregate `:creator:allTests` task that CI runs, and a test-fixtures home for shared test helpers

**Challenged:** amended — the plugin gives test fixtures their libraries; every Creator-independent `testing` helper of T01/T02a/T02b moves to foundation's fixtures; S01's `generateCreatorLibraries` and its bundling test stay in the app; foundation's contents include K11a/K11b/S01 files; `tests.yml` is edited by content (T04 and T06 changed it).

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** Modularization proposal (step 1), ARCH-7 follow-through, TST-6 (java-test-fixtures)  ·  **Depends on:** M02b, P01 (the size check treats `src/testFixtures` as tests)
**Files:**
- `settings.gradle.kts` (the `include(…)` list)
- new `gradle/build-logic/src/main/kotlin/com/pandulapeter/kubriko/buildLogic/plugins/CreatorModulePlugin.kt`; `gradle/build-logic/build.gradle.kts` (register it); `gradle/build-logic/CLAUDE.md`
- new `creator/foundation/build.gradle.kts`
- moved: `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/{foundation,console}/**` → `creator/foundation/src/main/kotlin/com/pandulapeter/kubrikoCreator/{foundation,console}/**`;
  `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/foundation/**` → `creator/foundation/src/test/kotlin/…`
- new `creator/foundation/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/testing/CreatorSources.kt`; moved from
  `creator/src/test/kotlin/…/testing/`: `TestDirectories.kt`, `BlockingTest.kt`, `Awaiting.kt`, `IdleTrackingDispatcher.kt`,
  `TestPorts.kt` (→ `creator/foundation/src/testFixtures/…/testing/`) and `IdleTrackingDispatcherTest.kt`, `TestPortsTest.kt`
  (→ `creator/foundation/src/test/…/testing/`); every test importing them needs no change (same package)
- `creator/src/test/kotlin/…/foundation/CreatorLibrariesTest.kt` (its bundling case stays in the app as `CreatorLibrariesBundlingTest.kt`)
- `creator/build.gradle.kts` (dependency, `allTests`, the moved `generateCreatorProperties`, the test system property)
- tests that read sources: `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/PackageLayeringTest.kt`, `commands/CommandRegistryTest.kt`, `ui/StringResourcesTest.kt`, `ui/DomainTextsTest.kt`
- `.github/workflows/tests.yml`
- docs: `CLAUDE.md` (Build, run, test; Module layout; Conventions → Convention plugins; Testing), `creator/CLAUDE.md` (Running and testing, new "Modules" paragraph), `.claude/skills/codebase-review/SKILL.md` (the Kubriko Creator bullet), `creator/plan/HANDOVER.md` (the integration test command under Working with the live app)

## Problem

`:creator` is one Gradle module of ~600 main files. The user decided a full staged split, app staying at `:creator` so
`./gradlew :creator:run` keeps working (decisions.md). M01's `PackageLayeringTest` holds the packages to the module layers; M02a/b
removed the generated `Res` from everything but the UI. `foundation` (`AtomicFileWrites`, `CreatorConfiguration`,
`EditorMutation`, `LatestRequests`, `SettingsDirectory`, `UnexpectedProblemHandler`, M02b's `DomainText`, K11a's `JsonFileStore`,
K11b's `HttpDownloads`, S01's `CreatorLibraries`) and `console`
(`ConsoleEntry`) import nothing else of Creator, so they go first, and this plan also builds what every later module needs.

What depends on `:creator` being one module today:
- `creator/build.gradle.kts` generates `creator.properties` (read by `foundation/CreatorConfiguration.kt` from the classpath root):
  ```kotlin
  val generateCreatorProperties = tasks.register("generateCreatorProperties") { … }
  sourceSets.main {
      resources.srcDir(generateCreatorProperties)
  }
  ```
- S01 added a second generated resource next to it, `generateCreatorLibraries` (`creator-libraries.txt`: the versions of every
  external module on the app's `runtimeClasspath`), read by `foundation/CreatorLibraries.kt`. It describes the whole app's runtime
  classpath, so it stays in `creator/build.gradle.kts`; generated in foundation it would list only foundation's few libraries.
- CI runs one task: `.github/workflows/tests.yml:69` `./gradlew spotlessCheck desktopTest :creator:test --continue --no-configuration-cache`,
  `:92` `./gradlew :creator:test --continue --no-configuration-cache`, and uploads `path: 'creator/build/reports/tests/'` (`:98`).
- Source-reading tests walk relative paths: `CommandRegistryTest.kt:70` `File("src/main/kotlin/com/pandulapeter/kubrikoCreator")`,
  `StringResourcesTest`'s `File("src/main/kotlin")`, `PackageLayeringTest`, `DomainTextsTest`.
- Docs name `./gradlew :creator:test` as "the unit tests" (`CLAUDE.md:30`, `creator/CLAUDE.md:25`, the codebase-review skill).

## Fix

**1. Convention plugin `kubriko-creator-module`** (`CreatorModulePlugin`, registered in `gradle/build-logic/build.gradle.kts` as
`register("creator-module") { id = "kubriko-creator-module"; implementationClass = "…plugins.CreatorModulePlugin" }`), applied by
every `:creator:*` child module, not by the app:

```kotlin
/**
 * A module of Kubriko Creator (a plain JVM library the `:creator` app depends on): Kotlin with serialization, the style check,
 * test fixtures, and explicit API mode, so that what a module offers the others is written `public` on purpose.
 */
class CreatorModulePlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply(libs.pluginId("kotlin-desktop"))
            apply(libs.pluginId("kotlin-serialization"))
            apply("java-test-fixtures")
        }
        pluginManager.apply(StylePlugin::class.java)
        extensions.configure<KotlinJvmProjectExtension> { explicitApi() }
        extensions.configure<BasePluginExtension> { archivesName.set("kubriko-creator-${project.name}") }
        dependencies {
            add("implementation", libs.findLibrary("kotlinx-coroutines").get())
            add("implementation", libs.findLibrary("kotlinx-serialization").get())
            add("testImplementation", libs.findLibrary("kotlin-test").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
            // A module's own implementation dependencies don't reach its test fixtures (they see only its API).
            listOf("kotlinx-coroutines", "kotlinx-serialization", "kotlin-test", "kotlinx-coroutines-test").forEach {
                add("testFixturesImplementation", libs.findLibrary(it).get())
            }
        }
        tasks.withType<Test>().configureEach {
            systemProperty(CREATOR_DIRECTORY_PROPERTY, rootProject.file("creator").absolutePath)
        }
    }
}
```

- No `jvmToolchain`: the app sets none and compiles with the daemon's JDK 21 (`gradle/gradle-daemon-jvm.properties`); a module
  on another JDK could produce bytecode the app can't load.
- `archivesName` keeps the app's packaged `lib/` free of bare names like `core.jar` (Compose's distribution copies every runtime jar).
- `explicitApi()`: Creator's code writes `internal` on every top-level declaration (only `fun main` relies on the default), so
  explicit API mode costs nothing until something is made `public`, and then asks for the modifier on each public member and an
  explicit return type on each public function, which is the "deliberate module API" the split wants. Data class properties,
  primary constructors, and overrides are exempt by Kotlin's rules.
- The plugins are found by ID like `ComposeLibraryPlugin` finds Compose: the root `build.gradle.kts` puts them on the classpath
  (`alias(libs.plugins.kotlin.desktop) apply false`, `alias(libs.plugins.kotlin.serialization) apply false`).
- `CREATOR_DIRECTORY_PROPERTY = "kubrikoCreator.directory"`, a `const val` in the same file.

`gradle/build-logic/CLAUDE.md`: a row in the convention plugins table, and in the `kubriko-style` row "every other module (the
root, `creator`, the `app` modules)" stays true since the app applies it itself.

**2. `:creator:foundation`.** `settings.gradle.kts`: add `":creator:foundation",` right after `":creator",`. New
`creator/foundation/build.gradle.kts` (license header like every build file):

```kotlin
plugins {
    id("kubriko-creator-module")
}

/**
 * The versions Kubriko Creator writes into the projects it creates … (moved verbatim from creator/build.gradle.kts, KDoc included).
 */
val generateCreatorProperties = tasks.register("generateCreatorProperties") { … }

sourceSets.main {
    resources.srcDir(generateCreatorProperties)
}
```

`rootProject.version`, `libs.versions.*`, and `gradle.startParameter` read the same from here. Remove both blocks from
`creator/build.gradle.kts` (leaving S01's `generateCreatorLibraries` and its `resources.srcDir` there), and add
`implementation(projects.creator.foundation)`. `CreatorLibraries` reads its resource through its class loader at run time, where the
app's and foundation's jars share one class path, so it keeps working; but `CreatorLibrariesTest.creatorsOwnLibrariesAreBundled`
(it calls `CreatorLibraries.load()`) only passes where the resource is generated, so that case stays in the app's tests
(`creator/src/test/kotlin/…/foundation/CreatorLibrariesBundlingTest.kt`, same package), while the parsing cases move with the
rest of `foundation`'s tests.

`git mv` the two packages and their tests (package names don't change, so no import changes). Then make public what other code
uses (all of it, today): change `internal` to `public` on each top-level declaration, add `public` to their public members
(`CreatorConfiguration`'s properties, `ConsoleEntry`'s nested classes, …) and the return types explicit API mode asks for; keep
`private` and truly internal helpers `internal`. Every public declaration keeps or gets one KDoc sentence (most have one).

**3. Test fixtures and the source walk.** `creator/foundation/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/testing/CreatorSources.kt`:

```kotlin
/**
 * The `src/main/kotlin` folder of the app and of every Kubriko Creator module, for tests that read the sources.
 */
public fun creatorSourceRoots(): List<File> {
    val creator = File(checkNotNull(System.getProperty("kubrikoCreator.directory")) { "Run through Gradle, which sets kubrikoCreator.directory." })
    val modules = creator.listFiles().orEmpty().filter { File(it, "build.gradle.kts").isFile }
    return (listOf(creator) + modules).map { File(it, "src/main/kotlin") }.filter { it.isDirectory }
        .also { check(it.isNotEmpty()) { "No sources under ${creator.absolutePath}" } }
}
```

plus `creatorTestRoots()` the same for `src/test/kotlin` and `src/testFixtures/kotlin`. Every helper of the app's `testing`
package that needs nothing of Creator moves here too, made `public` (with explicit return types): T01's `TestDirectories.kt`,
T02a's `BlockingTest.kt`, `Awaiting.kt`, and `IdleTrackingDispatcher.kt`, and T02b's `TestPorts.kt` (`grep -L
"com.pandulapeter.kubrikoCreator\." creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/testing/*.kt` lists them; their own tests,
`IdleTrackingDispatcherTest` and `TestPortsTest`, go to `creator/foundation/src/test/kotlin/…/testing/`). Every later module reaches
them through `testFixtures(projects.creator.foundation)`, so the ai and mcp tests M12 moves still find them. The fakes stay in the
app's `testing` package until their module is extracted (M07 core, M08 gradle-session, M09 git, M12 ai). The app's `creator/build.gradle.kts` gains `testImplementation(testFixtures(projects.creator.foundation))`
and `tasks.test { systemProperty("kubrikoCreator.directory", projectDir.absolutePath) }`.

`PackageLayeringTest`, `CommandRegistryTest.onlyCommandsChangeTheProject`, and `DomainTextsTest.everyDomainTextEnumIsListed` walk
`creatorSourceRoots()` (and the layering test `creatorTestRoots()` too) instead of a relative path, keeping their
`isDirectory` assertions through `creatorSourceRoots`'s `check`. `StringResourcesTest` keeps its app-relative paths (the strings
and their users are the app's until M04). `PackageLayeringTest` additionally asserts that every file under
`creator/foundation/src` is in the `foundation` layer (a file's layer must match its module from now on).

**4. `:creator:allTests`.** In `creator/build.gradle.kts`:

```kotlin
// Every Creator module's unit tests: :creator:test runs only the app's.
tasks.register("allTests") {
    group = "verification"
    dependsOn(tasks.test)
    dependsOn(subprojects.map { "${it.path}:test" })
}
```

`.github/workflows/tests.yml` (line numbers are from `cbf3b7140`; T04 added a nightly step and T06 an ABI check task to the first
line, so find the lines by content and keep what they added): in both `./gradlew … :creator:test --continue …` runs replace
`:creator:test` with `:creator:allTests` (the first becomes `./gradlew spotlessCheck desktopTest :creator:allTests
:tools:creator-api:<T06's check task> --continue --no-configuration-cache`), and the report upload's `path: 'creator/build/reports/tests/'`
becomes `path: 'creator/**/build/reports/tests/'` (the app's report folder matches too). If T04 added a nightly job that runs `:creator:test --tests
"*ProjectTemplateIntegrationTest*"`, leave it until M10 moves that test.

**5. Docs.**
- `CLAUDE.md` Build, run, test: replace the `:creator:test` line with
  `./gradlew :creator:allTests           # Run every Kubriko Creator module's unit tests (plain JVM modules, so not part of desktopTest); :creator:<module>:test runs one`
  and keep the `KUBRIKO_CREATOR_INTEGRATION` comment under it. Testing paragraph: "`Tests` … runs `desktopTest` and
  `:creator:allTests`". Conventions → Convention plugins: name `kubriko-creator-module` for Creator's modules. Module layout →
  `creator/`: "split into Gradle modules under `creator/` (`creator/CLAUDE.md` → Modules); the app is `:creator`".
- `creator/CLAUDE.md`: Running and testing, "`./gradlew :creator:allTests` runs every module's unit tests; `:creator:<module>:test
  --tests …` runs one module's." New short **Modules** paragraph (a few sentences, within the ~4,000-word budget): the module
  list in M01's layer order with what is extracted so far (`foundation`), that each child applies `kubriko-creator-module`
  (explicit API: what another module uses is `public`, the rest stays `internal`), that shared test helpers live in a module's
  `src/testFixtures` (`testImplementation(testFixtures(projects.creator.<module>))`), and that `PackageLayeringTest` keeps
  packages in their module's layer. Later M plans extend the list rather than adding sentences.
- `.claude/skills/codebase-review/SKILL.md`: "Its tests are `./gradlew :creator:test`, a plain JVM test task that `desktopTest`
  doesn't run" → "Its tests are `./gradlew :creator:allTests` (every Creator module; `desktopTest` doesn't run them)".
- `creator/plan/HANDOVER.md`: nothing changes until M10 (the integration test still lives in the app).

## Tests

- The moved foundation tests pass in `:creator:foundation:test` unchanged (`CreatorConfigurationTest` reads `creator.properties`
  generated by the moved task).
- New `creator/foundation/src/test/kotlin/com/pandulapeter/kubrikoCreator/testing/CreatorSourcesTest.kt`:
  `findsTheAppAndEveryModule` asserts `creatorSourceRoots()` contains `<creator>/src/main/kotlin` and
  `<creator>/foundation/src/main/kotlin` (as `File`s, for Windows), and `failsWithoutTheProperty` clears the property in a
  `try/finally` and asserts the `IllegalStateException` message.
- `PackageLayeringTest.everyModuleHoldsOnlyItsLayer` (new): fails if a file under `creator/foundation/src` belongs to another layer.
- Run `./gradlew :creator:allTests spotlessCheck` and confirm the test count equals the count before the move (the report's
  total under `creator/**/build/reports/tests/`); a lower count means a test task didn't run.

## Manual check

`./gradlew :creator:run` starts Creator, and the New project screen's Kubriko version is this repository's (`creator.properties`
is found from the foundation jar); `./gradlew :creator:createDistributable` builds, and its `lib/` folder holds
`kubriko-creator-foundation.jar`.
