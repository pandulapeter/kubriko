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
| Desktop | `java.util.prefs.Preferences.userRoot().node(fileName)` | bare key names safe |
| iOS | `NSUserDefaults.standardUserDefaults` | keys prefixed with `${fileName}_` |
| Web | `localStorage` | keys prefixed with `${fileName}_` |

iOS and Web prefix every stored key — changing `fileName` on those platforms silently loses all previously saved data.

## `generic` Type

```kotlin
persistenceManager.generic("key", defaultValue, serializer = { it.toJson() }, deserializer = { it.fromJson() })
```

Stored as a `String`. A null/empty stored string yields `defaultValue` (null from deserializer silently falls back). Prefer `kotlinx.serialization` JSON.

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

- Multiple `Kubriko` instances sharing one `PersistenceManager` share the same key namespace — use distinct key prefixes per game area
- `unloadAll()` / `unload()` are fire-and-forget coroutines; resources are not freed synchronously
- Do not call `persistenceManager.boolean(...)` inside `onUpdate` — each call allocates a wrapper if the key is new
