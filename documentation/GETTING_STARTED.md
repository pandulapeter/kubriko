# Getting started
This page guides you through creating an empty project that contains the basic setup for any Kubriko game.

## 1 - Creating a Compose Multiplatform project
The first step is using the [Kotlin Multiplatform Wizard](https://kmp.jetbrains.com/) to generate the project files.

<img src="images/screenshot_kotlin_multiplatform_wizard.png" width="200px" />

Enter a **Project Name** and a **Project ID**, and select all the platforms that you want your game to support. Please note that:
- iOS builds can only be created on a macOS device.
- If you need iOS support, make sure you have selected the radio button for "Share UI (with Compose Multiplatform UI framework)".
- The "Server" target is irrelevant for Kubriko.

After all the options are properly configured, press the **Download** button.
Extract the .zip file you just downloaded and open it in your IDE of choice ([Android Studio](https://developer.android.com/studio) or [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) are recommended). 

Check out the [official guide](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-create-first-app.html#create-the-project-with-a-wizard) to better understand the files of the project.

- To test the Android app, follow the instructions from [here](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-create-first-app.html#run-your-application-on-android).
- To test the iOS app, follow the instructions from [here](https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-create-first-app.html#run-your-application-on-ios) (again, you need a computer running macOS to do this).
- To test the Desktop (jvm) app, use the `./gradlew run` command.
- To test the Web (wasmJs) app, use the `./gradlew wasmJsBrowserRun` command.

It might be a good idea to create separate [Run Configurations](https://www.jetbrains.com/guide/java/tutorials/hello-world/creating-a-run-configuration/) for each platform to simplify switching between the different targets in the future:

<img src="images/screenshot_run_configurations.png" width="200px"  />

Make sure you have tested the build process for all of the platforms that you want to support before moving on to the next step.

## 2 - Adding the Kubriko engine dependency
Open the `libs.versions.toml` file from the `gradle/wrapper` folder.
This file is the version catalog for all the third party dependencies of your project.

It already contains a number of dependencies (some of which are unused and can safely be deleted). We need to add Kubriko to the list.
To do that, first define the version reference by adding a new line to the `[versions]` section:

```toml
kubriko = "0.0.1"
```

Use the latest release version of Kubriko that can be found [here](https://github.com/pandulapeter/kubriko/releases).
Regularly update this version number to get access to the latest features and bug fixes.

Next, define the library reference by adding a new line to the `[libraries]` section:

```toml
kubriko-engine = { group = "io.github.pandulapeter.kubriko", name = "engine", version.ref = "kubriko" }
```

After this, we need to reference the library in the game module's `build.gradle.kts` file (by default the module is named `composeApp`):

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

Make sure to sync the project after changing the build script.

## 3 - Integrating KubrikoViewport
Following the previous step you should be able to access the classes and functions provided by Kubriko (the engine itself only, as we didn't yet add any plugin dependencies).
Furthermore, you should also be able to see the sources, and more importantly, the KDoc comments of Kubriko's public API.

Let's verify this by replacing the app's main Composable (by default located in the `commonMain` source set's `App.kt` file) with [KubrikoViewport](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoViewport.kt).
While KubrikoViewport is responsible for the game's UI, we also need to provide a [Kubriko](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt) instance to it that handles the game state.
Here's how the `App()` Composable function should look like after all the changes:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport

@Composable
fun App() {
    val kubriko = remember { Kubriko.newInstance() }
    KubrikoViewport(
        kubriko = kubriko,
    )
}
```

Verify that you're able to see the sources for these newly added components.
More importantly, try running the app to make sure that there are no compilation issues.

If everything went well, you should see an empty screen, as we didn't add anything to the game yet.

## 4 - Adding Actors
TODO

## 5 - Adding Managers
TODO

## 6 - Adding plugins
TODO

## 7 - Next steps
This should cover the basics of what you need to get started developing your first Kubriko game!
Feel free to play around with the various components, and have fun exploring the possibilities.
Don't forget to read the KDoc comments of the public API components to get more information about how they are intended to be used.
If you ever get stuck, check out the resources below:

[<img src="images/badge_documentation.png" alt="Documentation" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/README.md)
[<img src="images/badge_tutorial_videos_coming_soon.png" alt="Tutorial videos" height="32px" />](#)
[<img src="images/badge_community_coming_soon.png" alt="Join the community" height="32px" />](#)