# Save strings on iOS under the same prefixed key they are read from

**Challenged:** sound

**Kind:** bug (data loss)  ·  **Severity:** high  ·  **Platforms:** iOS  ·  **Artifact:** `plugin-persistence`
**Files:** `plugins/persistence/src/iosMain/kotlin/com/pandulapeter/kubriko/persistence/implementation/KeyValuePersistenceManager.ios.kt`, `plugins/persistence/CLAUDE.md`

**Decision needed:** should values already written under the bare (unprefixed) key by the buggy build be picked up once, as a fallback read when the prefixed key is absent? — recommended: **yes, fallback read of the bare key, never deleted**.

## Problem

At 0008d027 the iOS backend writes strings to one key and reads them from another:

```kotlin
override fun getString(key: String, defaultValue: String): String {
    preferences.registerDefaults(mapOf(key.prefixed() to defaultValue))
    return preferences.stringForKey(key.prefixed()) ?: defaultValue
}

override fun putString(key: String, value: String) = preferences.setValue(value, key)

private fun String.prefixed() = "${fileName}_$this"
```

`putString` stores under the bare `key`, `getString` reads `"${fileName}_$key"`. Every `PersistenceManager.string(...)` and `generic(...)` value (both go through `putString`) is therefore back at its default after every relaunch on iOS. The boolean/int/float paths use `key.prefixed()` on both sides and are fine; Android, desktop and web are fine. In the repo this hits the debug menu's persisted log `filter` (`tools/debug-menu/.../InternalDebugMenu.kt`, `persistenceManager.string(key = "filter", ...)`); for consumers it hits every string or JSON-backed setting/save on iOS.

What the bare key holds today: `PersistenceManagerImpl` saves the loaded value right away (the `flow.onEach { save }` collector fires on the current value), so each launch first writes the default to the bare key and then whatever the player changed it to. The bare key therefore holds the value the last session ended with — recoverable.

## Fix

1. `putString`: `preferences.setObject(value, forKey = key.prefixed())` (drop the `platform.Foundation.setValue` import if it becomes unused).
2. `getString` — the options for already-affected installs:
   - **(recommended) Fallback read.** `return preferences.stringForKey(key.prefixed()) ?: preferences.stringForKey(key) ?: defaultValue`, and drop the `registerDefaults` call from `getString` (the `?: defaultValue` already supplies the default, and a registered default would make `stringForKey(key.prefixed())` non-null and hide the fallback). Because the load is immediately followed by a save, the value lands under the prefixed key on the first launch and the fallback is never consulted again for that key. Never remove the bare key: it lives in the app's shared `standardUserDefaults` domain and may be a key the host app owns.
     Caveats to write into `CLAUDE.md`: the bare key is shared by every `fileName`, so two `PersistenceManager`s with different `fileName`s that both used the same string key will both import whichever wrote last; and a host-app key with the same name as a Kubriko key would be imported once. Both only affect the first launch after the update and only when the prefixed key does not exist yet.
   - **No migration.** Only fix `putString`; users lose their pre-fix strings once (they already lose them on every launch today).
3. `plugins/persistence/CLAUDE.md` → "Storage Backends": state that on iOS every type is stored under `${fileName}_key`, and (if the fallback is taken) describe the one-time bare-key fallback for strings with the caveats above.

## Tests

None can be written: the code is in `iosMain` against `NSUserDefaults`, and iOS simulator test tasks are not part of this repo's verification (`./gradlew desktopTest`). Compile-check with `./gradlew :plugins:persistence:compileKotlinIosSimulatorArm64` (or the `iosArm64` variant available on the machine).

## Manual check

On an iOS simulator, with `showcase.isDebugMenuEnabled=true`: open the Showcase, open the debug menu, type a log filter, kill the app (swipe away), relaunch → the filter text is still there. For the migration: install the build from 0008d027, type a filter, quit, install the fixed build over it, launch → the filter from the old build shows up.
