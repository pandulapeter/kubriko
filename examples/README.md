# Examples
This folder contains the implementation modules of the individual games, demos, and tests found in the Kubriko Showcase app.

## Implementation
Each module exposes a single Composable that is embedded into the menu system (located in the [app](https://github.com/pandulapeter/kubriko/tree/main/app)
module), as well as a [StateHolder](https://github.com/pandulapeter/kubriko/blob/main/examples/shared/src/commonMain/kotlin/com/pandulapeter/kubriko/shared/StateHolder.kt) instance that can be used to persist game state across configuration changes.

## Games
- [game-wallbreaker](https://github.com/pandulapeter/kubriko/tree/main/examples/game-wallbreaker) - Playground for testing various shader effects
- [game-space-squadron](https://github.com/pandulapeter/kubriko/tree/main/examples/game-space-squadron) - Use your space ship to defend from alien invaders
- [game-annoyed-penguins](https://github.com/pandulapeter/kubriko/tree/main/examples/game-annoyed-penguins) - Slingshot your penguins to a chaotic victory

## Demos
- [demo-physics](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-physics) - Rigid body collisions, chains, explosions
- [demo-particles](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-particles) - Demo for editing and testing particle effects
- [demo-content-shaders](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-content-shaders) - Playground for testing various shader effects
- [demo-shader-animations](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-shader-animations) - Examples for dynamic SGSL shader overlays

## Tests
- [test-audio](https://github.com/pandulapeter/kubriko/tree/main/examples/test-auido) - For testing the playback of music and sound effects
- [test-input](https://github.com/pandulapeter/kubriko/tree/main/examples/test-input) - Playground for testing keyboard and pointer input
- [test-performance](https://github.com/pandulapeter/kubriko/tree/main/examples/test-performance) - Stress test using a large number of dynamic actors

## Other
- [shared](https://github.com/pandulapeter/kubriko/tree/main/examples/shared) - Contains code shared by the modules above