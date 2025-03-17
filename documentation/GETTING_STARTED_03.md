# Getting started

These pages guide you through creating an empty project that contains the basic setup for any Kubriko game.

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_02.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_04.md)

## 3 - Integrating KubrikoViewport

Following the previous step you should be able to access the classes and functions provided by Kubriko (the engine itself only, as we didn't yet add any plugin
dependencies).
Furthermore, you should also be able to see the sources, and more importantly, the KDoc comments of Kubriko's public API.

Let's verify this by replacing the app's main Composable (by default located in the `commonMain` source set's `App.kt` file)
with [KubrikoViewport](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/KubrikoViewport.kt).
While `KubrikoViewport` is responsible for the game's UI, we also need to provide
a [Kubriko](https://github.com/pandulapeter/kubriko/blob/main/engine/src/commonMain/kotlin/com/pandulapeter/kubriko/Kubriko.kt) instance to it that handles the
game state.
Here's how the `App()` Composable function should look like after all the changes:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.KubrikoViewport
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
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

[<img src="images/badge_previous.png" alt="Previous page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_02.md)
[<img src="images/badge_next.png" alt="Next page" height="32px" />](https://github.com/pandulapeter/kubriko/blob/main/documentation/GETTING_STARTED_04.md)