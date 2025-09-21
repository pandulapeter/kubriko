# Getting started

These pages guide you through creating your first Kubriko game from scratch.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_01.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_03.md)

## 2 - Adding the Kubriko engine dependency

Open the `libs.versions.toml` file from your project's `gradle` folder.
This file is the version catalog for all the third party dependencies of your project.

It already contains a number of dependencies (some of which are unused and can safely be deleted). We need to add Kubriko to the list.
To do that, first define the version reference by adding a new line to the `[versions]` section:

```toml
kubriko = "0.0.5"
```

Use the latest release version of Kubriko that can be found [here](https://github.com/pandulapeter/kubriko/releases).
Regularly update this version number to get access to the latest features and bug fixes.

While we're here, we should make sure that the following three versions in your project's `libs.versions.toml` are at least equal to the versions defined for
Kubriko. Cross reference these dependency versions with their counterparts from [here](https://github.com/pandulapeter/kubriko/blob/main/gradle/libs.versions.toml) and update where necessary.

- `android-compileSdk` (only if your project supports Android)
- `android-minSdk` (only if your project supports Android)
- `compose-multiplatform`

> [!IMPORTANT]  
> Trying to use versions incompatible with Kubriko might prevent certain targets from compiling.

Next, define the library reference by adding a new line to the `[libraries]` section:

```toml
kubriko-engine = { group = "io.github.pandulapeter.kubriko", name = "engine", version.ref = "kubriko" }
```

After this we need to reference the library in the game module's `build.gradle.kts` file (by default the module is named `composeApp`):

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

If your project supports Android, we should also update a couple of other lines in this file to ensure compatibility with some of Kubriko's features:

```kotlin
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    //...
}
//...
android {
    //..
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

Make sure to sync the project after changing the build script.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_01.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_03.md)