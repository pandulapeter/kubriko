# Kubriko
*A 2D game engine based on Compose Multiplatform.*

**Very early development phase.**

<img src="documentation/logo.png" width="20%" />

This repository contains the source code of the engine and its plugins / tooling as well as the Kubriko Showcase application that demonstrates the engine's capabilities.

## Scope
Kubriko aims to offer a lightweight, scalable, and easy-to-use solution for creating simple 2D games for **Android**, **Desktop** (Windows, Linux, MacOS), **iOS** and **Web**.
It achieves this goal by exposing a [Composable](https://www.jetbrains.com/compose-multiplatform/) that can be embedded into [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) projects.

The engine is highly modular and its core functionality can be extended in a granular fashion using the different plugins.
It covers most needs of simple games from viewport scaling and object management, through input handling and audio playback, all the way to shaders and physics simulation.
The tech stack is also easy to extend by creating custom plugins, and the low-level integration makes it possible to communicate with platform API-s in a straightforward way.

## Showcase app
Clone this repository and run the `app` module for the supported targets (Android, Desktop, iOS, Web) to check out what the engine is capable of.

Once the project reaches a more mature stage of development, I plan to publish the showcase app in relevant stores for all platforms.
For now only the web version is available [here](http://pandulapeter.github.io/kubriko).

## Latest release
The project is not yet published as a library, as that will only happen after the first release, in early 2025.
All releases will appear under the [Releases](https://github.com/pandulapeter/kubriko/releases) page.

To track the current progress of the development as well as the long-term goals, check out [this board](https://github.com/users/pandulapeter/projects/11).

## Project structure
This repository also serves as the documentation of the engine's feature set.
Each top-level folder / module has its own Readme file that contains high-level information about the relevant piece of functionality.
Classes and functions exposed in the public API are documented using KDoc.

The following folders are worth mentioning:
- [app](https://github.com/pandulapeter/kubriko/tree/main/app) - The menu system for the showcase application. It ties together the various modules located within the `examples` folder.
- [engine](https://github.com/pandulapeter/kubriko/tree/main/engine) - Defines the core components of Kubriko.
- [examples](https://github.com/pandulapeter/kubriko/tree/main/examples) - Contains the implementation of the various games and demos of the showcase app.
- [plugins](https://github.com/pandulapeter/kubriko/tree/main/plugins) - These modules can be used to extend the functionality of Kubriko.
- [tools](https://github.com/pandulapeter/kubriko/tree/main/tools) - Small extensions that can be useful during development.

## Learning
For now the best resource for understanding Kubriko is exploring this repository, and checking out how the various examples are implemented.

A high-level overview of Kubriko's main systems can be found in [this document](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md).

Tutorials in the form of videos are coming soon, once the public API gets finalized and a few more demo games are ready.