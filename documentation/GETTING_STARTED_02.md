# Getting started

These pages guide you through building your first Kubriko game, one small step at a time.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_01.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_03.md)

## 2 - Adding the Kubriko dependency

Kubriko is published to [Maven Central](https://repo1.maven.org/maven2/io/github/pandulapeter/kubriko/), so adding it is just a matter of
declaring a dependency.

### Declaring the version

Open `gradle/libs.versions.toml`. This file is the version catalog: a single place that lists the versions of every third party library your project uses.
The wizard already filled it with a few entries (some of which you may not need and can delete later).

Add a new line to the `[versions]` section:

```toml
kubriko = "0.7.1"
```

Use the newest release, which you can find on the [releases page](https://github.com/pandulapeter/kubriko/releases). It is worth updating this number every
now and then, to pick up new features and bug fixes.

### Matching the shared versions

Kubriko is built on top of Compose Multiplatform, so a few versions in your project have to be at least as new as the ones Kubriko itself uses.
Compare these entries with [Kubriko's own version catalog](https://github.com/pandulapeter/kubriko/blob/main/gradle/libs.versions.toml) and bump them if yours are older:

- `compose-multiplatform`
- `kotlin`
- `android-compileSdk` (only if your project supports Android)
- `android-minSdk` (only if your project supports Android)

> [!IMPORTANT]
> Older versions can cause confusing build failures on some targets. If a platform suddenly refuses to compile after adding Kubriko, this is the first thing to check.
> A too-low `android-compileSdk`, for example, fails with a wall of messages like *"requires libraries and applications that depend on it to compile against
> version 37 or later of the Android APIs"*.

### Adding the library

Add the engine to the `[libraries]` section of the same file:

```toml
kubriko-engine = { group = "io.github.pandulapeter.kubriko", name = "engine", version.ref = "kubriko" }
```

Then reference it from the `shared` module's `build.gradle.kts`. That's the module your game code lives in, so that's the only place that needs it:

```kotlin
kotlin {
    //...
    sourceSets {
        //...
        commonMain.dependencies {
            //...
            implementation(libs.kubriko.engine)
        }
    }
}
```

This single dependency gives you the engine: the game loop, the viewport, the Actor system, and four built-in Managers. Everything else (input, collisions,
audio, physics, and so on) lives in optional plugins that you add the same way, whenever you need them. Their artifacts follow one pattern,
`io.github.pandulapeter.kubriko:plugin-<name>`, and the complete list is in the
[main Readme](https://github.com/pandulapeter/kubriko/tree/main?tab=readme-ov-file#-artifacts). We'll add the first plugin on page 7.

### Java version

Kubriko is compiled with **Java 21**, so every module that touches it has to be built with Java 21 as well. In the `shared` module:

```kotlin
kotlin {
    jvmToolchain(21)
    //...
}
```

Do the same in `desktopApp`, and set the Android app module to match:

```kotlin
android {
    //...
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}
```

The wizard usually picks an older Java version, so this is a change you will almost certainly need to make. If you skip it, the build fails with:

```
Cannot inline bytecode built with JVM target 21 into bytecode that is being built with JVM target 17.
```

which is exactly this problem.

### One note about the Web target

If your project supports Web, delete the `js()` block from the `kotlin { ... }` block. Kubriko only supports the newer `wasmJs()` target.

Sync the project after all these changes. If Gradle finishes without complaints, the engine is on your classpath and you are ready to use it.

> [!NOTE]
> The Web target of Compose Multiplatform is still in alpha, and games built with Kubriko inherit its limitations, especially in iOS browsers.
> The [known issues page](https://github.com/pandulapeter/kubriko/blob/main/documentation/KNOWN_ISSUES.md) lists what to expect on each platform.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_01.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_03.md)
