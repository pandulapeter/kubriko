# Fall back to the default when a persisted value is missing or cannot be read, and never let a failed save escape

**Challenged:** sound

**Kind:** bug (crash)  ·  **Severity:** high  ·  **Platforms:** all (crash on Android/iOS, silent stop on desktop/web)  ·  **Artifact:** `plugin-persistence`
**Files:** `plugins/persistence/src/commonMain/kotlin/com/pandulapeter/kubriko/persistence/implementation/PersistedPropertyWrapper.kt`, `plugins/persistence/src/commonMain/kotlin/com/pandulapeter/kubriko/persistence/implementation/KeyValuePersistenceManager.kt`, the four `KeyValuePersistenceManager.<platform>.kt` actuals (`androidMain`, `desktopMain`, `iosMain`, `webMain`), `plugins/persistence/src/commonMain/kotlin/com/pandulapeter/kubriko/persistence/PersistenceManagerImpl.kt`, `plugins/persistence/src/commonTest/kotlin/com/pandulapeter/kubriko/persistence/PersistedPropertyWrapperTest.kt` (new), `plugins/persistence/CLAUDE.md`, `plugins/persistence/README.md`

Lands after plan 45 (same iOS file).

## Problem

`PersistedPropertyWrapper.Generic.load` (at 0008d027):

```kotlin
override fun load(keyValuePersistenceManager: KeyValuePersistenceManager) = flow.update { deserializer(keyValuePersistenceManager.getString(key, "")) ?: defaultValue }
```

When nothing is stored yet (every first launch) the consumer's deserializer is called with `""`. The public KDoc of `PersistenceManager.generic` says `defaultValue` is "The value to return if no value is found in storage", and the plugin's own README example is

```kotlin
deserializer = { Json.decodeFromString<UserProfile>(it) }
```

which throws `SerializationException` on `""`. The load runs inside `scope.launch(Dispatchers.Default) { wrapper.load(...); ... }` in `PersistenceManagerImpl` (both in `initialize()` and in `Composable()`); the Kubriko scope is `SupervisorJob() + Dispatchers.Default` with no exception handler, so the exception goes to the platform's uncaught handler: **the app crashes on Android and iOS on first launch**, and on desktop/web the stack trace is printed and the value (plus every wrapper after it in the `forEach`) never loads or saves. The same happens for stored data that no longer parses (a changed data class, a corrupted value) — on every launch, forever, since the bad value is never overwritten.

Saving has the same hole: `wrapper.flow.onEach { wrapper.save(keyValuePersistenceManager) }.launchIn(scope)`. A throwing `serializer`, or a backend that rejects the write — desktop `java.util.prefs.Preferences.put` throws `IllegalArgumentException` for keys over 80 characters or values over 8192 characters; web `localStorage.setItem` throws `QuotaExceededError` — kills that collector (crash on Android/iOS via the uncaught handler; elsewhere the key silently stops persisting for the rest of the session). The typed getters can also throw (Android `SharedPreferences.getInt` on a key stored with another type throws `ClassCastException`).

The in-repo `generic` users (`tools/scene-editor/.../UserPreferences.kt`) tolerate `""`, which is why this has not shown up in the Showcase.

## Fix

1. `KeyValuePersistenceManager`: add `fun getStringOrNull(key: String): String?` and implement it per platform without registering or substituting a default — Android `preferences.getString(key, null)`, desktop `preferences.get(key, null)`, web `preferences.getItem(key.prefixed())`, iOS the same lookup `getString` does after plan 45 minus the default (`stringForKey(key.prefixed())`, plus `?: stringForKey(key)` if plan 45's fallback was chosen; no `registerDefaults`).
2. `Generic.load`: read `getStringOrNull(key)`; when it is `null`, set `defaultValue` without calling the deserializer; otherwise `deserializer(stored) ?: defaultValue`.
3. Make every wrapper's `load` and `save` non-throwing: wrap the body in a `try`/`catch (throwable: Throwable)` — `Throwable`, not `Exception`, because Kotlin/Wasm surfaces JavaScript errors such as `QuotaExceededError` as `JsException`, which is not an `Exception` subclass (confirm against the stdlib at execution time; neither function suspends, so no `CancellationException` can pass through). On a failed load keep `defaultValue`; on a failed save leave storage as it was. Return `Boolean` (success) from both so the manager can log, or pass a failure callback — whichever reads cleaner; the collector must survive a failed save so the next change is attempted again.
4. `PersistenceManagerImpl`: when a load or save reports failure, `log(message = "Failed to load/save \"$key\"", details = throwable.message, importance = Logger.Importance.HIGH)` (the manager's `log` is already guarded by `isLoggingEnabled`).
5. KDoc of `PersistenceManager.generic` (`PersistenceManager.kt`): say the deserializer is only called with a previously stored string, that `defaultValue` is used when nothing is stored or the deserializer throws, and that a value the backend refuses to store (desktop's 80-character key / 8192-character value limits, the browser's storage quota) is not persisted. This matches the documented intent, so no public behaviour a consumer could rely on changes beyond "no longer crashes"; the one visible difference is that a deserializer is no longer called with `""` on first launch.
6. `plugins/persistence/CLAUDE.md` → "`generic` Type": replace "A null/empty stored string yields `defaultValue`" with the new rule, and add a Gotcha for the desktop size limits. `README.md` → "Custom Types": one sentence that the deserializer only sees stored values and failures fall back to `defaultValue`.

## Tests

`PersistedPropertyWrapperTest` in `commonTest`, with a small in-memory `KeyValuePersistenceManager` fake (a `MutableMap<String, Any>`, optionally configured to throw on put):
- `Generic` with an empty store: after `load`, `flow.value == defaultValue` and a deserializer that records its calls was never invoked.
- `Generic` with a stored `"garbage"` and a deserializer that throws: `load` does not throw and `flow.value == defaultValue`.
- `Generic` with a stored valid value: `flow.value` is the deserialized value.
- A fake whose `putString` throws: `save` does not throw (returns `false`), and a later `save` with a non-throwing fake stores the value.
- A throwing `serializer`: `save` does not throw.

Run `./gradlew :plugins:persistence:desktopTest`, and compile the other platforms (`./gradlew :plugins:persistence:build`).

## Manual check

Android: in a scratch game use `generic(key = "profile", defaultValue = Profile(), serializer = { Json.encodeToString(it) }, deserializer = { Json.decodeFromString(it) })`, clear app data, launch → no crash, default profile shown. Desktop: persist a `string` longer than 8192 characters → no stack trace from a dead collector, and a subsequent short value still persists across a restart.
