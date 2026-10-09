# Log the Annoyed Penguins foreground `ShaderManager` under the foreground tag.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** unpublished (examples)
**Files:** `examples/game-annoyed-penguins/src/commonMain/kotlin/com/pandulapeter/kubriko/gameAnnoyedPenguins/implementation/AnnoyedPenguinsGameStateHolderImpl.kt` (after G01; `AnnoyedPenguinsGameStateHolder.kt:124–129` before it)

## Problem
The foreground shader manager, registered in the main `_kubriko` instance (it drives the pause blur), is created with the background instance's tag:
```kotlin
private val shaderManager by lazy {
    ShaderManager.newInstance(
        isLoggingEnabled = isLoggingEnabled,
        instanceNameForLogging = LOG_TAG_BACKGROUND,
    )
}
```
With logging on (debug-menu builds), its log lines are labelled `AP-Background` and are indistinguishable from `backgroundShaderManager`'s, which sends anyone filtering the debug menu's log by instance to the wrong Kubriko instance.

## Fix
Run after G01. Change `instanceNameForLogging = LOG_TAG_BACKGROUND` to `instanceNameForLogging = LOG_TAG` in `shaderManager` only (`backgroundShaderManager` keeps `LOG_TAG_BACKGROUND`).

## Behaviour
Changes only the instance name in that manager's log entries: `AP-Background` → `AP`. No gameplay change.

## Public API
None.

## Tests
None (a logging label; no logic to test).

## Verify
`./gradlew :examples:game-annoyed-penguins:compileKotlinDesktop`

## Manual check
With `showcase.isDebugMenuEnabled=true`, open Annoyed Penguins, pause a level and check that the shader manager's log entries in the debug menu carry the `AP` source.
