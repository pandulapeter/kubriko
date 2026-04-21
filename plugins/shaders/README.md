# Shaders Plugin

The `shaders` plugin enables the use of custom SKSL (Skia Shading Language) shaders in Kubriko. It allows for high-performance visual effects applied to individual layers or the entire scene.

## Features

- **SKSL Support**: Write custom shaders using the Skia Shading Language.
- **Layer-based Application**: Apply shaders to specific actor layers or as global post-processing effects.
- **Dynamic Uniforms**: Update shader parameters (uniforms) in real-time from your Kotlin code.
- **Built-in Effects**: Includes a collection of ready-to-use shaders like Blur, Ripple, Chromatic Aberration, and more.
- **Content Access**: `ContentShader` allows effects to read the existing scene content as an input texture.

## Usage

### 1. Register the Manager

Add the `ShaderManager` to your `Kubriko` instance:

```kotlin
val kubriko = Kubriko.newInstance(
    ShaderManager.newInstance(),
    // ... other managers
)
```

### 2. Implement a Shader

Create an actor or an object that implements the `Shader` interface:

```kotlin
class MyEffect : Actor(), ContentShader<MyEffect.State> {

    override val shaderCode = """
        uniform shader content;
        uniform vec2 resolution;
        uniform float threshold;

        vec4 main(vec2 fragCoord) {
            vec4 color = content.eval(fragCoord);
            // ... effect logic
            return color;
        }
    """.trimIndent()

    override val shaderState = State()

    inner class State : Shader.State {
        var threshold = 0.5f

        override fun ShaderUniformProvider.applyUniforms() {
            setFloatUniform("threshold", threshold)
        }
    }

    override val shaderCache = Shader.Cache()
}
```

### 3. Use Built-in Shaders

The plugin comes with several pre-implemented effects:

```kotlin
val blurEffect = BlurShader(radius = 10f)
val vignetteEffect = VignetteShader(intensity = 0.8f)

// Shaders are Actors, so you can add them to the engine
actorManager.add(blurEffect)
```

## Included Shaders

The following shaders are available out-of-the-box in the `com.pandulapeter.kubriko.shaders.collection` package:

- **BlurShader**: Gaussian-like blur effect.
- **ChromaticAberrationShader**: Color-splitting effect often used for glitches or lenses.
- **RippleShader**: Animated water ripple effect.
- **VignetteShader**: Darkens the corners of the screen.
- **ComicShader**: A stylized "dot" or "halftone" print effect.
- **SmoothPixelationShader**: High-quality pixelation effect.

## Credit

The built-in shader collection is inspired by and adapted from the following amazing resources:
- [Photo-FX by manuel-martos](https://github.com/manuel-martos/Photo-FX)
- [agsl-fun by jurajkusnier](https://github.com/jurajkusnier/agsl-fun)
- [shady by drinkthestars](https://github.com/drinkthestars/shady)

## Public Artifact

The artifact for this module is:
`io.github.pandulapeter.kubriko:plugin-shaders`
