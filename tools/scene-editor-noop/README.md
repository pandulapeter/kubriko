# Scene Editor Tool - No-op

The `scene-editor-noop` module provides a no-op implementation of the `scene-editor-api` contract. It is designed to be used in production builds or on platforms where the Scene Editor is not supported, ensuring that your code can still compile and run without including the heavy editor logic.

## Why use this?

The Scene Editor is a powerful tool for arranging and customizing `Editable` actors on Desktop, but it is typically not needed in a released application. By using this no-op version, you can:
- **Reduce binary size**: Exclude editor-specific dependencies and code.
- **Simplify builds**: Avoid platform-specific issues on non-desktop targets.
- **Maintain code consistency**: Use the same `SceneEditor` object throughout your codebase without needing conditional compilation or manual flags in every usage site.

## Usage

Replace the `scene-editor` dependency with `scene-editor-noop` in your production build flavor or platform-specific source sets:

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.github.pandulapeter.kubriko:tool-scene-editor-noop")
}
```

The API remains identical to the real implementation, but calling its methods will have no effect.

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:tool-scene-editor-noop`
