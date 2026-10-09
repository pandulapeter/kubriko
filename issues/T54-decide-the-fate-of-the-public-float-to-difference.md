# Decide what to do with the accidentally public Float.toDifference() in tool-scene-editor

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** desktop  ·  **Class:** Planned
**Artifact:** tool-scene-editor (published)
**Files:** tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorSlider.kt
**Rebased:** on 70de96c6 after the Now plans landed.

## Problem
`EditorSlider.kt:94-98` (at 70de96c6; 1e59a627 dropped the slider's unused `name`/`suffix` parameters, the function is unchanged) declares a top-level function without a visibility modifier, so it is public API of the published
`tool-scene-editor` (JVM facade `com.pandulapeter.kubriko.sceneEditor.implementation.userInterface.components.EditorSliderKt`):
```kotlin
fun Float.toDifference(): Float {
    val a = absoluteValue / 1000
    val b = ln(2000.0) / 5
    return (a * exp(b * absoluteValue).toFloat() * sign)
}
```
It is the slider's internal exponential step curve, lives in an `implementation` package, has no KDoc (the style requires KDoc
on all public API) and is used only by `EditorSlider` — no other caller in this repo or in Tesselar.

## Decision
- (a) **Deprecate now, internalize later (recommended):** add KDoc and `@Deprecated("Implementation detail of the Scene Editor's
  slider; it will become internal.", level = DeprecationLevel.WARNING)`, suppress the warning at the one internal call site, and make it
  `internal` in the next release that may break binary compatibility.
- (b) Make it `internal` now — a binary and source break for a function nobody is known to call.
- (c) Leave it public and only add KDoc.

## Fix
Per the decision. For (a): KDoc describing the curve (maps the slider's −5…5 offset to a signed step growing exponentially, ±10 at
the ends), the `@Deprecated`, and `@Suppress("DEPRECATION")` on the call in `EditorSlider` (:54, `onValueChanged(value + add.value.toDifference())`). Note it in the release notes.

## Behaviour
None for (a)/(c). (b) removes it from the public surface.

## Public API
(a) adds a deprecation warning; (b) removes a public function; (c) none.

## Tests
None needed; a curve test could pin `0f.toDifference() == 0f`, oddness `(-x).toDifference() == -x.toDifference()` and
`5f.toDifference() ≈ 10f` if the implementer wants one.

## Verify
`./gradlew :tools:scene-editor:compileKotlinDesktop`

## Manual check
None.
