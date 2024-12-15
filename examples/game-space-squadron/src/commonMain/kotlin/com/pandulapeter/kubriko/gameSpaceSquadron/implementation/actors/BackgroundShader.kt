package com.pandulapeter.kubriko.gameSpaceSquadron.implementation.actors

import com.pandulapeter.kubriko.Kubriko
import com.pandulapeter.kubriko.actor.traits.Dynamic
import com.pandulapeter.kubriko.implementation.extensions.get
import com.pandulapeter.kubriko.manager.MetadataManager
import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

/**
 * Pablo Roman Andrioli
 * https://www.shadertoy.com/view/XlfGRj
 */
internal class BackgroundShader(
    initialState: State = State(),
    override val layerIndex: Int? = null,
) : Shader<BackgroundShader.State>, Dynamic {
    override var state = initialState
        private set
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform float $TIME;
    uniform float $SPEED;
    uniform shader ${ShaderManager.CONTENT};
    
    const float2 iMouse = float2(.0, .0);
    
    const int iterations = 17;
    const float formuparam = 0.53;

    const int volsteps = 20;
    const float stepsize = 0.1;

    const float zoom = 0.800;
    const float tile = 0.850;

    const float brightness = 0.0015;
    const float darkmatter = 0.300;
    const float distfading = 0.730;
    const float saturation = 0.850;

    vec4 main(in vec2 fragCoord)
    {
    	//get coords and direction
    	vec2 uv=fragCoord.xy/${ShaderManager.RESOLUTION}.xy-.5;
    	uv.y*=${ShaderManager.RESOLUTION}.y/${ShaderManager.RESOLUTION}.x;
    	vec3 dir=vec3(uv*zoom,1.);
    	float time=$TIME*$SPEED+.25;

    	//mouse rotation
    	float a1=.5+iMouse.x/${ShaderManager.RESOLUTION}.x*2.;
    	float a2=.8+iMouse.y/${ShaderManager.RESOLUTION}.y*2.;
    	mat2 rot1=mat2(cos(a1),sin(a1),-sin(a1),cos(a1));
    	mat2 rot2=mat2(cos(a2),sin(a2),-sin(a2),cos(a2));
    	dir.xz*=rot1;
    	dir.xy*=rot2;
    	vec3 from=vec3(1.,.5,0.5);
    	from+=vec3(time*2.,time,-2.);
    	from.xz*=rot1;
    	from.xy*=rot2;
    	
    	//volumetric rendering
    	float s=0.1,fade=1.;
    	vec3 v=vec3(0.);
    	for (int r=0; r<volsteps; r++) {
    		vec3 p=from+s*dir*.5;
    		p = abs(vec3(tile)-mod(p,vec3(tile*2.))); // tiling fold
    		float pa,a=pa=0.;
    		for (int i=0; i<iterations; i++) { 
    			p=abs(p)/dot(p,p)-formuparam; // the magic formula
    			a+=abs(length(p)-pa); // absolute sum of average change
    			pa=length(p);
    		}
    		float dm=max(0.,darkmatter-a*a*.001); //dark matter
    		a*=a*a; // add contrast
    		if (r>6) fade*=1.-dm; // dark matter, don't render near
    		//v+=vec3(dm,dm*.5,0.);
    		v+=fade;
    		v+=vec3(s,s*s,s*s*s*s)*a*brightness*fade; // coloring based on distance
    		fade*=distfading; // distance fading
    		s+=stepsize;
    	}
    	v=mix(vec3(length(v)),v,saturation); //color adjust
    	return vec4(v*.01,1.);	
    	
    }
""".trimIndent()
    override val cache = Shader.Cache()
    private lateinit var metadataManager: MetadataManager

    override fun onAdded(kubriko: Kubriko) {
        metadataManager = kubriko.get()
    }

    override fun update(deltaTimeInMillis: Float) {
        state = state.copy(time = (metadataManager.runtimeInMilliseconds.value % 100000L) / 1000f)
    }

    fun updateState(state: State) {
        this.state = state.copy(time = this.state.time)
    }

    data class State(
        val time: Float = 0f,
        val speed: Float = 0.01f,
    ) : Shader.State {

        override fun ShaderUniformProvider.applyUniforms() {
            uniform(TIME, time)
            uniform(SPEED, speed)
        }
    }

    companion object {
        private const val TIME = "time"
        private const val SPEED = "speed"
    }
}