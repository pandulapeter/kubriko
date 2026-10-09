# Merge BodyOverlaySwitch and CollisionMaskOverlaySwitch into one private OverlaySwitch

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** tool-debug-menu (internal code only)
**Files:** tools/debug-menu/src/commonMain/kotlin/com/pandulapeter/kubriko/debugMenu/implementation/ui/DebugMenuContents.kt

## Problem
`DebugMenuContents.kt:192-238` holds two private Composables that differ only in the label resource and the flag:
```kotlin
private fun BodyOverlaySwitch(debugMenuMetadata: DebugMenuMetadata, onIsBodyOverlayEnabledChanged: () -> Unit) = Row(
    modifier = Modifier.selectable(selected = debugMenuMetadata.isBodyOverlayEnabled, onClick = onIsBodyOverlayEnabledChanged).padding(start = 8.dp),
    ...
    Text(..., text = stringResource(Res.string.body_overlay), ...)
    Switch(..., checked = debugMenuMetadata.isBodyOverlayEnabled, onCheckedChange = { onIsBodyOverlayEnabledChanged() })
```
and `CollisionMaskOverlaySwitch` with `collision_mask_overlay` / `isCollisionMaskOverlayEnabled`.

## Fix
Replace both with
`@Composable private fun OverlaySwitch(title: StringResource, isChecked: Boolean, onToggled: () -> Unit) = Row(...)`
— the body of `BodyOverlaySwitch` verbatim with `stringResource(title)`, `selected = isChecked`, `checked = isChecked`,
`onClick = onToggled`, `onCheckedChange = { onToggled() }`. Update the four call sites (two in the horizontal Column, two in the
vertical `item("bodyOverlaySwitch")` / `item("collisionMaskOverlaySwitch")` — keep those item keys), e.g.
`OverlaySwitch(title = Res.string.body_overlay, isChecked = debugMenuMetadata.isBodyOverlayEnabled, onToggled = onIsBodyOverlayEnabledChanged)`.
Import `org.jetbrains.compose.resources.StringResource`.

## Behaviour
Same rows, same modifiers; rendered UI unchanged.

## Public API
None.

## Tests
The existing ones.

## Verify
`./gradlew :tools:debug-menu:compileKotlinDesktop :tools:debug-menu:compileKotlinWasmJs`

## Manual check
Open the debug menu (both portrait and landscape layouts): both switches toggle their overlays.
