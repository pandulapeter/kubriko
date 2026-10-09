# Document that `PersistenceManager.newInstance(fileName)` has to be unique per game on desktop and the web, where the default name is shared by every Kubriko game.

**Kind:** docs  ·  **Severity:** medium  ·  **Platforms:** Desktop, Web
**Challenged:** amended — the KDoc/README guidance also says the name must not contain `/` and should stay within 80 characters, since `java.util.prefs` treats `/` as a path separator and rejects longer node names, and Android's `getSharedPreferences` rejects a path separator.
**Files:** `plugins/persistence/src/commonMain/kotlin/com/pandulapeter/kubriko/persistence/PersistenceManager.kt`, `plugins/persistence/README.md`, `plugins/persistence/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-persistence` (KDoc only, no code change).

## Decision
Changing the default is the only way to protect games that already omit `fileName`, and it is a behaviour change.

- **A — document only (recommended).** Keep `fileName: String = "kubrikoPreferences"`. Explain in KDoc and the README that on desktop and the web the name is the whole namespace, and should be unique per game (a reverse-domain name such as `"com.example.mygame"`). Nobody loses saved data.
- **B — make `fileName` required.** Remove the default. Every caller that relies on it stops compiling (source break), and a caller who then picks a new name loses its old saves unless it migrates them.
- **C — derive a default** (for example from the application id). There is no portable way to get an application id on desktop or the web, and every game that relied on the old default silently loses its saves on upgrade.

Known callers all pass a name already: the Showcase games (`"kubrikoSpaceSquadron"`, `"kubrikoWallbreaker"`, `"kubrikoBlockysJourney"`, `"kubrikoAnnoyedPenguins"`), the tools (`"kubrikoDebugMenu"`, `"kubrikoSceneEditor"`), the test (`"kubrikoPersistenceTest"`), and Tesselar (`fileName = "preferences"` in `../Tesselar/ui/src/commonMain/kotlin/com/pandulapeter/tesselar/ui/scene/GameSession.kt`). So B and C would affect only external games. Tesselar's `"preferences"` is itself a generic name that this documentation warns against (outside this sweep's scope, for the user to decide).

## Problem
The storage backends scope by `fileName` alone:

```kotlin
// desktopMain KeyValuePersistenceManager.desktop.kt
private val preferences by lazy { Preferences.userRoot().node(fileName) }
```
```kotlin
// webMain KeyValuePersistenceManager.web.kt
private val preferences by lazy { localStorage }
private fun String.prefixed() = "${fileName}_$this"
```
```kotlin
// PersistenceManager.kt
fun newInstance(
    fileName: String = "kubrikoPreferences",
```

On Android (`SharedPreferences`, private to the app) and iOS (`NSUserDefaults.standardUserDefaults`, private to the app) the name only has to be unique within one app. On desktop, `java.util.prefs` user preferences are shared by every JVM program the user runs: macOS keeps them all in `~/Library/Preferences/com.apple.java.util.prefs.plist`, Linux under `~/.java/.userPrefs/<node>/`, Windows under `HKCU\Software\JavaSoft\Prefs\<node>`. On the web, `localStorage` is shared by every page on the same origin, and hosts that serve several games from one origin (for example a GitHub Pages user site) are common. So two games that both use the default name, or the same generic name, read and overwrite each other's values with the same keys (`"high_score"`, `"isSoundEnabled"`, …). The KDoc says only `@param fileName The name of the storage file.`, and the README example uses `"my_game_prefs"` without saying why the name matters.

## Fix
1. `PersistenceManager.newInstance` KDoc, `@param fileName`: say that it names the storage namespace; that on desktop (`java.util.prefs` user node) and the web (`localStorage` key prefix) that namespace is shared with every other program of the same user or every page of the same origin, so it should be unique to the game, such as a reverse-domain name; that the default `"kubrikoPreferences"` is shared by every game that keeps it; and that changing it later loses previously saved values (each backend looks them up only under the current name);
   and that it must not contain `/` and should be at most 80 characters (`java.util.prefs` treats `/` as a path
   separator and throws for node names over `Preferences.MAX_NAME_LENGTH` = 80; Android's `getSharedPreferences`
   throws for a name containing a path separator).
2. `plugins/persistence/README.md`, section "1. Register the Manager": change the example to a reverse-domain name (`fileName = "com.example.mygame"`) and add one or two sentences with the same warning.
3. `plugins/persistence/CLAUDE.md`, "Storage Backends" table and "Gotchas": add that the desktop node and the web origin are shared outside the app, and that changing `fileName` loses saved data on every platform (today the file says this only for iOS and the web; on Android it selects another `SharedPreferences` file and on desktop another node, so it loses data there too).

Do not add a runtime warning for the default name: it would only show with logging enabled, and nothing tells a correctly named game apart from one that deliberately shares.

If the user picks B or C instead, that is a separate, behaviour-changing plan; this documentation is still needed.

## Tests
None: documentation only.

## Manual check
None. Read the rendered KDoc (`./gradlew :plugins:persistence:build` still passes) and the README.
