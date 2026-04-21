# Sprites Plugin

The `sprites` plugin handles loading, caching, and rendering static and animated images (sprites) in Kubriko. It provides a simple API for preloading resources and managing frame-based animations.

## Features

- **SpriteManager**: Handles loading and caching of `ImageBitmap`s from resources.
- **AnimatedSprite**: A helper class for managing and drawing frame-based animations from a sprite sheet.
- **Preloading Support**: Easily track loading progress and ensure assets are ready before use.
- **Automatic Rotation**: Supports pre-rotated sprite sheets for improved performance.

## Usage

### 1. Register the Manager

Add the `SpriteManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    SpriteManager.newInstance(),
    // ... other managers
)
```

### 2. Preload Sprites (Optional)

You can preload individual `DrawableResource`s or `SpriteResource`s (which contain metadata about the sprite sheet) to ensure they are available when needed.

```kotlin
val spriteManager = kubriko.get<SpriteManager>()
spriteManager.preload(Res.drawable.player_idle, Res.drawable.player_run)

// Track loading progress
val progressFlow = spriteManager.getLoadingProgress(listOf(Res.drawable.player_idle))
```

**Note:** Preloading is optional. Calling `spriteManager.get()` for a resource that hasn't been loaded yet will trigger the loading process and return `null`. Subsequent calls will continue to return `null` until the resource is fully loaded and cached.

### 3. Use AnimatedSprite (if you need it)

`AnimatedSprite` makes it easy to handle animations:

```kotlin
val playerIdle = AnimatedSprite(
    getImageBitmap = { spriteManager.get(Res.drawable.player_idle) },
    frameSize = IntSize(32, 32),
    frameCount = 4,
    framesPerRow = 4,
    framesPerSecond = 8f
)

// In your update loop
playerIdle.stepForward(deltaTimeInMilliseconds, shouldLoop = true)

// In your draw loop
playerIdle.draw(scope)
```

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-sprites`
