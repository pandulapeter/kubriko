/*
 * This file is part of Kubriko.
 * Copyright (c) Pandula Péter 2025.
 * https://github.com/pandulapeter/kubriko
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */
package com.pandulapeter.kubriko.gameBlockysJourney.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shaders.Shader
import com.pandulapeter.kubriko.shaders.extensions.ShaderUniformProvider

/**
 * Credit: alro
 * https://www.shadertoy.com/view/ltSczW
 */
class BackgroundShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<BackgroundShader.State>, Dynamic {
    override var shaderState = initialState
        private set
    override val shaderCache = Shader.Cache()
    override val shaderCode = CODE
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMilliseconds: Int) {
        //shaderState = shaderState.copy(time = (metadataManager.activeRuntimeInMilliseconds.value % 100000L) / 1000f)
    }

    data class State(
        val time: Float = 0f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val CODE = """
uniform float2 ${Shader.RESOLUTION};
uniform float $TIME;
 
const vec2 s = vec2(1, 1.7320508);

float hash21(vec2 p){ return fract(sin(dot(p, vec2(141.13, 289.97)))*43758.5453); }

mat2 r2(in float a){ float c = cos(a), s = sin(a); return mat2(c, -s, s, c); }

float isoDiamond(in vec2 p){
    
    p = abs(p);
    
    // Below is equivalent to:
    //return p.x*.5 + p.y*.866025; 
    
    return dot(p, s*.5); 

}


vec4 getHex(vec2 p){
    
 
    vec4 hC = floor(vec4(p, p - vec2(.5, 1))/s.xyxy) + .5;
    
    vec4 h = vec4(p - hC.xy*s, p - (hC.zw + .5)*s);
    
   
    return dot(h.xy, h.xy)<dot(h.zw, h.zw) ? vec4(h.xy, hC.xy) : vec4(h.zw, hC.zw + vec2(.5, 1));
    
}




half4 main(float2 fragCoord) {
    float res = clamp(${Shader.RESOLUTION}.y, 300., 600.);
	vec2 u = (fragCoord - ${Shader.RESOLUTION}.xy*.5)/res;
    

    // Scaling, translating, then converting it to a hexagonal grid cell coordinate and
    // a unique coordinate ID. The resultant vector contains everything you need to produce a
    // pretty pattern, so what you do from here is up to you.
    vec4 h = getHex(u*6. + s.yx*$TIME/4.);
   
    
    // Storing the relative hexagonal position coordinates in "p," just to save some writing. :)
    vec2 p = h.xy;
    
    // Relative squared distance from the center.
    float d = dot(p, p)*1.5;
    
    
    // Using the idetifying coordinate - stored in "h.zw," to produce a unique random number
    // for the hexagonal grid cell.    
    float rnd = hash21(h.zw);
    rnd = sin(rnd*6.283 + $TIME)*.5 + .5;
    // It's possible to control the randomness to form some kind of repeat pattern.
    //rnd = mod(h.z + h.w, 4.);
    


    
    // Initiate the background to white.
    vec3 col = vec3(1);    


    // Using the random number associated with the hexagonal grid cell to provide some color
    // and some smooth blinking. The coloring was made up, but it's worth looking at the 
    // "blink" line which smoothly blinks the cell color on and off.
    //
    float blink = smoothstep(0., .125, rnd - .75); // Smooth blinking transition.
    float blend = dot(sin(u*3.14159*2. - cos(u.yx*3.14159*2.)*3.14159), vec2(.25)) + .5; // Screen blend.
    col = max(col - mix(vec3(0, .4, .6), vec3(0, .3, .7), blend)*blink, 0.); // Blended, blinking orange.
    col = mix(col, col.xzy, dot(sin(u*5. - cos(u*3. + $TIME)), vec2(.3/2.)) + .3); // Orange and pink mix.
    
    // Uncomment this if you feel that greener shades are not being fairly represented. :)
    col = mix(col, col.yxz, dot(cos(u*6. + sin(u*3. - $TIME)), vec2(.35/2.)) + .35); // Add some green.


    
    // Tile flipping - If the unique random ID is above a certain threshold, flip the Y coordinate, which
    // is effectively the same as rotating by 180 degrees. Due to the isometric lines, this gives the
    // illusion that the cube has been taken away. To build upon the illusion, the shading (based on 
    // distance to the cell center) is inverted also, which gives a fake kind of ambient occlusion effect.
    //
    if(rnd>.5) {
        
        p.xy = -p.xy;
        col *= max(1.25 - d, 0.);
    }
    else col *= max(d + .55, 0.);    
 
    
    // Cube face ID - not to be confused with the hexagonal cell ID. Basically, this partitions space
    // around the horizontal and two 30 degree sloping lines. The face ID will be used for a couple of
    // things, plus some fake face shading.
    float id = (p.x>0. && -p.y*s.y<p.x*s.x)? 1. : (p.y*s.y<p.x*s.x)? 2. : 0.;
 
    
    // Decorating the cube faces:
    //
    // Distance field stuff - There'd be a heap of ways the render the details on the cube faces,
    // and I'd imagine more elegant ways to get it done. Anyway, on the spot I decided to render three 
    // rotated diamonds on the hexagonal face, and do a little shading, etc. All of this is only called 
    // once, so whatever gets the job done, I guess. For more elaborate repeat decoration, I'd probably
    // use the "atan(p.y, p.x)" method. By the way, if someone can come up with a more elegant solution, 
    // feel free to let me know.
    //
    // Three rotated diamonds to represent the face borders.
    float di = isoDiamond((p - vec2(0, -.5)/s));
    di = min(di, isoDiamond(r2(3.14159/3.)*p - vec2(0, .5)/s));
	di = min(di, isoDiamond(r2(-3.14159/3.)*p - vec2(.0, .5)/s));
    di -= .25;
    
    // Face borders - or dark edges, if you prefer.
    float bord = max(di, -(di + .01));  
    
    // The cube holes. Note that with just the solid cubes, the example becomes much simpler,
    // and the code footprint decreases considerably.
    // Smaller diamonds for the holes and hole borders.
    float hole = di + .15;  
    if(abs(rnd - .55)>.4) hole += 1e5;
    float holeBord = max(hole, -(hole + .02));
    
    // The lines through the holes for that hollow cube look... Yeah, there'd definitely be
    // a better way to achive this. :)
    holeBord = min(holeBord, max(abs(p.x) - .01, hole));
    holeBord = min(holeBord, max(abs(p.x*s.x + p.y*s.y) - .02, hole));
    holeBord = min(holeBord, max(abs(-p.x*s.x + p.y*s.y) - .02, hole));
    
    // All the borders.
    bord = min(bord, holeBord);
    
    // Shading inside the holes - based on some isometric line stepping. It works fine,
    // but I coded it without a lot of forethought, so it looks messy... Needs an overhaul.
    float shade;
    if(id == 2.) shade = .8 -  step(0., -sign(rnd - .5)*p.x)*.2;
    else if(id == 1.) shade = .7 -  step(0., -sign(rnd - .5)*dot(p*vec2(1, -1), s))*.4;
    else shade = .6 -  step(0., -sign(rnd - .5)*dot(p*vec2(-1, -1), s))*.3;
    
    // Applying the cube face edges, shading, etc. 
    //
    col = mix(col, vec3(0), (1. - smoothstep(0., .01, hole))*shade); // Hole shading.

    col = mix(col, vec3(0), (1. - smoothstep(0., .01, bord))*.95); // Dark edges.
    col = mix(col, col*2., (1. - smoothstep(0., .02, bord - .02))*.3); // Edge highlighting.
    // Subtle beveled edges... just for something to do. :)
    col = mix(col, vec3(0), (1. - smoothstep(0., .01, max(di + .045, -(di  + .045 + .01))))*.5);
   
    // Cube shading, based on ID. Three different shades for each face of the cube.
    col *= id/2. + .1;
    


   //////
    
    // Random looking diagonal hatch lines.
    float hatch = clamp(sin((u.x*s.x - u.y*s.y)*3.14159*120.)*2. + .5, 0., 1.); // Diagonal lines.
    float hRnd = hash21(floor(p/6.*240.) + .73);
    if(hRnd>.66) hatch = hRnd; // Slight, randomization of the diagonal lines.  
    col *= hatch*.25 + .75; // Combining the background with the lines.

    
    // Subtle vignette.
    u = fragCoord/${Shader.RESOLUTION}.xy;
    col *= pow(16.*u.x*u.y*(1. - u.x)*(1. - u.y) , .125)*.75 + .25;
    // Colored variation.
    //col = mix(pow(min(vec3(1.5, 1, 1)*col, 1.), vec3(1, 3, 16)).zyx, col, 
             //pow(16.*u.x*u.y*(1. - u.x)*(1. - u.y) , .125)*.5 + .5);    
     
  
    // Rough gamma correction and screen presentation.
	return vec4(sqrt(max(col, 0.)), 1);
}
"""
    }
}