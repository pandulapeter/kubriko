<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's Block model, code generation, type store, project files, schema annotations, and Scene files into `:creator:core`

**Challenged:** amended — moves G01's and G02's test resources and the `.gitattributes` line T05 added with T05's fixtures; puts the every-Block definitions in core's test fixtures, since the integration test stays in the app until M10; compiles every dependent test source set when finding what to make public.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** Modularization proposal (core = actors + project + schema + Scene files; ARCH-7's "core needs its own Res" is void after M02a/b)  ·  **Depends on:** M06
**Files:**
- `settings.gradle.kts`; new `creator/core/build.gradle.kts`; `creator/build.gradle.kts`
- moved (main and test): `actors/**` (with `actors/codegen`, `actors/store`, and the package guide `actors/CLAUDE.md`), `project/**`, `schema/**`, `scenes/**` (only the Scene files are left there after M06) → `creator/core/src/{main,test}/kotlin/com/pandulapeter/kubrikoCreator/…`
- every test resource of core's tests → `creator/core/src/test/resources/…`: T05's golden format fixtures (`formats/v*/**`), G01's
  `formats/persisted-shape-*.txt`, and G02's generated-code golden files (`generated/*.kt.txt`); at this point all of
  `creator/src/test/resources/` belongs to core (check with `ls`)
- `.gitattributes` (T05's line `creator/src/test/resources/formats/** text eol=lf`, and any other line naming `creator/src/test/resources`)
- the every-Block definitions (G02's `actors/EveryBlockTypes.kt` or T05's `testing/EveryBlockTypes.kt`, whichever landed) →
  `creator/core/src/testFixtures/kotlin/…` (same package)
- new `creator/core/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/testing/` (the fakes of core interfaces from T01's `testing` package, at least `FakeBlockTypeRepository`)
- every file outside core that uses a core declaration (no import changes; visibility only)
- `PackageLayeringTest.kt`
- docs: `creator/CLAUDE.md` (the intro's path to the actors guide; Modules list), `.claude/skills/codebase-review/SKILL.md` (only if it quotes `creator/src/main/kotlin/…/actors`)

Paths are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` unless they start with `creator/`.

## Problem

After M06, `PackageLayeringTest` reports no upward import, and the core layer (`actors`, `actors.codegen`, `actors.store`,
`project`, `schema`, `scenes`) imports only `foundation` among Creator's packages (at `ecf76faf6`, 12 imports of
`foundation`/`console`; `ProjectInspector`'s Tooling API part moved to `gradle` in M06). It is the layer most code uses (241 imports
from `commands` alone), so it is the split's largest visibility change. Its external libraries at `ecf76faf6`:
`kotlinx.serialization` (`json`, `descriptors`, `encoding`), `kotlinx.coroutines` (`ProjectWatcher`, `BlockTypeStore`), and
`androidx.compose.ui.input.key.Key` in `actors/ComposeKeys.kt` (the key names the key picker offers):

```kotlin
// actors/ComposeKeys.kt
import androidx.compose.ui.input.key.Key
```

(no `@Composable` code: only generated-code text mentions Compose). After M02a/b no core file imports
`kubriko.creator.generated.resources` (check: `grep -rln "generated.resources" actors project schema scenes` prints nothing; if
something does, finish M02 for it first).

## Fix

1. `settings.gradle.kts`: `":creator:core",` after `":creator:foundation",`. `creator/core/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
   }

   dependencies {
       api(projects.creator.foundation)
       implementation(libs.compose.ui)
       testImplementation(testFixtures(projects.creator.foundation))
   }
   ```
   (`libs.compose.ui` resolves the desktop variant through Gradle metadata; a library doesn't need the Compose plugin to use the
   `Key` value class. If `ComposeKeys` is used only by the UI and the generators at execution, the same holds.) The app gets
   `implementation(projects.creator.core)` and `testImplementation(testFixtures(projects.creator.core))`.
2. **Move** with `git mv` (packages unchanged, so no import rewrites), including `actors/CLAUDE.md` and the tests of every moved
   package. The test resources (T05's `formats/v*/`, G01's `formats/persisted-shape-*.txt`, G02's `generated/*.kt.txt`) move to
   `creator/core/src/test/resources` under the same relative paths, so tests that load them from the classpath keep working, and a
   test that opens or (with `KUBRIKO_CREATOR_UPDATE_GOLDEN=true`) writes them by `File("src/test/resources/…")` resolves against
   the module folder now, which is the same relative path. Update the root `.gitattributes` line to
   `creator/core/src/test/resources/formats/** text eol=lf` (and any other line naming the old folder): without it a Windows
   checkout gets CRLF fixtures and the byte-for-byte test fails in `Tests` on Windows only.
   The every-Block definitions (`EveryBlockTypes`, G02/T05) are used by core's golden and format tests and by
   `ProjectTemplateIntegrationTest`, which stays in the app until M10: move them to `creator/core/src/testFixtures/kotlin/…`
   (same package, `public`), which the app reaches through `testFixtures(projects.creator.core)`.
3. **API.** Compile the app and every test and test-fixture source set that uses core (`./gradlew :creator:allTests` compiles
   them all; the app's tests use core internals too); for each "cannot access … internal" error, make that declaration `public` (top-level declarations,
   their public members, nested classes) and add what explicit API mode asks. Keep `internal` what only core uses (most of
   `actors.codegen`'s helpers, the store's internals). Prefer exposing a function over a mutable property. The `@EditorMutation`
   opt-in on `SceneStore`'s and the store's writers stays; it is `public` since M03. Every newly `public` declaration has a KDoc
   sentence (most already do).
4. **Fixtures.** Move the fakes of core interfaces from the app's `testing` package (T01) into `creator/core/src/testFixtures/…/testing/`
   (at least `FakeBlockTypeRepository`; T01's `BlockTypeRepositoryContract` test, which runs against both the fake and
   `BlockTypeStore`, moves into core's tests), made `public`. Fakes of higher modules stay in the app.
5. **Path-coupled tests.** None of core's tests read Creator's own sources at `ecf76faf6` (`grep -rln 'File("src' creator/core/src/test`
   prints nothing); if one does at execution, use `creatorSourceRoots()` from foundation's fixtures.
6. `PackageLayeringTest.everyModuleHoldsOnlyItsLayer` now covers `creator/core/src`.
7. **Docs.** `creator/CLAUDE.md`: the intro's guide path becomes `core/src/main/kotlin/com/pandulapeter/kubrikoCreator/actors/CLAUDE.md`;
   add `core` to the Modules list. Run `check_doc_names.py --staged` and `check_license_headers.py` (the guide keeps its header).

## Tests

- Every moved test passes in `:creator:core:test`; the golden format fixtures still decode and re-encode byte for byte.
- `./gradlew :creator:allTests` with the same test count as before.
- `./gradlew :creator:core:dependencies --configuration runtimeClasspath` shows no Tooling API, JGit, Ktor, MCP, ACP, or engine
  artifact (core is the model; say so in the commit's report).
