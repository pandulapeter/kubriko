<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-sprites internals

## Resource loading pipeline
Resources are identified by `SpriteResource(drawableResource: DrawableResource, rotation: Rotation)`.
`DrawableResource` wraps Compose Multiplatform's typed resource reference (generated from
`commonMain/composeResources/`). Raw bytes are read via
`getDrawableResourceBytes(getSystemResourceEnvironment(), resource)` on the manager's coroutine
scope, then decoded platform-specifically to `ImageBitmap`. Rotation baking happens at decode time,
not at draw time.

## Null-while-loading contract
`get(resource)` follows a strict state machine:
1. **Not in cache**: inserts `null` sentinel into cache, launches background load, returns `null`.
2. **In cache with null value**: load is already in progress, returns `null`.
3. **In pendingWarmingUp**: decoded but not yet GPU-warmed, returns `null`.
4. **In cache with non-null value**: returns the `ImageBitmap`.

Callers must handle `null` every frame until the sprite is ready. The typical pattern in
`Dynamic.update()` is to early-return or draw a placeholder when `get()` returns `null`.

## Warm-up phase
After decoding, bitmaps are placed in `pendingWarmingUp` and drawn once via an invisible
`Canvas` (alpha=0) in the manager's `Composable()` override. This uploads the texture to GPU
before first visible use, preventing a hitch. After 100 ms (`WARM_UP_TIMEOUT_MS`) the bitmap is
promoted from `pendingWarmingUp` to the main cache regardless of whether the Canvas draw fired.
`promoteToCache()` is idempotent.

## Platform differences in decoding
- **Android**: uses `BitmapFactory.decodeByteArray` with density scaling (`inDensity`/`inTargetDensity`); only downscales (no upscale from low-dpi resources). Rotation via `android.graphics.Matrix`.
- **Desktop / iOS / Web**: uses Skia `Image.makeFromEncoded`, then draws into a `Surface.makeRasterN32Premul` canvas with rotation transform. Same downscale-only density logic as Android (CMP-5657).
- All platforms decode at MDPI (160 dpi) target density; resource density is set to MDPI as well so no scaling occurs for standard assets.

## SpriteResource vs DrawableResource
`DrawableResource` is the raw Compose resource handle. `SpriteResource` wraps it with an optional
`Rotation` (NONE/90/180/270). The same `DrawableResource` with different rotations is stored as
separate cache entries. Use `DrawableResource.toSpriteResource(rotation)` as the idiomatic
conversion. Calling `get(DrawableResource)` delegates to `get(resource.toSpriteResource())`.

## AnimatedSprite frame layout
Frames are packed row-first in a single sprite sheet `ImageBitmap`. Construction parameters:
- `frameSize: IntSize` — pixel size of one frame in the sheet (pre-rotation).
- `framesPerRow: Int` — number of columns in the sheet.
- `frameCount: Int` — total frames (last row may be partial).
- `orientation: SpriteResource.Rotation` — rotates the coordinate system used to compute
  `srcOffset`, so the sheet can be stored rotated without affecting draw output size.

`_frameIndex` is a `Float` accumulated by `stepForward`/`stepBackwards`; `frameIndex` (Int) is
`floor(_frameIndex)`. Animation speed formula: `delta * speed * framesPerSecond / 1000`. The
`getImageBitmap: () -> ImageBitmap?` lambda should capture `{ spriteManager.get(resource) }` so
`isLoaded` reflects real-time cache state.

## Drawing a sprite inside Visible.draw()
```kotlin
override fun DrawScope.draw() {
    animatedSprite.draw(this)  // no-op if not loaded yet
}
```
For static sprites: `spriteManager.get(resource)?.let { drawImage(it) }`. Both paths are
allocation-free when the bitmap is cached.
