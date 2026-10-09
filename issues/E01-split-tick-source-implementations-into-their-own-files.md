# Split the four TickSource implementations out of TickSource.kt into files of their own

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** all  ·  **Class:** Now
**Artifact:** engine
**Files:**
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/TickSource.kt`
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/ManualTickSource.kt` (new)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/ViewportFrameTickSource.kt` (new)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/FixedRateTickSource.kt` (new)
- `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/helpers/FixedFrequencyTickSource.kt` (new)

## Problem

`TickSource.kt` (344 lines at 2480325f) holds the abstract `TickSource` with its companion factories plus four
subclasses, against the "one file, one thing" rule of `code-style`:

```kotlin
class ManualTickSource : TickSource() {                       // :260 (public, with KDoc at :257-259)
internal class ViewportFrameTickSource(                       // :268
internal class FixedRateTickSource(                           // :275
internal class FixedFrequencyTickSource(                      // :304
```

## Fix

Move each class verbatim (its KDoc with it) into a file named after it, same package
`com.pandulapeter.kubriko.helpers`, each starting with the MPL-2.0 header copied from `TickSource.kt`:

| Declaration | New file |
|---|---|
| `class ManualTickSource` with its KDoc `/** A [TickSource] that advances only when [tick] is called. */` | `ManualTickSource.kt` |
| `internal class ViewportFrameTickSource` | `ViewportFrameTickSource.kt` |
| `internal class FixedRateTickSource` | `FixedRateTickSource.kt` (imports `Job`, `delay`, `isActive`, `launch`, `Mutex`, `withLock`) |
| `internal class FixedFrequencyTickSource` | `FixedFrequencyTickSource.kt` (imports `Job`, `delay`, `isActive`, `launch`, `Mutex`, `withLock`, `nanoseconds`, `TimeSource`) |

`TickSource.kt` keeps `abstract class TickSource` and its companion (`manual()`, `viewportFrames()`, `fixedRate()`,
`fixedFrequency()`), which keep constructing the moved classes (same package, no import needed). Trim the imports of
`TickSource.kt` to what it still uses: `Job`, `delay`, `isActive`, `launch`, `Mutex`, `withLock`, `nanoseconds` and
`TimeSource` leave; `Kubriko`, `KubrikoImpl`, `Logger`, `CoroutineScope`, `MutableStateFlow`, `StateFlow`, `Volatile`,
`AtomicBoolean`, `ExperimentalAtomicApi` stay (re-check each by grep in the trimmed file).

Nothing changes visibility: the subclasses only use `protected` members of `TickSource` (`scope`, `emitTick`), which
stay accessible from another file. Compare the sorted declaration lists before/after (`git diff --color-moved`).

After the move, grep the whole repository (docs, `CLAUDE.md` files, skills, `documentation/`) for
`TickSource.kt` and the four class names; at 2480325f the only file reference is `helpers/TickSource.kt` for
`TickSource` itself (root `CLAUDE.md`, `engine/README.md`), which stays correct.

## Behaviour
Unchanged: same classes, same package, same bodies.

## Public API
None. `ManualTickSource` is a public class and stays in its package; classes carry no file facade.

## Tests
The existing ones (`TickSourceFactoryTest`, `TickSourceLifecycleTest`).

## Verify
`./gradlew :engine:compileKotlinDesktop :engine:compileKotlinWasmJs :engine:compileKotlinIosSimulatorArm64 :engine:desktopTest`

## Manual check
none
