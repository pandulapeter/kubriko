# Drop the unused debug-menu dependency from `game-blockys-journey`.

**Kind:** build  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-blockys-journey/build.gradle.kts`

## Problem
`examples/game-blockys-journey/build.gradle.kts:29` declares
```kotlin
implementation(if (project.findProperty("showcase.isDebugMenuEnabled") == "true") projects.tools.debugMenu else projects.tools.debugMenuNoop)
```
but no source file in the module references the debug menu (grep `examples/game-blockys-journey/src` for `debugMenu` / `DebugMenu`: no hits). The Showcase wires the debug menu itself in `app/shared`.

## Fix
Delete that line. Nothing else depends on it transitively: `tools/debug-menu` and `tools/debug-menu-noop` expose only `api(projects.tools.debugMenuApi)`, which exposes `api(projects.engine)`, and the module already declares `implementation(projects.engine)`. (Annoyed Penguins and Blocky's Journey keep their scene-editor line; it is used.)

## Behaviour
None at runtime; the Showcase still gets the debug menu through `app/shared/build.gradle.kts:44`.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :examples:game-blockys-journey:compileKotlinDesktop :examples:game-blockys-journey:compileKotlinWasmJs` and `./gradlew :app:desktop:compileKotlin` (run once with `-Pshowcase.isDebugMenuEnabled=true` as well, since that is the configuration the dependency was switched for).

## Manual check
none
