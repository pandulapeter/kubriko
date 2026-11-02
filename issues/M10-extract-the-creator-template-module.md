<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's project templates and upgrades (`template`, with their resources) into `:creator:template`, and point the integration test's commands at it

**Challenged:** amended — the template tests also need core's test fixtures (the every-Block definitions the integration test builds from) and gradle-session's test-only names made public.

**Kind:** architecture  ·  **Severity:** low  ·  **Platforms:** all
**Findings:** Modularization proposal (template; the verifier added it to the order: after git, before commands)  ·  **Depends on:** M09
**Files:**
- `settings.gradle.kts`; new `creator/template/build.gradle.kts`; `creator/build.gradle.kts`
- moved (main and test): `template/**` → `creator/template/src/{main,test}/kotlin/com/pandulapeter/kubrikoCreator/template/`
- moved resources: `creator/src/main/resources/templates/**`, `creator/src/main/resources/upgrades/**` → `creator/template/src/main/resources/`
- `.github/workflows/tests.yml` (T04's nightly job, if it names the test task)
- docs: `creator/CLAUDE.md` (Running and testing: the opt-in integration test; Modules list), `creator/plan/HANDOVER.md` (Working with the live app: the integration test command), `CLAUDE.md` (the `KUBRIKO_CREATOR_INTEGRATION` comment line, if it names a task)
- `PackageLayeringTest.kt`

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

`template/` (17 files) imports core (`actors`, `project`), `git` (`ProjectGenerator` uses `JGitRepository`), and `foundation`
(`CreatorConfiguration`), plus JGit and the Tooling API directly:

```kotlin
// template/TemplateFiles.kt:27 — the template files are classpath resources
    internal fun readResource(name: String) = TemplateFiles::class.java.classLoader.getResourceAsStream(name)?.use { it.readBytes().decodeToString().replace("\r\n", "\n") }
```

and its tests open the resource folder as a directory, which works only while the resources are a folder of the running module:

```kotlin
// creator/src/test/…/template/TemplateFilesTest.kt:34
        val directory = File(TemplateFiles::class.java.classLoader.getResource(template.root)!!.toURI())
```

The gated integration test `ProjectTemplateIntegrationTest` uses `session.ProjectClasspath` (gradle-session since M08) to resolve
the desktop runtime classpath as playing does. Its documented commands name the app's test task:

```text
creator/plan/HANDOVER.md → Working with the live app:
- The integration test: `KUBRIKO_CREATOR_INTEGRATION=true KUBRIKO_CREATOR_VERSION=0.8.1-creator ./gradlew :creator:test --tests
  "*ProjectTemplateIntegrationTest*" --rerun` …
```

## Fix

1. `settings.gradle.kts`: `":creator:template",`. `creator/template/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
   }

   dependencies {
       api(projects.creator.core)
       implementation(projects.creator.git)
       implementation(libs.gradle.toolingApi)
       implementation(libs.jgit)
       testImplementation(projects.creator.gradleSession)
       testImplementation(testFixtures(projects.creator.core))
       testImplementation(testFixtures(projects.creator.foundation))
   }
   ```
   (`api(projects.creator.git)` instead if a public `template` signature exposes a git type.) The app adds
   `implementation(projects.creator.template)` and drops `libs.jgit`/`libs.gradle.toolingApi` if nothing left in the app imports them.
2. `git mv` the package, its tests, and the two resource folders (`templates/`, `upgrades/`) into `creator/template/src/main/resources/`
   (same relative paths, so `TemplateFiles.readResource` and `TemplateFilesTest`'s `getResource(...).toURI()` keep working: during
   this module's tests the resources are its `build/resources/main` folder).
   `testFixtures(projects.creator.core)` holds `EveryBlockTypes` (M07), which `ProjectTemplateIntegrationTest` generates every
   Block from. That test also uses `GradleProjectConnection`, `CompilationMode`, `SessionInitScript`, and `ProjectClasspath` from
   gradle-session: make public whichever M08 left `internal`.
3. Make public what `commands` (`commands/project/*` upgrade commands, `ProjectNaming`), `workspace`, and the UI (New project)
   use; keep the file-level helpers internal.
4. **Integration test commands** become `./gradlew :creator:template:test --tests "*ProjectTemplateIntegrationTest*" --rerun`:
   `creator/plan/HANDOVER.md` (Working with the live app), `creator/CLAUDE.md` (Running and testing's opt-in tests: say the test
   lives in `:creator:template`), and T04's nightly job in `.github/workflows/tests.yml` if it runs `:creator:test --tests …`.
   `CreatorBuildBenchmarkTest` (app) references the test in KDoc only: update the KDoc link to the class's package if needed.
5. `PackageLayeringTest`: the template layer's files must all be under `creator/template/src`.
6. `creator/CLAUDE.md`: add `template` to the Modules list.

## Tests

- `:creator:template:test` runs the moved tests; `TemplateFilesTest` lists the starter template's files from the module's resources.
- With `KUBRIKO_CREATOR_INTEGRATION` unset the integration test is skipped through `Assume` (TST-4), as before.
- `./gradlew :creator:allTests` with the same test count as before.

## Manual check

`./gradlew :creator:run` → New project → create a project from the starter template: the files are written (the templates
resolve from the template jar) and the project builds.
