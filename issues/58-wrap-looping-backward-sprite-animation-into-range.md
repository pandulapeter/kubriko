# Wrap a looping backward sprite animation into the valid frame range

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** all  ·  **Artifact:** `plugin-sprites`
**Files:** `plugins/sprites/src/commonMain/kotlin/com/pandulapeter/kubriko/sprites/AnimatedSprite.kt`, `plugins/sprites/src/commonTest/kotlin/com/pandulapeter/kubriko/sprites/AnimatedSpriteTest.kt` (new)

## Problem

`AnimatedSprite.normalizeImageIndex` (at 0008d027):

```kotlin
private fun normalizeImageIndex(shouldLoop: Boolean) {
    if (frameIndex >= frameCount - 1 || frameIndex < 0) {
        _frameIndex = if (shouldLoop) {
            _frameIndex % frameCount
        } else {
            if (_frameIndex < 0) 0f else frameCount - 1f
        }
    }
}
```

Kotlin's `%` keeps the sign of the dividend, so `stepBackwards(..., shouldLoop = true)` past frame 0 leaves `_frameIndex` negative (e.g. `-0.5f % 4 == -0.5f`, so `frameIndex == -1`). It never wraps to the last frame, keeps decreasing on the next steps, and `draw` computes a negative `srcOffset` (`-1 % framesPerRow == -1`), i.e. it samples outside the sprite sheet — a blank or garbage frame depending on the platform — for the rest of the animation. `isFirstFrame`/`isLastFrame` are also never true again. `stepBackwards` is public API with `shouldLoop` documented as "Whether the animation should restart when it reaches the first frame". (`game-space-squadron`'s `Ship` uses `stepBackwards` without looping, so the Showcase does not show it.)

## Fix

In the loop branch use a non-negative remainder: `((_frameIndex % frameCount) + frameCount) % frameCount`. Forward looping is unchanged (the extra `+ frameCount` and second `%` leave a non-negative value as it was).

## Tests

`AnimatedSpriteTest` in `commonTest` (`AnimatedSprite(getImageBitmap = { null }, frameSize = IntSize(1, 1), frameCount = 4, framesPerRow = 2, framesPerSecond = 1000f)` — one frame per millisecond):
- From frame 0, `stepBackwards(deltaTimeInMilliseconds = 1, shouldLoop = true)` → `frameIndex == 3` and `isLastFrame`.
- `stepBackwards(5, shouldLoop = true)` from frame 0 → `frameIndex` in `0..3` (expected `3`); repeated 20 times, `frameIndex` always stays in `0..3`.
- Non-looping backward from 0 → stays `0`; forward looping from 3 by 1 → `0`.

Run `./gradlew :plugins:sprites:desktopTest`.

## Manual check

None in the Showcase (no looping backward animation); the unit test covers it.
