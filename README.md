# Kubriko
**Early development phase**

*A 2D game engine based on Compose Multiplatform*

<img src="documentation/images/logo.png" width="20%"  alt="Kubriko logo"/>

This repository contains the source code of the engine and its plugins / tooling, as well as the Kubriko Showcase application that demonstrates the engine's capabilities.

## 🎮 Overview
Kubriko aims to offer a lightweight, scalable, and easy-to-use solution for creating simple 2D games for **Android**, **Desktop** (Windows, Linux, MacOS), **iOS** and **Web**.
It achieves this goal by exposing a [Composable](https://www.jetbrains.com/compose-multiplatform/) that can be embedded into [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) projects.

The engine is highly modular, because its core functionality can be extended in a granular fashion using different plugins.
These plugins cover most needs of simple games from viewport scaling and object management, through persistence, input handling and audio playback, all the way to shaders and physics simulation.
Kubriko also comes with a Scene Editor and a number of other tools that can be useful during development.

The tech stack is also easy to extend by creating custom plugins, and the low-level integration makes it possible to communicate with platform API-s in a straightforward way.

## 🎨 Showcase app
Clone this repository and run the `app` module for the supported targets (Android, Desktop, iOS, Web) to check out what the engine is capable of.

Once the project reaches a more mature stage of development, I plan to publish the showcase app in relevant stores for all platforms.
For now only the web version is available, in the link below.

[<img src="documentation/images/badge_android_coming_soon.png" alt="Download for Android" width="16%" />](#)
[<img src="documentation/images/badge_ios_coming_soon.png" alt="Download for iOS" width="16%" />](#)
[<img src="documentation/images/badge_windows_coming_soon.png" alt="Download for Windows" width="16%" />](#)
[<img src="documentation/images/badge_macos_coming_soon.png" alt="Download for macOS" width="16%" />](#)
[<img src="documentation/images/badge_linux_coming_soon.png" alt="Download for Linux" width="16%" />](#)
[<img src="documentation/images/badge_web.png" alt="Download for Web" width="16%" />](https://pandulapeter.github.io/kubriko/)

## 📚 Learning
The [Documentation](https://github.com/pandulapeter/kubriko/tree/main/documentation) page contains a high-level introduction to Kubriko's main systems, as well as links to further resources.

Tutorials in the form of videos are coming soon, once the public API gets finalized, and a few more demo games are ready.

[<img src="documentation/images/badge_documentation.png" alt="Documentation" width="20%" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md)
[<img src="documentation/images/badge_youtube_coming_soon.png" alt="YouTube" width="20%" />](#)

Another way to learn about the engine is to explore this repository, and understand how the various examples are implemented.

## 🏗️ Project structure
This repository also serves as the documentation of the engine's feature set.

Each top-level folder / module has its own Readme file that contains high-level information about the relevant piece of functionality.
Classes and functions exposed in the public API are documented using KDoc.

The following folders are worth mentioning:
- [app](https://github.com/pandulapeter/kubriko/tree/main/app) - The menu system for the showcase app. It ties together the modules located in the `examples` folder.
- [documentation](https://github.com/pandulapeter/kubriko/tree/main/documentation) - Contains markdown files with an overview of the Kubriko API.
- [engine](https://github.com/pandulapeter/kubriko/tree/main/engine) - Defines the core components of Kubriko.
- [examples](https://github.com/pandulapeter/kubriko/tree/main/examples) - Contains the implementation of the games and demos of the showcase app.
- [plugins](https://github.com/pandulapeter/kubriko/tree/main/plugins) - These modules can be used to extend the functionality of Kubriko.
- [tools](https://github.com/pandulapeter/kubriko/tree/main/tools) - Extensions that can be useful during development such as the Scene Editor.

## 📜 Artifacts
The following is the complete list of all Kubriko dependencies hosted on MavenCentral.

- Engine:
    - `io.github.pandulapeter.kubriko:engine`
- Plugins: 
    - `io.github.pandulapeter.kubriko:plugins:audio-player`
    - `io.github.pandulapeter.kubriko:plugins:collision`
    - `io.github.pandulapeter.kubriko:plugins:keyboard-input`
    - `io.github.pandulapeter.kubriko:plugins:particles`
    - `io.github.pandulapeter.kubriko:plugins:persistence`
    - `io.github.pandulapeter.kubriko:plugins:physics`
    - `io.github.pandulapeter.kubriko:plugins:pointer-input`
    - `io.github.pandulapeter.kubriko:plugins:serialization`
    - `io.github.pandulapeter.kubriko:plugins:shaders`
    - `io.github.pandulapeter.kubriko:plugins:sprites`
- Tools: 
    - `io.github.pandulapeter.kubriko:tools:debug-menu`
    - `io.github.pandulapeter.kubriko:tools:scene-editor`

The latest version is:

[![](https://maven-badges.herokuapp.com/maven-central/io.github.pandulapeter.kubriko/engine/badge.svg?style=flat)](https://repo1.maven.org/maven2/io/github/pandulapeter/kubriko/)

Please note that the above artifacts are not yet public.