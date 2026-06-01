# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Kubriko is a 2D game engine built on Compose Multiplatform, targeting **Android, Desktop (Windows/Linux/macOS), iOS, and Web (Kotlin/Wasm)**. The repo contains the engine library, its plugins and tools (all published to Maven Central under `io.github.pandulapeter.kubriko`), and the "Kubriko Showcase" demo app that exercises them.

## Build, run, test

Requires **JDK 21** (the Kotlin Multiplatform toolchain pins language version 21; the `gradle/build-logic` build itself compiles against JDK 17). Use the `./gradlew` wrapper.

```bash
./gradlew build                       # Build everything
./gradlew :engine:build               # Build a single module (path = Gradle project path)
./gradlew :app:desktop:run            # Run the Showcase app on Desktop (JVM)
./gradlew :app:web:wasmJsBrowserDevelopmentRun   # Run the Showcase app in a browser
./gradlew :app:android:installDebug   # Install the Showcase app on a connected Android device/emulator
./gradlew test                        # Run JVM/common unit tests across modules
./gradlew :engine:desktopTest --tests "com.pandulapeter.kubriko.SomeTest"   # Single test
./gradlew publishToMavenCentral --no-configuration-cache   # Publish (CI uses this)
```

iOS runs from Xcode / the IDE run configuration (Mac only); there is no `gradlew` run task for it.

## Build configuration flags

`gradle.properties` holds flags that change what gets compiled into the Showcase app (toggled at the buildscript level so excluded code/resources never enter the build):

- `showcase.areTestExamplesEnabled` — include the `test-*` example modules vs. their `-noop` blanks
- `showcase.isDebugMenuEnabled`, `showcase.isSceneEditorEnabled` — swap real tool modules for `-noop` ones
- `showcase.shouldShowUnfinishedGames`
- `library.version` — the published artifact version

## Module layout

Top-level Gradle areas (each has its own README with details):

- `engine/` — core: the `Kubriko` instance, `KubrikoViewport`, default Managers, the Actor/Trait system, geometry types. Artifact: `io.github.pandulapeter.kubriko:engine`
- `plugins/` — opt-in feature modules; each exposes a Manager (sometimes new Traits). Artifacts: `io.github.pandulapeter.kubriko:plugin-<name>`
- `tools/` — development-time modules not meant to ship to players. Artifacts: `io.github.pandulapeter.kubriko:tool-<name>`
- `examples/` — the games/demos/tests embedded in the Showcase app; canonical "how to use Kubriko" references.
- `app/` — the Showcase shell (menu system + per-platform entry points: `android`, `desktop`, `ios`, `web`, `shared`). Intentionally atypical (runs many Kubriko instances in parallel), so not a best-practices source.
- `gradle/build-logic/` — an `includeBuild` of convention plugins. **All module build files apply these, not raw Compose/KMP plugins.**

## Core architecture

The engine is a small core extended granularly by plugins. The key objects:

### `Kubriko` (`engine/.../Kubriko.kt`)
A `sealed interface` and `CoroutineScope` created via:
```kotlin
val kubriko = Kubriko.newInstance(
    vararg manager: Manager,
    tickSource: TickSource = TickSource.viewportFrames(),
    isLoggingEnabled: Boolean = false,
    instanceNameForLogging: String? = null,
)
```
- Owns all Managers and thus the entire game state.
- `kubriko.dispose()` stops the TickSource, disposes all Managers, cancels the coroutine scope.
- `kubriko.get<T: Manager>()` retrieves a Manager by type (throws if not registered or disposed).
- The internal Manager set is deduplicated by type; the last instance added wins. Default Managers (see below) are auto-added unless explicitly overridden.
- Custom subclasses of the four built-in `Manager` sealed classes are **not** supported; always use `.newInstance()`.
- Persist the `Kubriko` instance across configuration changes (e.g. in a `remember {}` or ViewModel).

### `KubrikoViewport` (`engine/.../KubrikoViewport.kt`)
```kotlin
KubrikoViewport(modifier, kubriko, windowInsets = WindowInsets.safeDrawing)
```
The Composable that renders the game canvas. With the default `TickSource.viewportFrames()`, Managers are fully initialized after the first composition. The viewport also feeds focus state to `StateManager`.

### Managers (`manager/Manager.kt`)
Abstract base for all game-global state and logic. Key contract:
- Created with the `Kubriko` instance; cannot be added or removed at runtime.
- Lifecycle: `onInitialize(kubriko)` → `onUpdate(deltaTimeInMilliseconds: Int)` (every tick) → `onDispose()`.
- Default built-in Managers are initialized first; custom Manager initialization order follows registration order.
- `scope` (the Kubriko `CoroutineScope`) and the `manager<T>()` delegate are only valid after `onInitialize`.
- `manager<T>()` / `manager(T::class)` — lazy delegate that resolves another Manager at init time. Throws if that Manager was not registered.
- `autoInitializingLazy { }` — lazy property initialized during `onInitialize`.
- `Flow<T>.asStateFlow(initialValue)` / `asStateFlowOnMainThread(initialValue)` — convenience to pin a Flow to the Manager's scope.
- `Composable(windowInsets)` — optional override to render UI inside the viewport.
- `processModifier(modifier, layerIndex, gameTime)` / `processOverlayModifier(modifier)` — optional override to inject Compose modifiers into game world layers or the overlay.
- `log(message, details, importance)` — guarded by `isLoggingEnabled`.

### Default Managers (auto-created unless overridden)

**`ActorManager`**
```kotlin
ActorManager.newInstance(
    initialActors: List<Actor> = emptyList(),
    shouldUpdateActorsWhileNotRunning: Boolean = false,
    shouldPutFarAwayActorsToSleep: Boolean = true,
    invisibleActorMinimumRefreshTimeInMillis: Long = 0,
)
```
- `add(vararg Actor)` / `add(Collection<Actor>)` — batched, background-thread addition; `Actor.onAdded(kubriko)` fires on main thread just before.
- `remove(vararg Actor)` / `remove(Collection<Actor>)` / `removeAll()` — batched, background-thread removal; `Actor.onRemoved()` fires on main thread just after.
- `allActors: StateFlow<ImmutableList<Actor>>`, `visibleActorsWithinViewport`, `activeDynamicActors` — observable collections.
- `shouldPutFarAwayActorsToSleep` — `Dynamic` actors outside the viewport stop receiving `update()` unless `isAlwaysActive = true`.

**`StateManager`**
```kotlin
StateManager.newInstance(
    shouldAutoStart: Boolean = true,
    focusDebounce: Long = platform default,
)
```
- `isFocused: StateFlow<Boolean>` — set by the engine from the viewport focus state.
- `isRunning: StateFlow<Boolean>` — false when paused or not focused.
- `updateIsRunning(Boolean)` — pause/resume; only effective while `isFocused` is true.

**`ViewportManager`**
```kotlin
ViewportManager.newInstance(
    aspectRatioMode: AspectRatioMode = Dynamic,
    initialScaleFactor: Float = 1f,
    minimumScaleFactor: Float = 0.2f,
    maximumScaleFactor: Float = 5f,
    viewportEdgeBuffer: SceneUnit = 0f.sceneUnit,
    frameRate: FrameRate = FrameRate.NORMAL,
)
```
- `AspectRatioMode` variants: `Dynamic` (matches screen), `FitHorizontal(width)`, `FitVertical(height)`, `Fixed(ratio, width, alignment)`, `Stretched(size)`.
- `cameraPosition`, `size` (pixels), `scaleFactor`, `topLeft`, `bottomRight` — observable state.
- `setCameraPosition(SceneOffset)`, `addToCameraPosition(Offset)`, `setScaleFactor(Float)`, `multiplyScaleFactor(Float)` — camera control.
- Coordinate conversion (scene ↔ screen) is handled internally; use `topLeft`/`bottomRight` for world-space bounds.

**`MetadataManager`**
- `fps`, `totalRuntimeInMilliseconds`, `activeRuntimeInMilliseconds` — observable perf counters.
- `platform: Platform` — sealed hierarchy: `Android`, `Desktop.MacOS/Linux/Windows`, `IOS`, `Web`.

### Actors (`actor/Actor.kt`)
```kotlin
interface Actor {
    fun onAdded(kubriko: Kubriko) = Unit   // main thread, right before addition
    fun onRemoved() = Unit                  // main thread, right after removal
}
```
In-game objects or responsibilities. Added/removed at runtime via `ActorManager`. Capabilities come entirely from Traits.

### Actor Traits (engine)

| Trait | Purpose |
|---|---|
| `Visible : Positionable, LayerAware` | Has a visual representation; must implement `DrawScope.draw()`. Requires `BoxBody`. `drawingOrder` controls paint order (lower = drawn on top). `shouldClip` clips to body bounds. `isVisible` can hide without removing. |
| `Dynamic` | Hooks into the game loop: `update(deltaTimeInMilliseconds: Int)`. `isAlwaysActive = true` to update even when off-screen. |
| `Positionable` | Has a `body: PointBody` (position in scene). |
| `LayerAware` | Belongs to a rendering layer via `layerIndex: Int?` (default `0`; layers drawn in increasing order). |
| `Overlay : LayerAware` | Draws directly onto the viewport (not world-space): `DrawScope.drawToViewport()`. `overlayDrawingOrder` controls paint order. |
| `Disposable` | `dispose()` is called by the engine before `onRemoved()`. Use for resource cleanup. |
| `Group` | Contains `actors: List<Actor>`; adding/removing a `Group` adds/removes all its children simultaneously. |
| `Identifiable` | Optional `name: String?`; auto-assigned if null when added. Names are **not** enforced unique. |
| `Unique` | Marker interface — ensures only one instance of this type exists at a time. Adding a second automatically removes the first. |

### Actor Bodies

- `PointBody` — position only; used by `Positionable`.
- `BoxBody : PointBody` — adds `size: SceneSize`, `pivot: SceneOffset` (rotation/scale center, clamped to `[0, size]`), `scale: Scale`, `rotation: AngleRadians`. Computes an `AxisAlignedBoundingBox` lazily (dirtied on any property change). Used by `Visible`.

### `TickSource` (`helpers/TickSource.kt`)
Decouples the update loop from the viewport. Passed to `Kubriko.newInstance()`.

| Factory | Behavior |
|---|---|
| `TickSource.viewportFrames()` | Default. Started by `KubrikoViewport`; ticks from Compose frames while visible and focused. Pauses on focus loss by default. |
| `TickSource.fixedRate(intervalInMilliseconds)` | Coroutine-based; delta = configured interval. First tick delta = 0. Requires explicit `start()`. |
| `TickSource.fixedFrequency(ticksPerSecond)` | Coroutine-based; delta = measured elapsed time. Re-syncs if behind. First tick delta = 0. Requires explicit `start()`. |
| `TickSource.manual()` | Advances only on explicit `tick(deltaTimeInMilliseconds)`. Deterministic; use for tests/replay. Requires `start()`. |

Lifecycle: `start()` initializes Kubriko if needed and begins emitting ticks; `stop()` suspends ticks; `kubriko.dispose()` stops and disposes everything. Calling `start()`/`stop()` multiple times is safe.

Custom TickSource: extend `TickSource`, call `emitTick(delta)` from any timing source. Override `onStart()`/`onStop()`/`onDispose()` for resource management. Use `onInitialize(kubriko)` if you need the Kubriko instance before start. `emitTick()` is safe to call before `start()`.

### Coordinate system
Two parallel systems, both share Compose's convention (X left→right, Y top→down):
- **Scene units** (`SceneUnit`, `SceneOffset`, `SceneSize`, `Scale`, `AngleRadians`/`AngleDegrees`) — resolution-independent; used for all game logic and positioning. All are `@JvmInline value class` wrappers.
- **Screen pixels** (`Float`, `Offset`, `Size`) — Compose units; used for final rendering and raw input.

Create scene types with extension properties: `1f.sceneUnit`, `SceneOffset(x, y)`, `SceneSize(w, h)`. Extension helpers live in `helpers/extensions/`.

`SceneOffset` direction constants: `Zero`, `Left`, `Right`, `Up`, `Down`, `UpLeft`, `UpRight`, `DownLeft`, `DownRight`.

`Scale` holds independent `horizontal` and `vertical` float factors. `Scale.Unit = Scale(1f, 1f)`.

### `Timer` (`helpers/Timer.kt`)
Utility for time-based callbacks:
```kotlin
val timer = Timer(timeInMilliseconds = 500L, shouldTriggerMultipleTimes = false) { /* onDone */ }
// In Dynamic.update():
timer.update(deltaTimeInMilliseconds)
```

## Plugins

Each plugin requires its Manager to be passed to `Kubriko.newInstance()`. Omitting the Manager means the plugin's Traits will not function.

| Plugin | Manager(s) | Traits | Artifact |
|---|---|---|---|
| `audio-playback` | `MusicManager` (streaming, looping, volume), `SoundManager` (low-latency SFX, simultaneous) | — | `plugin-audio-playback` |
| `collision` | `CollisionManager` | `Collidable` (defines `CollisionMask`), `CollisionDetector : Collidable` (receives `onCollisionDetected`) | `plugin-collision` |
| `keyboard-input` | `KeyboardInputManager` | `KeyboardInputAware` (`onKeyPressed`, `onKeyReleased`, `handleActiveKeys`) | `plugin-keyboard-input` |
| `particles` | `ParticleManager(cacheSize)` | `ParticleEmitter<S>` (continuous or burst emission, pooled `ParticleState`) | `plugin-particles` |
| `persistence` | `PersistenceManager(fileName)` | — | `plugin-persistence` |
| `physics` | `PhysicsManager(initialGravity)` | `RigidBody` (mass, friction, forces/impulses), `JointWrapper` | `plugin-physics` |
| `pointer-input` | `PointerInputManager` | `PointerInputAware` (`onPointerPressed/Released/Moved`, zoom/drag) | `plugin-pointer-input` |
| `serialization` | `SerializationManager` (via `SerializableMetadata.newSerializationManagerInstance(...)`) | `Serializable<T>` (save/restore state as string) | `plugin-serialization` |
| `shaders` | `ShaderManager` | `Shader` (SKSL, layer-based), `ContentShader` (reads existing scene as input) | `plugin-shaders` |
| `sprites` | `SpriteManager` | — | `plugin-sprites` |

**Collision masks**: `PointCollisionMask`, `CircleCollisionMask`, `BoxCollisionMask` (rotatable), `PolygonCollisionMask`.

**Audio formats**: SFX → WAV (max 48k bitrate for Android). Music → MP3 (max 320 kbps). Audio playback only works on Android and Desktop (JVM).

**Shaders**: Written in SKSL. Built-in collection in `com.pandulapeter.kubriko.shaders.collection`: `BlurShader`, `ChromaticAberrationShader`, `RippleShader`, `VignetteShader`, `ComicShader`, `SmoothPixelationShader`. Shaders are Actors.

**Persistence**: Returns `MutableStateFlow`s that auto-persist on value change. Types: `boolean`, `int`, `float`, `string`, `generic(serializer, deserializer)`.

**Sprites**: `SpriteManager.get(resource)` returns null while loading (triggers load on first call). `AnimatedSprite` handles frame-based animation from sprite sheets. Preload with `spriteManager.preload(...)`.

**Physics**: Based on KPhysics/JPhysics. Bodies can be circular or polygonal. Supports joints, ray casting, and explosion forces.

## Tools

Not for production builds. Swapped in/out via `gradle.properties` flags (`isDebugMenuEnabled`, `isSceneEditorEnabled`).

- **`debug-menu`** (`tool-debug-menu`) — in-game overlay: log viewer, actor inspector, perf metrics. Wrap `KubrikoViewport` with `DebugMenu(kubriko, isEnabled) { ... }`. Toggle with `DebugMenu.toggleVisibility()`.
- **`debug-menu-api`** / **`debug-menu-noop`** — public API surface and blank noop; consumers depend on these, not on `debug-menu` directly.
- **`scene-editor`** (`tool-scene-editor`, Desktop only) — visual scene editor: property inspector, transform tools, serialization to/from JSON. Launch as standalone window with `SceneEditor.show(...)` or embed as Composable. Actors must implement `Editable` and use `@Exposed` annotation on setters to appear in the editor.
- **`scene-editor-api`** / **`scene-editor-noop`** — same -api/-noop split as debug-menu.
- **`logger`** (`tool-logger`) — `Logger.log(message, details, source, importance)`. Observable via `Logger.logs: StateFlow`. Already part of the engine's API; no separate dependency needed for basic logging.
- **`ui-components`** — Compose components for the Kubriko visual style, shared by tools and the Showcase app.

The `Editable` trait from `scene-editor-api` can be combined with `Serializable` for full scene save/load integration.

## Conventions

- **Convention plugins**: module build files apply `kubriko-library`, `kubriko-compose-library`, and/or `kubriko-public-artifact` (defined in `gradle/build-logic/`). A published module sets its artifact via `artifactMetadata { artifactId = "..." }`. When adding a module, mirror an existing sibling's build file rather than re-deriving KMP/Compose setup.
- **Type-safe project accessors** are enabled — depend on modules with `projects.engine`, `projects.plugins.physics`, etc., not string paths.
- **`-api` / `-noop` split**: `scene-editor` and `debug-menu` each have an `-api` module (public surface) plus a `-noop` blank implementation, swapped in via the `gradle.properties` flags so tools are excluded from production builds. The `examples/test-*` modules follow the same pattern with `test-*-noop`.
- New modules must be registered in `settings.gradle.kts`.
- Every source file starts with the MPL-2.0 license header (the project is licensed MPL-2.0; derivative engines/forks must stay open source).
- Geometry uses custom value types (`SceneUnit`, `SceneOffset`, `SceneSize`, `Scale`, `AngleRadians`/`AngleDegrees`) rather than raw floats/Compose units — use them and their extension helpers in `helpers/extensions/`.

## Common patterns

**Typical Actor**:
```kotlin
class MyActor : Actor, Visible, Dynamic, Unique {
    override val body = BoxBody(
        initialPosition = SceneOffset.Zero,
        initialSize = SceneSize(100f.sceneUnit, 100f.sceneUnit),
    )
    override val layerIndex = 0
    private lateinit var stateManager: StateManager

    override fun onAdded(kubriko: Kubriko) {
        stateManager = kubriko.get()
    }
    override fun update(deltaTimeInMilliseconds: Int) { /* game logic */ }
    override fun DrawScope.draw() { drawRect(Color.Red, size = body.size.raw) }
}
```

**Typical custom Manager**:
```kotlin
class MyManager : Manager() {
    private val actorManager by manager<ActorManager>()     // resolved at init
    private val myState by autoInitializingLazy { /* computed after init */ }

    override fun onInitialize(kubriko: Kubriko) { /* setup */ }
    override fun onUpdate(deltaTimeInMilliseconds: Int) { /* per-frame logic */ }
    override fun onDispose() { /* cleanup */ }
}
```

**Multiple Kubriko instances**: valid (e.g. a background layer and a game layer sharing some Managers). Each instance has its own Manager set. Managers can be shared between instances.

## Performance

Performance is of paramount importance in this codebase. The engine runs every frame and must sustain smooth frame rates across all target platforms, including lower-end Android and Web (Wasm) targets.

Concrete rules:
- **Zero per-frame allocation in hot paths.** Code that executes every tick (inside `onUpdate`, `draw`, comparators, filters on actor lists) must not allocate heap objects. This rules out lambdas that capture, boxed primitives (e.g. `List<Int>` instead of `IntArray`, `List<Float>` instead of `FloatArray`), unnecessary `map`/`filter` chains that produce intermediate lists, and any approach that trades allocations for code simplicity.
- **Sort in place.** Prefer `sortWith` on existing `ArrayList`s over `sortedWith`/`sortedByDescending`, which always allocate a new list.
- **Prefer primitives.** Use `FloatArray`, `IntArray`, etc. wherever a sequence of primitive values is needed. Kotlin generic collections box primitives.
- **Snapshot keys correctly without boxing.** When a sort key must be captured once (e.g. to fix a `-0.0f` vs `+0.0f` comparator inconsistency), prefer a targeted fix (e.g. `+ 0f` inside the comparator) over a snapshot approach that allocates intermediate collections.

## Known platform limitations

- **Web (Wasm)**: no multi-touch support (Compose limitation). iOS browsers have significant issues (performance, audio, frequent freezes). Chrome/Firefox desktop is near-JVM quality.
- **Desktop (JVM)**: no multi-touch support (Compose limitation).
- **Audio**: only Android and Desktop (JVM); not available on Web or iOS.
- **Shaders**: SKSL only; behavior may differ across platforms if GPU support varies.
