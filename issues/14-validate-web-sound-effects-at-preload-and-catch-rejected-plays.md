# Validate web sound effects at preload and catch rejected plays

**Kind:** bug  ·  **Severity:** low  ·  **Platforms:** Web
**Challenged:** amended — the docs step now states that web sound URIs must be fetchable (same origin or CORS-enabled), as music URIs already are, since a cross-origin URI without CORS that `<audio src>` used to play becomes a failed load.
**Files:** `plugins/audio-playback/src/webMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/SoundPlayer.web.kt`, `plugins/audio-playback/CLAUDE.md`

Ships in `io.github.pandulapeter.kubriko:plugin-audio-playback`. No API change; brings Web in line with the documented
failure handling.

## Problem

The web `preload` never touches the network or checks anything — it creates the elements and returns them:

```kotlin
override suspend fun preload(uri: String) = withContext(Dispatchers.Default) {
    buildList {
        repeat(maximumSimultaneousStreamsOfTheSameSound) {
            add((document.createElement("audio") as HTMLAudioElement).apply { src = uri })
        }
    }
}
```

So on the web a missing (404) or undecodable sound is reported as loaded: `getLoadingProgress()` reaches 1
immediately, the `AudioCache` failure path (log at `HIGH`, retry on the next `play()`) never runs, and the README's
promise ("the failure is logged, ... playing it again retries the load") does not hold. Each element also downloads
the file separately.

`play` ignores the `Promise` returned by `HTMLMediaElement.play()`:

```kotlin
cachedSound.firstOrNull { it.paused }?.play()
```

Before the first user gesture the browser rejects it with `NotAllowedError`, and a broken source rejects with
`NotSupportedError`; every such call logs an "Uncaught (in promise)" error to the console — one per sound effect
played before the player clicks.

## Fix

1. `preload`: `window.fetch(uri).await()`; when the response is not `ok`, or fetching throws anything other than
   `CancellationException`, return `null` (as `WebMusicPlayer.load` does). Otherwise read `response.blob().await()`,
   create one object URL with `URL.createObjectURL(blob)` and give it to every element (`src = objectUrl`), so the
   file is downloaded once. Return a small holder (`private class WebCachedSound(val objectUrl: String, val
   elements: List<HTMLAudioElement>)`) instead of the bare list.
   Do not wait for `canplaythrough`/`loadedmetadata` instead: iOS Safari does not load media before a user gesture,
   so such a wait would hang or time out and mark every sound as failed there.
2. `dispose(cachedSound)`: after pausing and clearing the elements as today, `URL.revokeObjectURL(objectUrl)`.
3. `play`: attach a rejection handler to the promise so it is not unhandled —
   `element.play().catch { null }` (Kotlin/Wasm `Promise.catch`). Nothing else is needed: a rejected element stays
   `paused` and is reused on the next play.
4. `plugins/audio-playback/CLAUDE.md`: in the Web row of the Platform Backends table, note that SFX are fetched once at
   preload (a failed fetch is a failed load) and shared by the element pool through an object URL. Also say, there
   and in the README's "Files That Fail to Load" section, that on the web a sound URI must now be fetchable — same
   origin, or served with CORS headers — exactly like a music URI (`WebMusicPlayer.load` already fetches): an
   `<audio src>` plays a cross-origin file without CORS, `fetch` does not, so such a sound that used to play becomes
   a failed load. Compose Resources URIs (`Res.getUri`, what every example uses) are same-origin and unaffected.

## Tests

None: the Wasm test tasks are disabled and the code is browser interop only.

## Manual check

Run the Showcase on the web with the browser console open. Rename one sound file referenced by a game (or point a
scratch `preload` at a missing path) with logging on: a `HIGH` "Failed to load" log must appear, and the loading
progress must still reach 1. Open a game straight from a deep link and let it play sound effects before clicking: the
console must not fill with "Uncaught (in promise) NotAllowedError". Sound effects must still play normally after the
first click, several at once, in Chrome, Firefox and Safari; the Network tab must show each sound fetched once.
