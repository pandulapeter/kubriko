package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class CloudShader(
    private val properties: Properties = Properties(),
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.UNIFORM_RESOLUTION};
    uniform float $UNIFORM_TIME;
    uniform float $UNIFORM_SKY_1_RED;
    uniform float $UNIFORM_SKY_1_GREEN;
    uniform float $UNIFORM_SKY_1_BLUE;
    uniform float $UNIFORM_SKY_2_RED;
    uniform float $UNIFORM_SKY_2_GREEN;
    uniform float $UNIFORM_SKY_2_BLUE;
    uniform shader ${ShaderManager.UNIFORM_CONTENT};
    
    const float cloudscale = 1.1;
    const float speed = 0.03;
    const float clouddark = 0.5;
    const float cloudlight = 0.3;
    const float cloudcover = 0.2;
    const float cloudalpha = 8.0;
    const float skytint = 0.5;  

    const mat2 m = mat2( 1.6,  1.2, -1.2,  1.6 );

    float2 hash( float2 p ) {
    	p = float2(dot(p,float2(127.1,311.7)), dot(p,float2(269.5,183.3)));
    	return -1.0 + 2.0*fract(sin(p)*43758.5453123);
    }

    float noise( in float2 p ) {
        const float K1 = 0.366025404; // (sqrt(3)-1)/2;
        const float K2 = 0.211324865; // (3-sqrt(3))/6;
    	float2 i = floor(p + (p.x+p.y)*K1);	
        float2 a = p - i + (i.x+i.y)*K2;
        float2 o = (a.x>a.y) ? float2(1.0,0.0) : float2(0.0,1.0); //float2 of = 0.5 + 0.5*float2(sign(a.x-a.y), sign(a.y-a.x));
        float2 b = a - o + K2;
    	float2 c = a - 1.0 + 2.0*K2;
        float3 h = max(0.5-float3(dot(a,a), dot(b,b), dot(c,c) ), 0.0 );
    	float3 n = h*h*h*h*float3( dot(a,hash(i+0.0)), dot(b,hash(i+o)), dot(c,hash(i+1.0)));
        return dot(n, float3(70.0));	
    }

    float fbm(float2 n) {
    	float total = 0.0, amplitude = 0.1;
    	for (int i = 0; i < 7; i++) {
    		total += noise(n) * amplitude;
    		n = m * n;
    		amplitude *= 0.4;
    	}
    	return total;
    }

    // -----------------------------------------------

    half4 main(in float2 fragCoord ) {
        float2 p = fragCoord.xy / ${ShaderManager.UNIFORM_RESOLUTION}.xy;
    	float2 uv = p*float2(${ShaderManager.UNIFORM_RESOLUTION}.x/${ShaderManager.UNIFORM_RESOLUTION}.y,1.0);    
        float time = $UNIFORM_TIME * speed;
        float q = fbm(uv * cloudscale * 0.5);
        
        //ridged noise shape
    	float r = 0.0;
    	uv *= cloudscale;
        uv -= q - time;
        float weight = 0.8;
        for (int i=0; i<8; i++){
    		r += abs(weight*noise( uv ));
            uv = m*uv + time;
    		weight *= 0.7;
        }
        
        //noise shape
    	float f = 0.0;
        uv = p*float2(${ShaderManager.UNIFORM_RESOLUTION}.x/${ShaderManager.UNIFORM_RESOLUTION}.y,1.0);
    	uv *= cloudscale;
        uv -= q - time;
        weight = 0.7;
        for (int i=0; i<8; i++){
    		f += weight*noise( uv );
            uv = m*uv + time;
    		weight *= 0.6;
        }
        
        f *= r + f;
        
        //noise colour
        float c = 0.0;
        time = $UNIFORM_TIME * speed * 2.0;
        uv = p*float2(${ShaderManager.UNIFORM_RESOLUTION}.x/${ShaderManager.UNIFORM_RESOLUTION}.y,1.0);
    	uv *= cloudscale*2.0;
        uv -= q - time;
        weight = 0.4;
        for (int i=0; i<7; i++){
    		c += weight*noise( uv );
            uv = m*uv + time;
    		weight *= 0.6;
        }
        
        //noise ridge colour
        float c1 = 0.0;
        time = $UNIFORM_TIME * speed * 3.0;
        uv = p*float2(${ShaderManager.UNIFORM_RESOLUTION}.x/${ShaderManager.UNIFORM_RESOLUTION}.y,1.0);
    	uv *= cloudscale*3.0;
        uv -= q - time;
        weight = 0.4;
        for (int i=0; i<7; i++){
    		c1 += abs(weight*noise( uv ));
            uv = m*uv + time;
    		weight *= 0.6;
        }
    	
        c += c1;
        
        float3 skycolour = mix(float3($UNIFORM_SKY_1_RED, $UNIFORM_SKY_1_GREEN, $UNIFORM_SKY_1_BLUE), float3($UNIFORM_SKY_2_RED, $UNIFORM_SKY_2_GREEN, $UNIFORM_SKY_2_BLUE), p.y);
        float3 cloudcolour = float3(1.1, 1.1, 0.9) * clamp((clouddark + cloudlight*c), 0.0, 1.0);
       
        f = cloudcover + cloudalpha*f*r;
        
        float3 result = mix(skycolour, clamp(skytint * skycolour + cloudcolour, 0.0, 1.0), clamp(f + c, 0.0, 1.0));
        
    	return float4( result, 1.0 );
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(UNIFORM_TIME, properties.time)
        provider.uniform(UNIFORM_SKY_1_RED, properties.sky1Red)
        provider.uniform(UNIFORM_SKY_1_GREEN, properties.sky1Green)
        provider.uniform(UNIFORM_SKY_1_BLUE, properties.sky1Blue)
        provider.uniform(UNIFORM_SKY_2_RED, properties.sky2Red)
        provider.uniform(UNIFORM_SKY_2_GREEN, properties.sky2Green)
        provider.uniform(UNIFORM_SKY_2_BLUE, properties.sky2Blue)
    }

    data class Properties(
        val time: Float = 0f,
        val sky1Red: Float = 0.2f,
        val sky1Green: Float = 0.4f,
        val sky1Blue: Float = 0.6f,
        val sky2Red: Float = 0.4f,
        val sky2Green: Float = 0.7f,
        val sky2Blue: Float = 1f,
    )

    companion object {
        private const val UNIFORM_TIME = "iTime"
        private const val UNIFORM_SKY_1_RED = "sky1Red"
        private const val UNIFORM_SKY_1_GREEN = "sky1Green"
        private const val UNIFORM_SKY_1_BLUE = "sky1Blue"
        private const val UNIFORM_SKY_2_RED = "sky2Red"
        private const val UNIFORM_SKY_2_GREEN = "sky2Green"
        private const val UNIFORM_SKY_2_BLUE = "sky2Blue"
    }
}