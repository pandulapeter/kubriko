# Declare the touchscreen optional in the Android manifest

**Kind:** config (platform edge case)  ·  **Severity:** low  ·  **Platforms:** Android (Google Play on ChromeOS and
non-touch devices)
**Challenged:** sound
**Files:** `app/android/src/main/AndroidManifest.xml`, `app/android/CLAUDE.md`

Ships in no published artifact (`app/android` is the Showcase).

## Problem

The manifest declares no `<uses-feature>` at all (nor does any library manifest in the repo), so Google Play applies
its default of requiring `android.hardware.touchscreen`. Chromebooks without a touchscreen (most clamshells), and
other non-touch Android devices with a mouse or keyboard, are filtered out of the listing — even though the Showcase
is fully usable there: the pointer-input plugin handles a mouse, the keyboard-input and gamepad-input plugins cover
the games, and the Activity is already `resizeableActivity="true"` for windowed ChromeOS use.

## Fix

Add inside `<manifest>`, before `<application>`:

```xml
<uses-feature
    android:name="android.hardware.touchscreen"
    android:required="false" />
```

Add a line to `app/android/CLAUDE.md` (a "Manifest" note under "Entry point" or "Build configuration"): the
touchscreen is declared optional so Play offers the app on non-touch Chromebooks and other mouse/keyboard devices;
the Showcase must stay playable with a pointer, keyboard or gamepad.

## Decision

Whether to also declare `android.hardware.faketouch` optional (`app/` is unpublished; a store-reach choice):
- **Recommended: no.** A non-touch Chromebook's trackpad/mouse reports `faketouch`, so the touchscreen line alone
  opens it up. Devices without even `faketouch` are essentially Android TV and similar, which additionally need a
  leanback launcher intent and a banner to be listed, and where the Showcase's menus have not been checked for
  D-pad-only use.
- Yes: also add `<uses-feature android:name="android.hardware.faketouch" android:required="false" />`, widening the
  device catalog to pointer-less devices; only worth it together with a deliberate TV target.

## Tests

None: a manifest declaration.

## Manual check

After the next Play upload (Play Console → the release → App bundle explorer / Device catalog): the touchscreen is
no longer listed as a required feature, and a non-touch Chromebook model in the device catalog shows as supported.
On a non-touch Chromebook (or an emulator / ChromeOS device without touch), install from the internal testing track
and check the menu and one game are usable with mouse and keyboard.
