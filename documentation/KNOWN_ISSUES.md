# Known issues
This page lists all the long-term, high-level issues that I'm aware of regarding Kubriko, and gives additional context on them. For smaller and more specific bugs see the [Issues page](https://github.com/pandulapeter/kubriko/issues).

## Web limitations
The [Kubriko Showcase web app](https://pandulapeter.github.io/kubriko/) has been used for testing.

### General performance and bugs
Despite Compose WASM support being in [Alpha](https://kotlinlang.org/docs/wasm-overview.html), in general, I was pleasantly surprised by how stable the overall experience feels, especially on macOS and Windows.
- On desktop Chrome and Firefox the performance is almost comparable to JVM builds. I'm not aware of any issues on these browsers.
- Similarly, Chrome on Android works just fine, but the native Android app is more performant of course.
- Chrome on Android: there is a corner case bug of music playback sometimes getting stuck on configuration changes. This might be a WASM memory leak, I wasn't able to fix it on the Javascript/Kotlin level, but it happens rarely.
- Performance on macOS Safari is not as great as on Chrome and Firefox, and I sometimes encountered random freezes.
- iPad and iPhone browsers have significant issues. See the next section.

### Issues on iOS
[WASM garbage collection](https://webassembly.org/features/) support has only been recently introduced to WebKit and it shows. Kubriko web apps running on iPhones and iPads (both on Safari and Chrome) present some considerable problems. I don't have any control over these issues, but hopefully, with time, WASM support will get gradually optimized by Apple.
- Lower general performance compared to Android web browsers.
- Audio playback issues (sometimes the music or sound effects simply just don't start).
- Frequent app freezes that can last 10-20 seconds. Sudden jumps in the memory required by the app seems to trigger this problems, like loading screens.

### Missing multi-touch support
Multi-touch on web doesn't work on the Compose level. [Here's a relevant ticket](https://youtrack.jetbrains.com/issue/CMP-6957/Web.-detectTransformGestures-doesnt-catch-zoom-and-rotation-gestures).

## Desktop limitations
### Missing multi-touch support
Multi-touch on the JVM doesn't work on the Compose level. [Here's a relevant ticket](https://youtrack.jetbrains.com/issue/CMP-2209/Desktop.-Unable-to-zoom-using-detectTransformGestures-in-Modifier.pointerInput-using-touch-screens). I don't consider this such an important issue as on the web, as Windows / Linux systems with touch screens generally have other input methods as well.