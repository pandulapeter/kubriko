# Drop the `removePrefix` that never matches from the web shell's `initialPath`

**Kind:** refactor  ·  **Severity:** low  ·  **Platforms:** web  ·  **Class:** Now
**Artifact:** unpublished (app)
**Files:**
- `app/web/src/webMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcaseApp.kt`

## Problem
`KubrikoShowcaseApp.kt:32` (at 2480325f):

```kotlin
val initialPath = remember { window.location.pathname.removePrefix(BuildConfig.WEB_ROOT_PATH_NAME) }
val rootPath = remember { if (window.location.pathname == "/") "/" else "/${BuildConfig.WEB_ROOT_PATH_NAME}/" }
```

`BuildConfig.WEB_ROOT_PATH_NAME` is `"kubriko"` (`gradle.properties`: `showcase.webRootPathName=kubriko`, injected by
`app/shared/build.gradle.kts`), while `Location.pathname` of an http(s) page always starts with `/` — also after
`index.html`'s spa-github-pages snippet, which rewrites the URL with `history.replaceState(null, null, l.pathname.slice(0, -1) + decoded + l.hash)`
before the app starts. So the prefix never matches, and `initialPath` is always the raw pathname. That is also what the
only reader relies on:

```kotlin
if (deeplink == null && currentPath.value.removePrefix(rootPath).isNotBlank() && initialPath == rootPath) {
    window.history.back()
```

compares it to `rootPath` (`"/"` on the dev server, `"/kubriko/"` deployed), i.e. "the session started at the root" — a
comparison that would break (never true when deployed) if the strip ever did match. The call misleads the reader into
thinking paths are stripped here.

## Fix
`val initialPath = remember { window.location.pathname }`. Nothing else.

## Behaviour
`removePrefix` on a string that does not start with the prefix returns it unchanged, so `initialPath` is the same value
in every deployment (dev server at `/`, GitHub Pages under `/kubriko/`, deep links, the 404 redirect). Unchanged.
`app/web/CLAUDE.md`'s "`WEB_ROOT_PATH_NAME` … is stripped from all paths before processing" stays true (that is
`rootPath`'s `removePrefix`).

## Public API
None.

## Tests
None (the web shell has no test source set; the reasoning above is the proof).

## Verify
`./gradlew :app:web:compileKotlinWasmJs`

## Manual check
`./gradlew :app:web:wasmJsBrowserDevelopmentRun`: open the root, open an entry, go back with the in-app back button → the
browser history steps back to the root (as before); the browser's own back / forward buttons still switch between the
menu and the entry.
