# Make the Manager lookup cache safe to use from several threads

**Challenged:** amended — the test is a smoke test that also passes at 0008d027 on the JVM (a lost `HashMap` update just falls back to the linear search, and the stress run never saw a failure); the plan now says so, so the executor does not try to make it fail first.

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** all (worst on iOS/Kotlin Native, where a corrupted `HashMap` can crash)  ·  **Artifact:** `engine`
**Files:** `engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoImpl.kt`, `engine/src/desktopTest/kotlin/com/pandulapeter/kubriko/ManagerLookupConcurrencyTest.kt` (new)

Apply after plan 09 (same file).

## Problem

`KubrikoImpl.get` (0008d027 ~111-122) memoizes lookups in a plain `LinkedHashMap`:

```kotlin
private val managerCache = mutableMapOf<KClass<out Manager>, Manager>()
...
val cached = managerCache[managerType] as? T
if (cached != null) return cached
val found = managers.firstOrNull { managerType.isInstance(it) } as? T ?: throw ...
managerCache[managerType] = found
```

The cache is written lazily, the first time each type is looked up — and that is *after* construction for every type except the four built-ins `requireAndVerify` warms. The first `kubriko.get<SpriteManager>()` (or any plugin/custom type) typically happens in an actor's `onAdded`, which runs on `Dispatchers.Default`, while the main thread can be calling `get()` from composition (`remember { kubriko.get<…>() }`) or from `TickSource.start()` resolving `manager<T>()` delegates. Unsynchronized concurrent `put`s on a `HashMap` can lose entries or corrupt the table (JVM: a lost update at worst, since a miss falls back to the linear search; Kotlin/Native: undefined, can throw). `dispose()` also `clear()`s it from whatever thread disposes. The stress run's 300 instances × 8 threads saw no error, so the race is rare — this is hardening, not an observed crash.

## Fix

Replace the mutable map with copy-on-write in a `kotlin.concurrent.atomics.AtomicReference<Map<KClass<out Manager>, Manager>>` (opt in to `ExperimentalAtomicApi` if needed):
- `get`: read `cache.load()[managerType]`; on a miss do the linear search, then `cache.update { it + (managerType to found) }` (or a `compareAndSet` loop). Reads stay lock-free and allocation-free; the map is copied only on the first lookup of each type.
- `dispose()`: `cache.store(emptyMap())`.

The `isDisposed` check stays first.

## Tests

`ManagerLookupConcurrencyTest` (desktopTest):
- `concurrentFirstLookupsAllResolve` — 200 iterations: a fresh Kubriko with 8 distinct custom Manager classes (`M1 … M8 : Manager()`), 8 threads released by one `CountDownLatch`, each calling `get()` for all 8 types in a different order 100 times; assert every call returns the registered instance and no thread threw. This guards the new code path rather than reproducing the bug: it is expected to pass at 0008d027 too (the JVM race only loses an update, which the linear search masks; the Kotlin/Native failure mode is not reachable from `desktopTest`).

## Manual check

None.
