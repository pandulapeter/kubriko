# Move the debug menu's inline metadata labels into a parameterized string resource

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** tool-debug-menu (internal code and resources)
**Files:** tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/DebugMenuContents.kt, tools/debug-menu/src/commonMain/composeResources/values/strings.xml

## Problem
`DebugMenuContents.kt:185-189` (private `Metadata`) builds on-screen copy from inline literals, against the user-facing
strings rule:
```kotlin
    text = "Kubriko: ${debugMenuMetadata.kubrikoInstanceName}\n" +
            "FPS: ${debugMenuMetadata.fps.roundToInt()}\n" +
            "Actors: ${debugMenuMetadata.visibleActorWithinViewportCount}/${debugMenuMetadata.totalActorCount}\n" +
            "Play time: ${debugMenuMetadata.playTimeInSeconds}\n" +
            "Viewport size: ${debugMenuMetadata.viewportSize.width.roundToInt()}*${debugMenuMetadata.viewportSize.height.roundToInt()}"
```
(Listed as an unfiled follow-up by the first sweep.)

## Fix
Add to the debug-menu `strings.xml`:
`<string name="debug_metadata">Kubriko: %1$s\nFPS: %2$d\nActors: %3$d/%4$d\nPlay time: %5$d\nViewport size: %6$d*%7$d</string>`
and set `text = stringResource(Res.string.debug_metadata, debugMenuMetadata.kubrikoInstanceName, debugMenuMetadata.fps.roundToInt(), debugMenuMetadata.visibleActorWithinViewportCount, debugMenuMetadata.totalActorCount, debugMenuMetadata.playTimeInSeconds, debugMenuMetadata.viewportSize.width.roundToInt(), debugMenuMetadata.viewportSize.height.roundToInt())`
(import `kubriko.tools.debug_menu.generated.resources.debug_metadata`). The `\n` escape and `%N$d` placeholders are already
used this way elsewhere (`examples/game-space-squadron` `score`).

## Behaviour
Identical text (same labels, separators and numbers).

## Public API
None (the module's `Res` is internal).

## Tests
The existing ones (rendering a resource needs the Compose runtime).

## Verify
`./gradlew :tools:debug-menu:compileKotlinDesktop :tools:debug-menu:compileKotlinWasmJs`

## Manual check
Open the debug menu in the Showcase: the metadata block reads exactly as before.
