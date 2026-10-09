# Stop forcing a non-mixable Playback audio session on iOS

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** iOS
**Challenged:** sound
**Files:** `plugins/audio-playback/src/iosMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/MusicPlayer.ios.kt`, `plugins/audio-playback/src/iosMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/SoundPlayer.ios.kt`, a new `plugins/audio-playback/src/iosMain/kotlin/com/pandulapeter/kubriko/audioPlayback/implementation/IosAudioSession.kt`, `plugins/audio-playback/CLAUDE.md`, `plugins/audio-playback/README.md`

Ships in `io.github.pandulapeter.kubriko:plugin-audio-playback`. **Decision** (observable behaviour) — see below.

## Problem

Both iOS players configure the shared audio session as soon as they are created in `Manager.Composable()`:

```kotlin
// MusicPlayer.ios.kt and SoundPlayer.ios.kt, init of the anonymous player
AVAudioSession.sharedInstance().apply {
    setCategory(AVAudioSessionCategoryPlayback, error = null)
    setActive(true, error = null)
}
```

`AVAudioSessionCategoryPlayback` without options is non-mixable and ignores the Ring/Silent switch. So merely showing a
`KubrikoViewport` with a `MusicManager` or `SoundManager` registered (even before anything plays):

- stops whatever the user was listening to (Spotify, a podcast) — games are expected not to do this;
- plays the game's sounds with the phone on silent;
- silently overwrites any category the host app configured itself (e.g. a game that wants `Playback` with
  `mixWithOthers`, or a host app with its own audio), every time a manager's player is created.

Neither the Showcase nor Tesselar configures `AVAudioSession` itself (grepped), so nothing in the repo depends on the
current category.

## Decision

Options:

- **(a) Ambient by default, host-configured sessions left alone (recommended).** Set
  `AVAudioSessionCategoryAmbient` only when the session still has the system default category
  (`AVAudioSessionCategorySoloAmbient`), i.e. nobody configured it; otherwise keep the host's category and only
  activate. Ambient mixes with other apps' audio and follows the silent switch, the standard choice for games. No API
  change; a host that wants `Playback` sets it before the first composition. Observable: Kubriko audio becomes
  silent with the switch on silent and no longer stops other apps' audio (the Showcase included).
- (b) `Playback` with `AVAudioSessionCategoryOptionMixWithOthers`: mixes with other apps but still ignores the silent
  switch. Smaller behavioural change, but not what most games want.
- (c) A new public parameter (e.g. on `MusicManager.newInstance`/`SoundManager.newInstance`) choosing the category:
  an API addition in common code for an iOS-only concern, and two managers could ask for different categories. Not
  recommended; (a)'s "configure it yourself first" covers the need.
- (d) Keep today's behaviour and only document it.

## Fix (option a)

1. New `IosAudioSession.kt` (MPL header) with `internal fun configureAudioSession()`:
   ```kotlin
   val session = AVAudioSession.sharedInstance()
   if (session.category == AVAudioSessionCategorySoloAmbient) {
       session.setCategory(AVAudioSessionCategoryAmbient, error = null)
   }
   session.setActive(true, error = null)
   ```
   (one short comment on why SoloAmbient means "not configured by the host").
2. Replace the `init` blocks of the players in `MusicPlayer.ios.kt` and `SoundPlayer.ios.kt` with
   `init { configureAudioSession() }` and drop the now-unused imports.
3. Docs: in `plugins/audio-playback/CLAUDE.md`, the iOS row of the Platform Backends table says
   `AVAudioPlayer` + Ambient session unless the host configured one; in `plugins/audio-playback/README.md` add a short
   "iOS audio session" note under Technical Details: Kubriko uses the Ambient category (mixes with other audio,
   follows the silent switch) unless the app sets its own category before the first composition of `KubrikoViewport`,
   with a one-line Kotlin example of setting `AVAudioSessionCategoryPlayback`.

If the user picks (b), step 1 sets `Playback` with `AVAudioSessionCategoryOptionMixWithOthers` under the same
"only when unconfigured" check, and the docs say so.

## Tests

None: iOS test tasks are disabled and the code is platform interop only. Compile with
`./gradlew :plugins:audio-playback:build` on a Mac (iOS targets).

## Manual check

On an iPhone: start music in another app, open the Showcase and a game with music — the other app's music must keep
playing alongside the game's. Flip the silent switch on — the game's music and sound effects must go silent. Then, in a
scratch build that sets `AVAudioSessionCategoryPlayback` before the viewport is shown, confirm Kubriko leaves it in
place (game audio plays with the switch on silent).
