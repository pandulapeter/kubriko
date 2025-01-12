# Examples
This folder contains the implementation for the individual demos and games found in the Kubriko Showcase app.

## Implementation
Each module exposes a single Composable that is embedded into the menu system (located in the [app](https://github.com/pandulapeter/kubriko/tree/main/app)
module), as well as a StateHolder instance that can be used to persist game state across configuration changes.

## Modules
- [demo-content-shaders](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-content-shaders) - Playground for testing various shader effects
- [demo-input](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-input) - Playground for testing keyboard and pointer input
- [demo-performance](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-performance) - Stress test using a large number of dynamic actors
- [demo-physics](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-physics) - Rigid body collisions, chains, explosions
- [demo-shader-animations](https://github.com/pandulapeter/kubriko/tree/main/examples/demo-shader-animations) - Examples for dynamic SGSL shader overlays
- [game-space-squadron](https://github.com/pandulapeter/kubriko/tree/main/examples/game-space-squadron) - Use your space ship to defend from alien invaders
- [game-wallbreaker](https://github.com/pandulapeter/kubriko/tree/main/examples/game-wallbreaker) - Playground for testing various shader effects
- [shared](https://github.com/pandulapeter/kubriko/tree/main/examples/shared) - Contains code shared by the modules above