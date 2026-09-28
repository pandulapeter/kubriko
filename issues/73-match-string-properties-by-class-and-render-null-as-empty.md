# Match String properties by class in the property inspector and show null as an empty field

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** desktop
**Artifact:** `tool-scene-editor`
**Files:** `tools/scene-editor/src/desktopMain/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/PropertyEditorMapper.kt`, `tools/scene-editor/src/desktopTest/kotlin/com/pandulapeter/kubriko/sceneEditor/implementation/userInterface/panels/instanceManagerColumn/PropertyEditorKindTest.kt` (new), `tools/scene-editor-api/CLAUDE.md`, `tools/scene-editor/CLAUDE.md`

## Problem

`PropertyEditorMapper.kt` matches a property's `returnType` against prebuilt `KType`s with `==`, which includes nullability:

```kotlin
private val stringType = String::class.starProjectedType.withNullability(true)
...
when (returnType) {
    ...
    stringType -> {
        {
            StringPropertyEditor(
                name = name,
                value = getter.call(actor) as String,
                onValueChanged = { applyValue(it) }
            )
        }
    }
```

1. A plain `@set:Exposed(name = "label") var label: String` has type `String` (not `String?`), matches nothing and gets **no editor** — silently.
2. A `String?` property (the only form that matches, and the one `scene-editor-api/CLAUDE.md` lists) whose value is `null` hits `getter.call(actor) as String` inside the composable lambda: a `NullPointerException` ("null cannot be cast to non-null type") during composition, which takes down the editor window as soon as that actor is selected.

## Fix

1. Extract the type dispatch into a pure function in the same file:
   ```kotlin
   internal enum class PropertyEditorKind { BOOLEAN, COLOR, ANGLE_DEGREES, ANGLE_RADIANS, SCENE_OFFSET, SCALE, FLOAT, INT, STRING, SCENE_UNIT }

   internal fun KType.toPropertyEditorKind(): PropertyEditorKind? = when {
       classifier == String::class -> PropertyEditorKind.STRING
       else -> when (this) { booleanType -> BOOLEAN; ...; else -> null }
   }
   ```
   Only `String` is matched regardless of nullability; every other type keeps its exact (non-null) match, so nothing else changes. `toPropertyEditor` then does `when (returnType.toPropertyEditorKind()) { ... }`.
2. The `STRING` branch reads `value = (getter.call(actor) as String?).orEmpty()`. Editing writes back the typed `String`, which is valid for both `String` and `String?` setters.
3. Delete `stringType` and the now-unused `starProjectedType`/`withNullability` imports.
4. `tools/scene-editor-api/CLAUDE.md` → supported setter types: "`String?` — rendered as a text input" → "`String` or `String?` — rendered as a text input (`null` shows as empty)".
5. `tools/scene-editor/CLAUDE.md` → Property inspector says the label falls back "to the `KMutableProperty.name` when the annotation's `name` is blank"; the code has no such fallback (`Exposed.name` is required, and the `Exposed` KDoc explains why reflection names are not used). Change that sentence to "The displayed label is `@Exposed.name`."

## Tests

`PropertyEditorKindTest` (desktopTest; `kotlin-reflect` is already a `desktopMain` dependency). Declare a private test class with `var a: String = ""`, `var b: String? = null`, `var c: Int = 0`, `var d: Int? = null`, `var e: Color = Color.Red`, `var f: SceneOffset = SceneOffset.Zero`, and assert through `TestClass::a.returnType.toPropertyEditorKind()` etc.: `a` and `b` → `STRING`, `c` → `INT`, `d` → `null`, `e` → `COLOR`, `f` → `SCENE_OFFSET`.

Run `./gradlew :tools:scene-editor:desktopTest`.

## Manual check

Desktop: temporarily add `@set:Exposed(name = "label") var label: String? = null` to an `Editable` actor in an example (e.g. an Annoyed Penguins actor), open its scene editor, place and select that actor: the panel shows an empty "label" field and typing into it works (before the fix the editor crashes on selection). Change the property to non-null `String = "x"`: the field appears showing "x" (before the fix it was missing). Revert the temporary change.
