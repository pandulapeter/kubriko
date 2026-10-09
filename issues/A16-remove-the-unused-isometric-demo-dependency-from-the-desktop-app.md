# Remove the unused `demo-isometric-graphics` dependency from the desktop app's build file

**Kind:** build  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/desktop/build.gradle.kts`

## Problem
`app/desktop/build.gradle.kts:21` (at 2480325f) declares `implementation(projects.examples.demoIsometricGraphics)`. The
desktop module depends directly on the examples whose desktop-only scene editors it opens
(`AnnoyedPenguinsGameSceneEditor`, `BlockysJourneyGameSceneEditor`, `PerformanceDemoSceneEditor`,
`PhysicsDemoSceneEditor`); the isometric demo's scene editor was removed, and nothing under `app/desktop/src` references
`com.pandulapeter.kubriko.demoIsometricGraphics` any more (grep). The leftover line suggests a direct use that does not
exist.

## Fix
Delete the line. Nothing else.

## Behaviour
`app/shared` still declares `implementation(projects.examples.demoIsometricGraphics)` in `commonMain`, and an
`implementation` dependency of a dependency stays on the consumer's runtime classpath, so the demo is still compiled,
packaged and ProGuarded into the desktop app. The desktop sources never compiled against it. Unchanged.

## Public API
None.

## Tests
None.

## Verify
`./gradlew :app:desktop:compileKotlin` and
`./gradlew :app:desktop:dependencies --configuration runtimeClasspath | grep demo-isometric-graphics` (still listed, via
`:app:shared`).

## Manual check
None (if convenient, `./gradlew :app:desktop:run` and open the Isometric Graphics demo).
