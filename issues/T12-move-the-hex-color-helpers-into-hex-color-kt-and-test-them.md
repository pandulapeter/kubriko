# Move the hex color helpers out of ColorPropertyEditor.kt into HexColor.kt and test them

**Kind:** test  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Now
**Artifact:** tool-scene-editor (internal code only)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/propertyEditors/ColorPropertyEditor.kt, tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/propertyEditors/HexColor.kt (new), tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/propertyEditors/HexColorTest.kt (new)
**Challenged:** amended — the parse test compares whole `Color` values, since `Color` stores sRGB channels in 8 bits and `alpha` reads back as 77/255, not 0.3f.

## Problem
`ColorPropertyEditor.kt` keeps pure parsing/formatting as private helpers next to the Composables, out of reach of tests:
```kotlin
private fun Char.isHexDigit() = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
private fun Float.toHexChannel() = (this * 255).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
private fun Color.toHexString() = "${red.toHexChannel()}${green.toHexChannel()}${blue.toHexChannel()}"
private fun String.parseHexColor(alpha: Float) = takeIf { it.length == 6 }?.toLongOrNull(16)?.let { value -> Color(...) }
```
and the sanitising expression inside `HexInput`'s `onValueChange`:
`val sanitized = newValue.text.filter { it.isHexDigit() }.uppercase().takeLast(6)`.

## Fix
- Move the four helpers verbatim into `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/propertyEditors/HexColor.kt` (same package, MPL-2.0 header), widening
  `toHexString` and `parseHexColor` to `internal` (used by `ColorPropertyEditor.kt`). `isHexDigit` and `toHexChannel` become
  `internal` only if the test or another file needs them; otherwise keep them `private` in `HexColor.kt`.
- Add `internal fun sanitizeHexInput(text: String) = text.filter { it.isHexDigit() }.uppercase().takeLast(6)` there and call it
  from `HexInput`: `val sanitized = sanitizeHexInput(newValue.text)`.
- Trim imports in `ColorPropertyEditor.kt` (`roundToInt`).
- Check the package for name clashes first (`grep -rn "isHexDigit\|toHexString\|parseHexColor" tools/scene-editor`).

## Behaviour
Same expressions, moved; the hex field behaves identically.

## Public API
None (internal).

## Tests
`HexColorTest` (pure; `androidx.compose.ui.graphics.Color` is a value class and needs no Skia):
- `Color(red = 1f, green = 0.5f, blue = 0f).toHexString() == "FF8000"` (0.5 * 255 = 127.5 rounds to 128 = 0x80).
- `"FF8000".parseHexColor(0.3f) == Color(red = 1f, green = 128 / 255f, blue = 0f, alpha = 0.3f)` — compare whole `Color`s, not
  channels against literals: sRGB `Color` packs each channel into 8 bits, so `.alpha` reads back 77/255 (≈ 0.302), not `0.3f`;
  `"FFF".parseHexColor(1f)` and `"GG0000".parseHexColor(1f)` give null.
- `sanitizeHexInput("#ab-12cdEF99") == "CDEF99"` (non-hex characters dropped, uppercased, last six kept); `sanitizeHexInput("") == ""`.
- Round trip: for a few colors with channels that are multiples of 1/255, `toHexString().parseHexColor(alpha)` equals the color.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop :tools:scene-editor:desktopTest`

## Manual check
None.
