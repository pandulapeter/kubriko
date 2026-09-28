# Fix the frame row lookup for sprite sheets rotated by 270 degrees

**Challenged:** sound

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all  ·  **Artifact:** `plugin-sprites`
**Files:** `plugins/sprites/src/commonMain/kotlin/com/pandulapeter/kubriko/sprites/AnimatedSprite.kt`, `plugins/sprites/src/commonTest/kotlin/com/pandulapeter/kubriko/sprites/AnimatedSpriteTest.kt` (created by plan 58, or new)

## Problem

`AnimatedSprite` maps a frame index to a cell of the (possibly rotated) sheet (at 0008d027):

```kotlin
private fun getXIndex(frameIndex: Int) = when (orientation) {
    ...
    Rotation.DEGREES_90 -> numberOfRows - 1 - frameIndex / framesPerRow
    Rotation.DEGREES_270 -> frameIndex / framesPerRow
}

private fun getYIndex(frameIndex: Int) = when (orientation) {
    ...
    Rotation.DEGREES_90 -> frameIndex % framesPerRow
    Rotation.DEGREES_270 -> framesPerRow - 1 - frameIndex / framesPerRow
}
```

For `DEGREES_270` both coordinates are derived from the row (`frameIndex / framesPerRow`), so every frame of a row maps to the same cell: the animation shows one frame per row and ignores the column. A 270° rotation is the inverse of the 90° one, so where 90° sends cell `(column, row)` to `(numberOfRows - 1 - row, column)`, 270° must send it to `(row, framesPerRow - 1 - column)` — `getYIndex` needs `% framesPerRow`, mirroring the 90° case. `getXIndex` is already right. (Nothing in the repo uses `DEGREES_270` with an `AnimatedSprite`.)

## Fix

`Rotation.DEGREES_270 -> framesPerRow - 1 - frameIndex % framesPerRow` in `getYIndex`. Make `getXIndex`/`getYIndex` `internal` (they are private today) so the test can reach them; no public API change.

## Tests

In `AnimatedSpriteTest` (`commonTest`): for every `Rotation` value, with `frameCount = 6`, `framesPerRow = 3` (and a second case with a partial last row, `frameCount = 5`), collect `getXIndex(i) to getYIndex(i)` for `i in 0 until frameCount` and assert the pairs are all distinct and inside the rotated grid (`0 until numberOfRows` × `0 until framesPerRow` for 90°/270°, the reverse for 0°/180°). Additionally assert for `DEGREES_270` that frame 0 → `(0, framesPerRow - 1)` and frame 1 → `(0, framesPerRow - 2)`.

Run `./gradlew :plugins:sprites:desktopTest`.

## Manual check

None in the Showcase (no rotated animated sheet).
