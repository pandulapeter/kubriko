# Preload every entry's title and subtitle and every licence name before the Showcase shows, deriving the lists from the enums

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all (visible on web, where string tables load asynchronously)  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/ResourceLoader.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/LicensesScreenStateHolder.kt` (created by A05)

Depends on **A05** (makes `LicenseType` internal in its own file and moves `areStringResourcesLoaded` to
`LicensesScreenStateHolder.kt`); lands after every move in the lane.

## Problem
The loading gate (`KubrikoTheme(areResourcesLoaded = ResourceLoader.areResourcesLoaded() && …)`) is meant to hold the UI
until every string the first screens show has loaded, but its hand-written lists miss some:

- `ResourceLoader.areStringResourcesLoaded()` (`ResourceLoader.kt:97-135` at 2480325f) lists the title and subtitle of
  every entry except `ISOMETRIC_GRAPHICS` — `demo_isometric_graphics` / `demo_isometric_graphics_subtitle` are absent
  between `demo_content_shaders_subtitle` and `demo_particles`, though the menu shows that entry.
- `LicensesScreenStateHolder.areStringResourcesLoaded()` (`LicensesScreen.kt:271-277`) lists every `LicenseType` name
  except `other_licenses_ccby_4_0`, though the Licenses screen renders it as a heading (`stringResource(type.licenseName)`).

On web, where `stringResource` resolves asynchronously, the gate can open with those strings still empty, so the menu
row / heading renders blank for a frame or more. Every new entry or licence must be remembered in two places.

## Fix
Derive the per-entry and per-licence parts from the enums, keeping the `&&` short-circuit and the existing order (the
gate asks for the next resource only once the previous one has loaded, which is what spreads the requests out):

In `ResourceLoader.areStringResourcesLoaded()`, replace the 28 lines from `game_wallbreaker` through
`other_about_subtitle` with

```kotlin
&& ShowcaseEntry.entries.all { showcaseEntry ->
    preloadedString(showcaseEntry.titleStringResource).value.isNotBlank()
            && preloadedString(showcaseEntry.subtitleStringResource).value.isNotBlank()
}
```

keeping the lines before (`kubriko_showcase` … `welcome_subtitle`) and after (`welcome_disclaimer`) as they are. The enum
order (WALLBREAKER, SPACE_SQUADRON, ANNOYED_PENGUINS, BLOCKYS_JOURNEY, CONTENT_SHADERS, ISOMETRIC_GRAPHICS, PARTICLES,
PERFORMANCE, PHYSICS, SHADER_ANIMATIONS, AUDIO, COLLISION, INPUT, LICENSES, ABOUT) is exactly today's order with the
isometric pair inserted. Unavailable entries keep being preloaded, as today. Remove the per-entry string imports that
become unused; add `ShowcaseEntry`'s import.

In `LicensesScreenStateHolder.areStringResourcesLoaded()`, replace the five licence lines (`apache_2_0` … `mpl_2_0`) with

```kotlin
&& LicenseType.entries.all { preloadedString(it.licenseName).value.isNotBlank() }
```

keeping `other_licenses_content` first and `other_licenses_music_note` last. `LicenseType`'s order (APACHE_2_0, CC0_1_0,
CCBY_4_0, LGPL_2_1, MIT, MPL_2_0) is today's with CC BY inserted. Drop the now unused licence string imports.

`all` is inline, so calling `preloadedString` (a Composable) inside its lambda is allowed; it stops at the first unloaded
string exactly like the `&&` chain, and the calls happen at stable positions because the enum's order never changes at
runtime.

The category titles (`demos`, `games`, `tests`, `other`) stay hand-listed in their current order — they are already
complete, and deriving them from `ShowcaseEntryType.entries` would reorder them.

## Behaviour
The gate additionally waits for the three strings it missed; everything else is requested in the same order and with
the same short-circuit. Nothing else changes.

## Public API
None.

## Tests
None can run the gate itself (it is a Composable reading Compose resources, which need a composition and the resource
reader). The derivation is over enum entries, so completeness follows by construction.

## Verify
`./gradlew :app:shared:compileKotlinDesktop :app:web:compileKotlinWasmJs`

## Manual check
`./gradlew :app:web:wasmJsBrowserDevelopmentRun` with the network throttled ("Slow 4G"): after the loading screen, the
"Isometric Graphics" menu row and, on the Licenses screen, the "CC BY 4.0" heading are filled in on the first frame.
