# Remove the four unused imports from the sprites plugin's desktop `ImageLoader`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** plugin-sprites
**Files:**
- `plugins/sprites/src/desktopMain/kotlin/com/pandulapeter/kubriko/sprites/implementation/ImageLoader.desktop.kt`

## Problem
At 2480325f the file imports

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.toArgb
...
import org.jetbrains.skia.Matrix33
```

(:12, :14, :15, :19) and uses none of them (`grep -cE "\bColor\b|Matrix\b|toArgb|Matrix33"` counts only the four
import lines). They are also the main textual difference from the otherwise-identical iOS/web copies.

## Fix
Delete the four import lines. Nothing else.

## Behaviour
Unchanged.

## Public API
None (the file holds an `internal actual fun`).

## Tests
The existing ones.

## Verify
`./gradlew :plugins:sprites:compileKotlinDesktop`

## Manual check
none

Note for lane E: its Skia-shared-source-set plan moves this actual later; landing this first leaves the desktop copy
closer to the iOS/web ones.
