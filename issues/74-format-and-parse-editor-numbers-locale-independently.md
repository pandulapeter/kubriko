# Format and parse the scene editor's number fields locale-independently, outside the suffix, with local text state

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** desktop
**Artifact:** `tool-scene-editor`
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorNumberInput.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/components/EditorTextInput.kt`, `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/NumberFormatting.kt` (new), `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/helpers/NumberFormattingTest.kt` (new)

## Problem

Every numeric property field (position, size, scale, rotation, color channels, `Float`/`Int`/`SceneUnit` properties) goes through `EditorNumberInput` (~48-53):

```kotlin
EditorTextInput(
    modifier = Modifier.weight(1f),
    title = name,
    value = (if (shouldRound) value.roundToInt().toString() else "%.2f".format(value)) + suffix,
    onValueChanged = { newValue ->
        newValue.toFloatOrNull()?.let {
            onValueChanged(if (valueRange == null) it else min(valueRange.endInclusive, max(valueRange.start, it)))
        }
    },
    ...
)
```

1. `"%.2f".format(value)` uses the JVM default locale. On a Hungarian, German, French… system it renders `1,50`, and `toFloatOrNull()` rejects every edit of that text, so typing does nothing and the field snaps back.
2. The suffix is part of the edited text. Rotation in degrees passes `suffix = "°"` (`RotationPropertyEditor.kt` ~44), so the field shows `45.00°`, any edit yields `46.00°`, `toFloatOrNull()` returns `null`, and degree values cannot be typed in any locale.
3. The field is fully controlled by the model: each keystroke that parses is immediately replaced by the reformatted value (clear "1.50" to type "2" and the text jumps around; intermediate states that do not parse are thrown away).

## Fix

1. New `helpers/NumberFormatting.kt` (MPL header), pure:
   ```kotlin
   internal fun formatEditorNumber(value: Float, shouldRound: Boolean): String =
       if (shouldRound) value.roundToInt().toString() else String.format(Locale.ROOT, "%.2f", value)

   internal fun parseEditorNumber(text: String): Float? =
       text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
   ```
2. `EditorTextInput`: add `suffix: String? = null`, rendered as an `EditorText` right after the `TextInput` inside the same `Row` (before `extraContent`), so it is shown but never edited.
3. `EditorNumberInput`: keep the text locally so partial input survives:
   ```kotlin
   var text by remember { mutableStateOf(formatEditorNumber(value, shouldRound)) }
   LaunchedEffect(value, shouldRound) {
       if (parseEditorNumber(text) != value) text = formatEditorNumber(value, shouldRound)
   }
   EditorTextInput(
       value = text,
       suffix = suffix.takeIf { it.isNotEmpty() },
       onValueChanged = { newText ->
           text = newText
           parseEditorNumber(newText)?.let { onValueChanged(valueRange?.let { range -> it.coerceIn(range) } ?: it) }
       },
       ...
   )
   ```
   The effect re-syncs the text when the value changes from elsewhere (slider, undo, drag, clamping) and leaves it alone while it already denotes the current value. Replace the `min`/`max` pair with `coerceIn` and drop the `kotlin.math.max`/`min` imports if unused.

`shouldRound` Int fields keep their integer text. No call site changes: callers still pass `suffix`.

## Tests

`NumberFormattingTest` (desktopTest, pure):
- `formatEditorNumber(1.5f, false)` is `"1.50"` with `Locale.setDefault(Locale.forLanguageTag("hu-HU"))` and with `de-DE` (restore the previous default in a `finally`/`@AfterTest`);
- `formatEditorNumber(2.6f, true)` is `"3"`;
- `parseEditorNumber`: `"1.5"` → 1.5, `"1,5"` → 1.5, `" 45.00 "` → 45, `"-0.25"` → -0.25, `""`/`"-"`/`"abc"`/`"45°"` → `null`, `"NaN"`/`"Infinity"` → `null`.

Run `./gradlew :tools:scene-editor:desktopTest`.

## Manual check

Desktop: start the Showcase with a comma-decimal locale (e.g. add `-Duser.language=hu -Duser.country=HU` to the desktop run configuration's JVM args), open a scene editor and select an actor:
1. Position fields show `12.00` (dot), and typing `3,5` or `3.5` moves the actor.
2. With Settings → angle controls = Degrees, the rotation field shows `45.00` followed by a separate `°`; typing `90` rotates the actor.
3. Clear a field and type a new number digit by digit: the text is not reformatted under the cursor; dragging the slider afterwards updates the text.
