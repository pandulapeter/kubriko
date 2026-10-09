# Drop the never-passed name and suffix parameters, and their label, from EditorSlider

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorSlider.kt

## Problem
`internal fun EditorSlider(modifier, name: String = "", suffix: String = "", value, onValueChanged, valueRange, enabled)`
has two callers, both in `components/EditorNumberInput.kt` (the two `EditorSlider(...)` calls), and neither passes `name`
or `suffix`. So the block
```kotlin
    if (name.isNotBlank()) {
        EditorTextLabel(
            text = "$name: ${"%.2f".format(value)}$suffix",
        )
    }
```
never runs.

## Fix
Remove the `name` and `suffix` parameters and that `if` block. Keep the `Column(modifier = modifier.padding(bottom = 4.dp))`
wrapper (layout unchanged). Leave `fun Float.toDifference()` untouched — it is public and covered by its own decision plan.
`EditorTextLabel` stays (other callers).

## Behaviour
The removed branch was unreachable; the rendered slider is identical.

## Public API
None (`EditorSlider` is internal; `toDifference` is not touched).

## Tests
The existing ones.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
