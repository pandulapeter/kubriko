<!--
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025-2026.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
-->
# plugin-persistence

Key-value store with per-platform backends. Values are exposed as `MutableStateFlow`s that auto-persist on change.

## Key Files

- `src/commonMain/.../PersistenceManagerImpl.kt` — implementation; initialization deferred to `Composable()`
- `src/commonMain/.../implementation/PersistedPropertyWrapper.kt` — typed wrappers owning `MutableStateFlow`
- `src/commonMain/.../implementation/KeyValuePersistenceManager.kt` — `expect` interface, one `actual` per platform

## Critical: Storage Not Available Until First Composition

`createKeyValuePersistenceManager` is `@Composable` and called from `PersistenceManager.Composable()`. **Values are at `defaultValue` until after `KubrikoViewport` first composes.** Observe the flow; do not read `.value` eagerly at startup.

## Flow Creation and Caching

Call `persistenceManager.boolean(key, default)` (or `int`, `float`, `string`, `generic`) during `Manager.onInitialize` — not inside hot paths. Each `key` maps to exactly one `PersistedPropertyWrapper` cached in `stateFlowMap`. Calling the same key+type twice returns the same `MutableStateFlow` instance.

## Auto-Persist Mechanism

Every `MutableStateFlow.value` change triggers a coroutine write on `Dispatchers.Default` via `flow.onEach { wrapper.save(...) }.launchIn(scope)`. **There is no debounce.** For high-frequency values (score, position), keep a local variable and push to the flow only on meaningful events (level end, pause).

## Storage Backends

| Platform | Backend | Key scoping |
|---|---|---|
| Android | `SharedPreferences` (MODE_PRIVATE) | `fileName` = prefs file name; bare key names safe |
| Desktop | `java.util.prefs.Preferences.userRoot().node(fileName)` | bare key names safe within the node; the node is shared by every JVM program of the user |
| iOS | `NSUserDefaults.standardUserDefaults` | every type stored under `${fileName}_key` |
| Web | `localStorage` | keys prefixed with `${fileName}_`; shared by every page of the same origin |

On desktop and the web, `fileName` is the whole namespace outside the app, so it has to be unique per game (a reverse-domain name); two games keeping the default `"kubrikoPreferences"` overwrite each other's values. Changing `fileName` silently loses all previously saved data on every platform (Android selects another `SharedPreferences` file, desktop another node, iOS and the web another key prefix).

Older iOS builds wrote strings (and so `generic` values) under the bare key. When the prefixed key does not exist yet, `getString` falls back to the bare key once; the immediate save then moves the value under the prefixed key. The bare key is never deleted (the shared defaults domain may belong to the host app). Caveats, both limited to the first launch after the update: the bare key is shared by every `fileName`, so two managers with the same string key both import whichever wrote last; and a host-app key with the same name as a Kubriko key is imported once.

## `generic` Type

```kotlin
persistenceManager.generic("key", defaultValue, serializer = { it.toJson() }, deserializer = { it.fromJson() })
```

Stored as a `String`. The deserializer is only called with a previously stored string: nothing stored yields `defaultValue` without calling it, and a deserializer that returns null or throws also falls back to `defaultValue`. Every load and save is non-throwing — failures are logged (when logging is enabled) and a failed save leaves storage as it was, while the next change is attempted again. Prefer `kotlinx.serialization` JSON.

## Typical Usage

```kotlin
class PrefsManager(private val persistenceManager: PersistenceManager) : Manager() {
    private lateinit var _soundEnabled: MutableStateFlow<Boolean>
    val soundEnabled get() = _soundEnabled.asStateFlow()

    override fun onInitialize(kubriko: Kubriko) {
        _soundEnabled = persistenceManager.boolean("soundEnabled", true)
    }
    fun toggleSound() { _soundEnabled.update { !it } }
}
```

## Gotchas

- `fileName` must not contain `/` (a path separator for `java.util.prefs` and Android's `getSharedPreferences`, both throw) and should be at most 80 characters (`Preferences.MAX_NAME_LENGTH`)
- Multiple `Kubriko` instances sharing one `PersistenceManager` share the same key namespace — use distinct key prefixes per game area
- Desktop's `java.util.prefs` rejects keys over 80 characters and values over 8192 characters, and the browser's `localStorage` has a quota — such values are silently not persisted (the in-memory flow still holds them)
- Do not call `persistenceManager.boolean(...)` inside `onUpdate` — each call allocates a wrapper if the key is new
