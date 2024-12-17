package com.pandulapeter.kubriko.shader.collection

import androidx.compose.ui.geometry.Offset
import com.pandulapeter.kubriko.shader.ContentShader
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

/**
 * Credit: Zach Klippenstein
 * https://gist.github.com/zach-klippenstein/f92f7d59c1bdabcda67a3aed51a3fe3a
 */
// TODO: Extract parameters
class ComicShader(
    override var state: State = State(),
    override val layerIndex: Int? = null,
) : ContentShader<ComicShader.State> {
    override val cache = Shader.Cache()
    override val code = CODE

    data class State(
        val focusPoint: Offset = Offset.Zero,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(FOCUS_POINT, focusPoint.x, focusPoint.y)
        }
    }

    companion object {
        private const val FOCUS_POINT = "focusPoint"
        private const val CODE = """
uniform float2 ${Shader.RESOLUTION};
uniform shader ${ContentShader.CONTENT};
uniform float2 $FOCUS_POINT;

float tileSize = 12;
float dotMinDiameter = 9;
vec2 maxRedShift = vec2(-2, 3);
vec2 maxGreenShift = vec2(2.5, 0);
vec2 maxBlueShift = vec2(1, -2);
half3 baseColor = half3(0, 0, 0);

half4 main(vec2 fragcoord) {
  float maxDimension = max(${Shader.RESOLUTION}.x, ${Shader.RESOLUTION}.y);
  float focalSizeCutoff = maxDimension / 2;
  float focalChromaticCutoff = maxDimension;
  vec2 focalPoint = $FOCUS_POINT.xy;
  
  vec2 tileIndex = vec2(
    floor(fragcoord.x / tileSize),
    floor(fragcoord.y / tileSize)
  );
  vec2 tileOrigin = tileIndex * tileSize;
  vec2 tileCoord = fragcoord - tileOrigin;
  vec2 tileCenter = vec2(tileSize / 2, tileSize / 2);

  // Take a sample of the source's color from the center of the tile.
  vec2 sampleCoord = tileOrigin + tileCenter;
  half4 sampleColor = ${ContentShader.CONTENT}.eval(sampleCoord);

  // Modulate the size of the dot by the mouse position.
  float distanceFromFocus = distance(fragcoord, focalPoint.xy);
  float dotRadius = max(
    dotMinDiameter / 2,
    mix(tileSize / 2, 0, distanceFromFocus / focalSizeCutoff)
  );

  // Chromatic aberration
  float sampleRed = sampleColor.r;
  float sampleGreen = sampleColor.g;
  float sampleBlue = sampleColor.b;
  // …also modulated by mouse position.
  vec2 redShift = maxRedShift * distanceFromFocus / focalChromaticCutoff;
  vec2 greenShift = maxGreenShift * distanceFromFocus / focalChromaticCutoff;
  vec2 blueShift = maxBlueShift * distanceFromFocus / focalChromaticCutoff;
  float distRed = distance(tileCoord, tileCenter + redShift);
  float distGreen = distance(tileCoord, tileCenter + greenShift);
  float distBlue = distance(tileCoord, tileCenter + blueShift);
  return half4(
    distRed <= dotRadius ? sampleRed : baseColor.r,
    distGreen <= dotRadius ? sampleGreen : baseColor.g,
    distBlue <= dotRadius ? sampleBlue : baseColor.b,
    sampleColor.a
  );
}
"""
    }
}