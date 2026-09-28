# Default a missing BoxBody pivot to the center of the decoded size

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `plugin-serialization`
**Files:** `plugins/serialization/src/commonMain/kotlin/com/pandulapeter/kubriko/serialization/typeSerializers/BoxBodySerializer.kt`, `plugins/serialization/src/commonTest/kotlin/com/pandulapeter/kubriko/serialization/BoxBodySerializerTest.kt` (new)

**Decision needed:** should a serialized `BoxBody` without a `pivot` field get the center of its decoded `size` (as `BoxBody`'s own constructor default does) instead of the top-left corner? — recommended: **yes**. It only affects JSON written by hand or by another tool, since `serialize` always writes `pivot`.

## Problem

`BoxBodySerializer.deserialize` (at 0008d027):

```kotlin
var position = SceneOffset.Zero
var size = SceneSize.Zero
var pivot = size.center
...
BoxBody(
    initialPosition = position,
    initialSize = size,
    initialPivot = pivot,
```

`pivot` is initialized from `size` while `size` is still `SceneSize.Zero`, so a document without `"pivot"` gets pivot `(0, 0)` — rotation and scaling around the top-left corner — while `BoxBody(initialSize = size)` defaults `initialPivot` to `initialSize.center`. A hand-written or trimmed scene file (or one produced by an external level tool) that omits the pivot renders rotated/scaled actors around the wrong point, with no error.

## Fix

Track the pivot as `SceneOffset?` initialized to `null`, assign it in the `2 ->` branch (decode with the default `SceneOffset.Zero` as the "previous value" argument), and pass `initialPivot = pivot ?: size.center` after the loop. No change for documents that contain `pivot`, which is everything `serialize` produces.

## Tests

`BoxBodySerializerTest` in `commonTest`:
- `Json.decodeFromString(BoxBodySerializer, <JSON with position and size 10×20, no pivot>)` → `pivot == SceneOffset(5, 10)`. Build the JSON by encoding a `BoxBody` with `Json.encodeToString(BoxBodySerializer, ...)` and removing the `pivot` key from the `JsonObject`, so the test does not depend on the exact element encoding.
- A full round trip of a `BoxBody` with a non-default pivot, scale and rotation keeps all five fields.

Run `./gradlew :plugins:serialization:desktopTest`.

## Manual check

None.
