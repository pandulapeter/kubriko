# Provide a URI handler that ignores links the platform cannot open instead of crashing

**Challenged:** sound

**Kind:** bug  ·  **Severity:** medium  ·  **Platforms:** Android, Desktop (Windows, macOS, Linux)
**Files:** `app/shared/src/commonMain/kotlin/com/pandulapeter/kubrikoShowcase/KubrikoShowcase.kt`,
`app/shared/CLAUDE.md`

## Problem

Every external link in the Showcase is opened with the platform `UriHandler`, uncaught, from a click handler:

- `implementation/ui/about/AboutScreen.kt`: `uriHandler.openUri("https://github.com/pandulapeter/kubriko")`,
  `…/issues/new`, `uriHandler.openUri("mailto:pandulapeter@gmail.com?subject=Kubriko")` ("Drop me a line"),
  `https://pandulapeter.com/`, the privacy policy, and `ReviewButton(uriHandler)`.
- `implementation/ui/about/ReviewButton.android.kt` / `ReviewButton.ios.kt`: the store listing.
- `implementation/ui/welcome/WelcomeScreen.kt`: repository, getting started, documentation, Discord.
- `implementation/ui/licenses/LicensesScreen.kt`: `.clickable { uriHandler.openUri(dependency.url) }`.

The platform handlers throw when nothing can open the link (verified against the sources in the Gradle cache for the
versions in `libs.versions.toml`, Compose Multiplatform 1.12.1 / androidx ui 1.12.1):

- Android, `AndroidUriHandler.openUri` rethrows `ActivityNotFoundException` as
  `IllegalArgumentException("Can't open $uri.")`. A device with no mail app (Android TV, many emulators, work profiles,
  a disabled Gmail) crashes the app on "Drop me a line"; one without a browser crashes on every link.
- Desktop, `DesktopUriHandler.openUri` throws `UnsupportedOperationException` on Windows/macOS when AWT does not
  support `Desktop.Action.BROWSE`, and `Desktop.browse` itself throws `IOException` when the OS has no handler for the
  scheme (e.g. `mailto:` with no mail client on Windows, or a Linux desktop without a registered browser).

The exception propagates out of the click handler and takes the app down.

## Fix

In `KubrikoShowcase`, wrap the whole content (outside `KubrikoTheme`, so every screen, dialog and platform
`ReviewButton` is covered) in a provider that replaces `LocalUriHandler` with a guarded handler:

```kotlin
val platformUriHandler = LocalUriHandler.current
val uriHandler = remember(platformUriHandler) {
    object : UriHandler {
        override fun openUri(uri: String) {
            try {
                platformUriHandler.openUri(uri)
            } catch (_: Exception) {
            }
        }
    }
}
CompositionLocalProvider(LocalUriHandler provides uriHandler) { … existing content … }
```

Catching `Exception` is deliberate: the common source set cannot name `java.io.IOException`, `openUri` is not a
suspending call (so no `CancellationException` concern), and the three concrete types above are all subclasses. The
call sites keep using `LocalUriHandler.current` unchanged. Give the handler a short KDoc if it is extracted into a
private class (e.g. `SafeUriHandler`); keep it in `KubrikoShowcase.kt`.

**Decision needed:** should a link that cannot be opened fail silently, or tell the user? — recommended: fail
silently. The Showcase has no snackbar host today; feedback would need one plus a new string such as
`error_link_cannot_be_opened` in `app/shared/src/commonMain/composeResources/values/strings.xml` (read through
`stringResource`, per the `code-style` skill). If the user picks feedback, add that string and a snackbar, and list
`strings.xml` in this plan's files.

Add one line to the `app/shared/CLAUDE.md` "Key files" entry for `KubrikoShowcase.kt` (or its section) noting that it
provides a `LocalUriHandler` that swallows platform failures.

## Tests

None: the wrapper is a few lines of Compose wiring, and the failure only happens with the real platform handlers; the
app has no test source set for Composables.

## Manual check

Android (any API level; e.g. an API 35 "Google APIs" emulator, or a device after
`adb shell pm disable-user --user 0 com.google.android.gm` and with no other mail app):

1. Open About → "Drop me a line".
2. Before the fix: the app crashes with `IllegalArgumentException: Can't open mailto:…`. After: nothing happens and the
   app keeps running. Re-enable Gmail afterwards with `adb shell pm enable com.google.android.gm`.
3. The other About and Welcome buttons still open the browser.

Desktop (Windows 11 with no default mail client configured): About → "Drop me a line" no longer throws; the web links
still open the browser.
