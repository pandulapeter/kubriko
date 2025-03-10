# App

This module is the entry point for Kubriko Showcase, a multiplatform app that demonstrates the capabilities of the engine.

## Running the app

After cloning the repository and opening it in your preferred IDE ([Android Studio](https://developer.android.com/studio)
or [IntelliJ IDEA](https://www.jetbrains.com/idea/) are recommended), the following entry points should be available:

- Android
- Desktop
- iOS (Mac only)
- Web

For any configuration / tooling issues check out the official documentation
for [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform-intro.html#learn-key-concepts).

## A note on performance

Just like with any other Compose app, release builds of Kubriko Showcase have **significantly** better performance than debug builds.

## Configuring the build

The [gradle.properties](https://github.com/pandulapeter/kubriko/blob/main/gradle.properties) file located in the root folder of the repository contains
some important flags that affect how the Showcase app is built:

- `showcase.areTestExamplesEnabled`
- `showcase.isDebugMenuEnabled`
- `showcase.isSceneEditorEnabled`
- `showcase.shouldShowUnfinishedGames`

Disabling components is done on the buildscript level so that the unwanted resources and the irrelevant source code are never included in the builds.

## Pre-built binaries

The pre-built Kubriko Showcase application can be accessed using the following links:

[<img src="../documentation/images/badge_android_coming_soon.png" alt="Download for Android" height="33px" />](#)
[<img src="../documentation/images/badge_ios_coming_soon.png" alt="Download for iOS" height="33px" />](#)
[<img src="../documentation/images/badge_windows_coming_soon.png" alt="Download for Windows" height="33px" />](#)
[<img src="../documentation/images/badge_macos_coming_soon.png" alt="Download for macOS" height="33px" />](#)
[<img src="../documentation/images/badge_linux_coming_soon.png" alt="Download for Linux" height="33px" />](#)
[<img src="../documentation/images/badge_web.png" alt="Download for Web" height="33px" />](https://pandulapeter.github.io/kubriko/)

## Implementation

The showcase app contains an ever-expanding list of games, demos, and tests that are embedded into a nice and scalable menu system.
Check out how effortlessly Kubriko handles resizing the window!

This module only ties together the various examples implemented under the different modules of
the [examples](https://github.com/pandulapeter/kubriko/tree/main/examples) folder.
It also contains the UI for the Welcome screen, but the examples behind all the other menu items are implemented in the submodules mentioned above.

Due to the nature of this app, it does contain some solutions that should not be necessary under normal circumstances.
Most games will probably not need to be embedded into a menu system, and will probably not support this many Kubriko instances in parallel.

As a result, the `app` module itself might not be a good source for learning the best practices of using the engine.
However, the individual modules within the `examples` folder (linked above) are!