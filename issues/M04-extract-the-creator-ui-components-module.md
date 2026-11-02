<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# Extract Kubriko Creator's shared UI components and theme into `:creator:ui-components`, with its own resources and the offscreen UI test helpers as its test fixtures

**Challenged:** amended — resolves every `knownExceptions` entry M01 tagged M04 (components lanes H and M02b added since the review), not only the three named.

**Kind:** architecture  ·  **Severity:** medium  ·  **Platforms:** all
**Findings:** Modularization proposal (ui-components; the `ui.theme -> settings.ThemeChoice` blocker the verifier found)  ·  **Depends on:** M03
**Files:**
- `settings.gradle.kts`; new `creator/ui-components/build.gradle.kts`; `creator/build.gradle.kts`
- moved: `ui/components/**` (32 files) and `ui/theme/**` (2) with their tests (`ui/components/*Test.kt`, 5 files) → `creator/ui-components/src/{main,test}/kotlin/com/pandulapeter/kubrikoCreator/ui/{components,theme}/`
- moved and changed: `window/ProvideInterfaceScale.kt` → `ui/theme/ProvideInterfaceScale.kt`; `settings/ThemeChoice.kt` → `ui/theme/ThemeChoice.kt`;
  `actors/ColorHex.kt` (and its test, if any) → `creator/foundation/src/main/kotlin/com/pandulapeter/kubrikoCreator/foundation/ColorHex.kt`
- `settings/CreatorSettings.kt`, `settings/CreatorSettingsController.kt`, `ui/settings/CreatorSettingsDialog.kt`, `actors/HexColorSerializer.kt`, `context/TypeNotation.kt`, `ui/ProjectSidebar.kt`, `KubrikoCreatorApp.kt`, `window/*` (imports)
- moved: `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/UiSnapshots.kt` → `creator/ui-components/src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/testing/UiSnapshots.kt`; every UI test's import of it
- resources: new `creator/ui-components/src/main/composeResources/{values/strings.xml,drawable/,font/}`; `creator/src/main/composeResources/{values/strings.xml,drawable/,font/}`
- `creator/src/test/kotlin/com/pandulapeter/kubrikoCreator/ui/StringResourcesTest.kt` → a shared check in the fixtures, used by a test in each module
- `PackageLayeringTest.kt` (remove the M04 entries of `knownExceptions`)
- docs: `creator/CLAUDE.md` (Modules list; Running and testing's `ui/UiSnapshots.kt` and `build/ui-snapshots` sentence; the Definition of done names `BufferedTextField`, … without paths, so it stays)

Paths without a module prefix are below `creator/src/main/kotlin/com/pandulapeter/kubrikoCreator/` (the app) at the start.

## Problem

`ui/components` and `ui/theme` are the second-lowest layer (M01) and are used by every screen. At `ecf76faf6` they import only
three things above them:

```kotlin
// ui/theme/CreatorTheme.kt
import com.pandulapeter.kubrikoCreator.settings.ThemeChoice
// ui/components/ColorField.kt, ui/components/ColorPickerValue.kt
import com.pandulapeter.kubrikoCreator.actors.ColorHex
```

and their tests use the app's test helpers (`ui/UiSnapshots.kt`'s `uiTest`, `showing`, `saveSnapshot`), which call
`window.ProvideInterfaceScale`, whose default reads `settings.CreatorSettings.DEFAULT_INTERFACE_SCALE`:

```kotlin
internal fun ProvideInterfaceScale(
    scale: Float = CreatorSettings.DEFAULT_INTERFACE_SCALE,
```

They also use the app's generated `Res`: four fonts (`Res.font.public_sans_*` in `CreatorTypography`), the drawables `ic_check`,
`ic_chevron`, `ic_close`, and the strings `action_add`, `action_cancel`, `action_done`, `actor_type_color`, `color_picker_*` (6),
`description_hint`, `description_hint_without_ai`, `description_label` (re-check with
`grep -rhoE "Res\.(string|drawable|font)\.[a-z0-9_]+" ui/components ui/theme | sort -u` at execution; H-lane plans added components).

## Fix

1. **Remove the blockers first (still in the app):**
   - `ColorHex` → `foundation` (module `:creator:foundation`, package `foundation`), `public`; update its four importers.
   - `ThemeChoice` → `ui/theme/ThemeChoice.kt` (package `ui.theme`); `settings/*` import it from there (settings is app, above).
   - `ProvideInterfaceScale` → `ui/theme/`, with `const val DEFAULT_INTERFACE_SCALE = 0.85f` moving beside it as a top-level
     `public const val` in `ui/theme/InterfaceScale.kt`; `CreatorSettings` refers to it (`interfaceScale: Float = DEFAULT_INTERFACE_SCALE`,
     and its `interfaceScales` list), so the serialized default is unchanged.
   - Lane H added components (H05's `SearchableChoiceButton`/`ChoiceMatching`, H06b's `PanelSplitter`/`PanelWidthLimits`, H06b's
     `PanelLayout` if it was put in `ui/components`). Run `PackageLayeringTest` and fix every upward import out of
     `ui/components`/`ui/theme` that M01 listed under M04 in the same way: a component that reads Creator settings, a model type, or a
     command takes the value or a callback as a parameter, and the app passes it in (or a type it needs moves down to `foundation`,
     as `ColorHex` does). Name each one in the commit's report.
2. **The module.** `settings.gradle.kts`: `":creator:ui-components",` after `":creator:foundation",`. `creator/ui-components/build.gradle.kts`:
   ```kotlin
   plugins {
       id("kubriko-creator-module")
       alias(libs.plugins.compose)
       alias(libs.plugins.compose.compiler)
   }

   dependencies {
       api(projects.creator.foundation)
       api(projects.tools.uiComponents)
       api(compose.desktop.currentOs)
       implementation(libs.compose.resources)
       testFixturesApi(compose.desktop.uiTestJUnit4)
       testFixturesApi(libs.kotlin.test)
   }

   compose.resources {
       packageOfResClass = "com.pandulapeter.kubrikoCreator.uiComponents.generated.resources"
       publicResClass = false
   }
   ```
   (Use the plugin aliases the app's build uses; `compose.desktop.currentOs` mirrors the app's dependency. If the app's material3
   comes from somewhere else today, add exactly what the moved files need, nothing more.) In `creator/build.gradle.kts` add
   `implementation(projects.creator.uiComponents)`, `testImplementation(testFixtures(projects.creator.uiComponents))`, and pin the
   app's own resource package so its 200 importers don't move: `compose.resources { packageOfResClass = "kubriko.creator.generated.resources" }`.
   The two packages must differ: on the JVM, Compose packs each module's resources under `composeResources/<package>/`.
3. **Move** `ui/components` and `ui/theme` with `git mv` (packages unchanged), then:
   - **Resources split by owner.** Create the module's `strings.xml` (license header comment like the app's) with exactly the keys
     its sources use, copied verbatim; copy the three drawables and `git mv` the four fonts (only `ui/theme` uses fonts). Rewrite
     the module's `import kubriko.creator.generated.resources.*` lines to the new package. In the app's `strings.xml` and
     `drawable/`, delete what the app's own sources no longer reference; keys both use stay in both (`action_cancel`,
     `action_done`, `ic_close`, … are the app's own words too), identical in text.
   - **API.** Make `public` what the app uses (nearly every component and `CreatorTheme`, `LocalIsAiEnabled`, `PanelWidths`,
     `sidePanel`, …); keep `private`/`internal` what only the module uses. Explicit API mode asks for a return type only on public
     functions and properties with expression bodies (`fun Modifier.sidePanel(…): Modifier = …`); block-bodied Composables need
     none. Each public declaration keeps its KDoc.
4. **Test fixtures.** `git mv` `ui/UiSnapshots.kt` to the module's `src/testFixtures/kotlin/com/pandulapeter/kubrikoCreator/testing/`,
   change its package to `testing`, make `uiTest`, `showing`, `saveSnapshot` and their constants `public`, and import them in every
   UI test (`grep -rln "uiTest\|saveSnapshot\|showing {" creator/src/test`; tests in package `ui` used them without an import).
   Snapshots still go to `build/ui-snapshots` relative to the running module, so the app's land in `creator/build/ui-snapshots`
   and the module's in `creator/ui-components/build/ui-snapshots`.
   Turn `StringResourcesTest`'s body into a fixture `public fun assertEveryStringAndIconIsUsed(module: File)` (reads
   `module/src/main/composeResources/values/strings.xml` and `drawable/`, scans `module/src/main/kotlin`, keeps the
   escaped-apostrophe check and the `isDirectory` assertions) and call it from a `StringResourcesTest` in the app (`File(".")`)
   and one in `:creator:ui-components` (`File(".")`, since each test runs in its module's folder).
5. `PackageLayeringTest`: drop every `knownExceptions` entry tagged M04 (at least `ColorHex`, `ThemeChoice`, and `UiSnapshots`); its
   `everyModuleHoldsOnlyItsLayer` now covers `creator/ui-components/src`.
6. `creator/CLAUDE.md`: add `ui-components` to the Modules list; in Running and testing, "`uiTest { showing { … }; saveSnapshot("name") }`
   (`testing/UiSnapshots.kt` in `:creator:ui-components`' test fixtures) … saves a PNG under the module's `build/ui-snapshots`".
   Mention that each module with resources has its own `strings.xml` and `packageOfResClass`, and `StringResourcesTest` runs per module.

## Tests

- All moved component tests pass in `:creator:ui-components:test`; the app's UI tests pass with the fixture import.
- `StringResourcesTest` in both modules: delete one string from the module's `strings.xml` that a component uses and the module
  doesn't compile; add an unused one and its test fails.
- `CreatorSettingsStoreTest` (or the existing settings test): a settings file without `interfaceScale` still loads as `0.85f`
  (literal), proving the moved default.
- `./gradlew :creator:allTests` total test count equals the count before (plus the new `StringResourcesTest`).

## Manual check

`./gradlew :creator:run`: the window's fonts are Public Sans (a missing font falls back silently), the light and dark themes
follow Creator settings, and the color picker dialog shows its labels.
