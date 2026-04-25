# TickSource

`TickSource` controls when a `Kubriko` instance advances its engine state.

Each emitted tick calls the engine update path with a `deltaTimeInMilliseconds` value. Managers receive that delta through their update lifecycle, and dynamic Actors are updated through the relevant Managers. Until a TickSource is started, no update ticks are emitted.

## Why TickSource exists

Originally, the engine loop was tied directly to `KubrikoViewport`: once the Composable entered composition, viewport frames drove the updates.

That remains the default behavior, but the loop can now be supplied separately when creating the engine:

```kotlin
val kubriko = Kubriko.newInstance(
    myManager,
    tickSource = TickSource.viewportFrames(),
)
```

By separating the update loop from the viewport, Kubriko can run in places where no viewport is mounted:

- automated tests
- deterministic simulations
- replay capture and playback
- editors and tools that step the engine manually
- server-side or command-line simulations
- background systems where rendering is optional

## Built-in TickSources

### Viewport frames

```kotlin
val kubriko = Kubriko.newInstance(
    tickSource = TickSource.viewportFrames(),
)
```

`TickSource.viewportFrames()` is the default. It is started automatically by `KubrikoViewport` and emits ticks from Compose frame callbacks while the viewport is
available, non-empty, and focused.

Use this for normal games where rendering and engine updates should be coupled to the displayed viewport.

### Fixed rate

```kotlin
val tickSource = TickSource.fixedRate(intervalInMilliseconds = 16L)
val kubriko = Kubriko.newInstance(
    tickSource = tickSource,
)

tickSource.start()
```

`TickSource.fixedRate()` emits ticks from the Kubriko coroutine scope after a fixed delay. The delta passed to the engine is the configured interval. This source
does not require `KubrikoViewport`, so it is suitable for headless runs.

The first tick uses a delta of `0`.

### Fixed frequency

```kotlin
val tickSource = TickSource.fixedFrequency(ticksPerSecond = 60)
val kubriko = Kubriko.newInstance(
    tickSource = tickSource,
)

tickSource.start()
```

`TickSource.fixedFrequency()` targets a number of ticks per second. It measures elapsed time between ticks and passes that measured delta to the engine. If the
loop falls behind, it resynchronizes instead of trying to replay an unbounded backlog of missed ticks.

The first tick uses a delta of `0`.

### Manual

```kotlin
val tickSource = TickSource.manual()
val kubriko = Kubriko.newInstance(
    tickSource = tickSource,
)

tickSource.start()
tickSource.tick(deltaTimeInMilliseconds = 16)
tickSource.tick(deltaTimeInMilliseconds = 16)
```

`TickSource.manual()` returns a `ManualTickSource`. It advances only when `tick()` is called. This is the most deterministic option and is useful for unit tests,
replays, and editor tools that need frame-by-frame control.

## Lifecycle

A TickSource is attached to the Kubriko instance during `Kubriko.newInstance()`.

Calling `start()`:

- initializes the attached Kubriko instance if needed
- initializes Managers if they have not already been initialized
- marks the TickSource as running
- calls `onStart()`

Calling `stop()`:

- stops future ticks from being emitted
- calls `onStop()`
- keeps the Kubriko instance alive

Calling `kubriko.dispose()`:

- stops the TickSource
- calls `onDispose()`
- disposes Managers
- cancels the Kubriko coroutine scope

Calling `start()` or `stop()` multiple times is safe.

## Headless usage

For a headless engine, keep a reference to the TickSource you pass into `Kubriko.newInstance()` and start it yourself:

```kotlin
val tickSource = TickSource.fixedFrequency(ticksPerSecond = 60)
val kubriko = Kubriko.newInstance(
    simulationManager,
    tickSource = tickSource,
)

tickSource.start()

// Later:
tickSource.stop()
kubriko.dispose()
```

When no viewport is mounted, viewport-dependent state such as `ViewportManager.size` may remain empty or unchanged. Managers and Actors intended for headless
execution should avoid depending on current viewport dimensions unless those values are configured by the test, tool, or simulation environment.

## Extending TickSource

Create a custom TickSource by extending `TickSource` and calling `emitTick(deltaTimeInMilliseconds)` from your timing source.

```kotlin
class ExternalClockTickSource : TickSource() {

    fun onExternalFrame(deltaTimeInMilliseconds: Int) {
        emitTick(deltaTimeInMilliseconds)
    }
}
```

`emitTick()` only forwards updates while the TickSource is running, so external callbacks can safely call it before or after `start()` if that simplifies
integration.

For sources that allocate resources, override the lifecycle callbacks:

```kotlin
class SubscriptionTickSource(
    private val clock: ExternalClock,
) : TickSource() {
    private var subscription: Subscription? = null

    override fun onStart() {
        subscription = clock.subscribe { deltaTimeInMilliseconds ->
            emitTick(deltaTimeInMilliseconds)
        }
    }

    override fun onStop() {
        subscription?.cancel()
        subscription = null
    }

    override fun onDispose() {
        // Release resources that should live until the Kubriko instance is disposed.
    }
}
```

Use `onInitialize(kubriko)` when the source needs access to the attached `Kubriko` instance before it starts. Use the protected `scope` property to launch
coroutines that should live inside Kubriko's coroutine scope and be cancelled when the engine is disposed.

## Choosing a TickSource

- Use `TickSource.viewportFrames()` for normal rendered games.
- Use `TickSource.fixedRate()` when every tick should receive the same configured delta.
- Use `TickSource.fixedFrequency()` when the engine should target a frequency but use measured elapsed time.
- Use `TickSource.manual()` for deterministic stepping.
- Extend `TickSource` when ticks come from an external scheduler, platform callback, server loop, editor timeline, or replay stream.
