# List the runtime dependencies the Licenses screen misses (Jamepad and its SDL2 natives, `jbr-api`, NavigationEvent)

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all (Jamepad / JBR: desktop)  ·  **Class:** Planned
**Artifact:** unpublished (app)
**Files:**
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/Dependency.kt`
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/LicenseType.kt` (only if a new licence type is needed)
- `app/shared/src/commonMain/composeResources/values/strings.xml` (only for a new licence type)
- `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/implementation/ui/licenses/LicensesScreenStateHolder.kt` (nothing to do: `:32` already preloads every `LicenseType.entries`)

**Rebased:** on 70de96c6 after the Now plans landed.

A05 (4b68a776, the split into `Dependency.kt` / `LicenseType.kt` / `LicensesScreenStateHolder.kt`) and A12 (4c985b69, the
Licenses gate derived from `LicenseType.entries`) have landed.

## Problem
The Licenses screen is the Showcase's attribution for what it ships. At 70de96c6 its 23 `Dependency` entries (`Dependency.kt`) do not
include, from `gradle/libs.versions.toml`:
- `com.badlogicgames.jamepad:jamepad` (used by `plugins/gamepad-input` on desktop, which `examples/test-input`
  depends on) — it bundles SDL2 native libraries, which carry their own licence (zlib);
- `org.jetbrains.runtime:jbr-api` (`app/desktop`), and the desktop distributions bundle the JetBrains Runtime itself
  (`javaHome` in `app/desktop/build.gradle.kts`);
- `org.jetbrains.androidx.navigationevent:navigationevent-compose` (`app/shared`), not covered by the listed AndroidX
  Activity / Core Splash Screen / Lifecycle entries.

## Fix
For each candidate, verify at the source which licence applies and whether the artifact ships in a Showcase build
(the test examples are swapped for `-noop` blanks when `showcase.areTestExamplesEnabled=false`, which drops Jamepad from
those builds): Jamepad (github.com/libgdx/Jamepad — expected Apache 2.0), SDL2 (expected zlib), `jbr-api`
(github.com/JetBrains/JetBrainsRuntimeApi — check), the JetBrains Runtime (expected GPLv2 with the Classpath Exception),
NavigationEvent (expected Apache 2.0, AndroidX). Add one `Dependency` entry per shipped item with its licence URL, in the
group of its `LicenseType`; add a `LicenseType` (and its `other_licenses_<id>` string) only for a licence not yet
listed (zlib; GPLv2+CPE). The gate's derivation from `LicenseType.entries` (A12, 4c985b69) preloads any new `LicenseType` automatically.

## Decision
Whether to list items that ship only in some builds (Jamepad only with the test examples) unconditionally or behind the
same flag. **Recommended: unconditionally** — the screen is static attribution and over-attribution is harmless.

## Behaviour
The Licenses screen shows the added entries.

## Public API
None.

## Tests
None (static data).

## Verify
`./gradlew :app:shared:compileKotlinDesktop`

## Manual check
Open Licenses on desktop: the new entries appear under the right headings and their links open.
