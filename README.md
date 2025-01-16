# Kubriko
*A 2D game engine based on Compose Multiplatform*

<img src="documentation/images/logo.png" width="128px"  alt="Kubriko logo"/>

This repository contains the source code of the engine and its plugins / tooling, as well as the Kubriko Showcase application that demonstrates the engine's capabilities.

## 🎮 Overview
Kubriko aims to offer a lightweight, scalable, and easy-to-use solution for creating simple 2D games for **Android**, **Desktop** (Windows, Linux, macOS), **iOS** and **Web**.
It achieves this goal by exposing a [Composable](https://www.jetbrains.com/compose-multiplatform/) function that can easily be embedded into [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) projects.

The engine is highly modular, because its core functionality can be extended in a granular fashion using different plugins.
These plugins cover most needs of simple games; from viewport scaling and object management, through persistence, input handling and audio playback, all the way to shaders and physics simulation.
Kubriko also offers a number of tools, such as a Scene Editor, that can be useful during development.

The tech stack is easy to extend by creating custom plugins, and the low-level integration makes it possible to communicate with platform API-s in a straightforward way.

## 🎨 Showcase app
Clone this repository and run the `app` module for the supported targets (Android, Desktop, iOS, Web) to check out what the engine is capable of.

You can find the pre-built applications following these links:

[<img src="documentation/images/badge_android_coming_soon.png" alt="Download for Android" height="32px" />](#)
[<img src="documentation/images/badge_ios_coming_soon.png" alt="Download for iOS" height="32px" />](#)
[<img src="documentation/images/badge_windows_coming_soon.png" alt="Download for Windows" height="32px" />](#)
[<img src="documentation/images/badge_macos_coming_soon.png" alt="Download for macOS" height="32px" />](#)
[<img src="documentation/images/badge_linux_coming_soon.png" alt="Download for Linux" height="32px" />](#)
[<img src="documentation/images/badge_web.png" alt="Download for Web" height="32px" />](https://pandulapeter.github.io/kubriko/)

Please note that some of these might not be available / up to date, so the best way is to build the project yourself.

## 📚 Learning
The following page gives a short overview of the first steps of creating a Kubriko game from scratch:

[<img src="documentation/images/badge_getting_started.png" alt="Getting started" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED.md)

After setting up your first project, check out the documentation page, which contains a high-level introduction to Kubriko's main systems, as well as links to further resources:

[<img src="documentation/images/badge_documentation.png" alt="Documentation" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md)

Video tutorials that offer a more practical perspective are also available.
These are step by step guides to creating simple games, each introducing more and more advanced concepts from the engine.

[<img src="documentation/images/badge_tutorial_videos_coming_soon.png" alt="Tutorial videos" height="32px" />](#)

Another way to learn about Kubriko is to explore this repository, and understand how the various examples are implemented.
And the best way, of course, is having fun while creating your very own games!

## 🏗️ Project structure
Besides hosting the engine's source code, this repository also serves as the documentation for its feature set.

Each top-level folder / module has its own Readme file that contains general information about the relevant piece of functionality:
- [app](https://github.com/pandulapeter/kubriko/tree/main/app) - The menu system for the showcase app. It ties together the modules located in the `examples` folder.
- [documentation](https://github.com/pandulapeter/kubriko/tree/main/documentation) - Contains markdown files with an overview of the Kubriko API.
- [engine](https://github.com/pandulapeter/kubriko/tree/main/engine) - Defines the core components of Kubriko.
- [examples](https://github.com/pandulapeter/kubriko/tree/main/examples) - Contains the implementation of the games and demos of the showcase app.
- [plugins](https://github.com/pandulapeter/kubriko/tree/main/plugins) - These modules can be used to extend the functionality of Kubriko.
- [tools](https://github.com/pandulapeter/kubriko/tree/main/tools) - Extensions that can be useful during development such as the Scene Editor. 

For more details, read the KDoc comments for the classes and functions that are exposed in the public API.

## 📜 Artifacts
Expand the sections below to see the complete list of all Kubriko dependencies hosted on [MavenCentral](https://repo1.maven.org/maven2/io/github/pandulapeter/kubriko/).

<details>
<summary>Engine</summary>


- `io.github.pandulapeter.kubriko:engine`

</details>
<details>
<summary>Plugins</summary>


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

</details>
<details>
<summary>Tools</summary>


- `io.github.pandulapeter.kubriko:tools:debug-menu`
- `io.github.pandulapeter.kubriko:tools:scene-editor`

</details>

The latest version is:

[![](https://maven-badges.herokuapp.com/maven-central/io.github.pandulapeter.kubriko/engine/badge.svg?style=flat)](https://repo1.maven.org/maven2/io/github/pandulapeter/kubriko/)

## 🤝 Contributing
All help in making Kubriko a better engine is welcome!
- For improvement ideas or bug reports simply create a [new issue](https://github.com/pandulapeter/kubriko/issues/new). Make sure to provide as many details as possible and use the correct labels.
- For code contributions use the [project board](https://github.com/users/pandulapeter/projects/11) to make sure that the issue you want to fix is not already being worked on. Please leave a comment as well so that the issue can be assigned to you! After that fork the project, commit your changes, push them to the repository, and create a [pull request](https://github.com/pandulapeter/kubriko/compare).

## 🫶️ Support
Displaying the [Kubriko logo](https://pandulapeter.github.io/kubriko/documentation/images/logo.png) in games created with the engine, or just mentioning Kubriko somewhere in the credits is highly appreciated!
Also, if you create anything with Kubriko that you're proud of, don't hesitate to reach out to me via [email](mailto:pandulapeter@gmail.com)!

Kubriko is my pet project, that I'm developing in my free time. If you found it useful, I'm happy to accept donations on the following pages:

[<img src="documentation/images/badge_coffee.png" alt="Buy me a coffee" height="32px" />](https://buymeacoffee.com/pandulapeter)
[<img src="documentation/images/badge_sponsor.png" alt="Sponsor on GitHub" height="32px" />](https://github.com/sponsors/pandulapeter/)

Thanks in advance!

## ⚖️ License
This library is published under the Apache License, Version 2.0, which can be found [here](https://github.com/pandulapeter/kubriko/blob/main/LICENSE).
You can use it in your own projects as it is, for free, without giving credit.
However, if you plan to create derivative works (competing engines / forks), they must be open source as well, and link back to this repository.