package com.pandulapeter.kubriko.shader.collection

import com.pandulapeter.kubriko.shader.Shader
import com.pandulapeter.kubriko.shader.ShaderManager
import com.pandulapeter.kubriko.shader.implementation.extensions.ShaderUniformProvider

data class CloudShader(
    private val properties: Properties = Properties(),
    override val canvasIndex: Int? = null,
) : Shader {
    override val code = """
    uniform float2 ${ShaderManager.RESOLUTION};
    uniform float $SCALE;
    uniform float $SPEED;
    uniform float $DARK;
    uniform float $LIGHT;
    uniform float $COVER;
    uniform float $ALPHA;
    uniform float $TINT;
    uniform float $TIME;
    uniform float $SKY_1_RED;
    uniform float $SKY_1_GREEN;
    uniform float $SKY_1_BLUE;
    uniform float $SKY_2_RED;
    uniform float $SKY_2_GREEN;
    uniform float $SKY_2_BLUE;
    uniform shader ${ShaderManager.CONTENT};
    
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
        float2 p = fragCoord.xy / ${ShaderManager.RESOLUTION}.xy;
    	float2 uv = p*float2(${ShaderManager.RESOLUTION}.x/${ShaderManager.RESOLUTION}.y,1.0);    
        float time = $TIME * $SPEED;
        float q = fbm(uv * $SCALE * 0.5);
        
        //ridged noise shape
    	float r = 0.0;
    	uv *= $SCALE;
        uv -= q - time;
        float weight = 0.8;
        for (int i=0; i<8; i++){
    		r += abs(weight*noise( uv ));
            uv = m*uv + time;
    		weight *= 0.7;
        }
        
        //noise shape
    	float f = 0.0;
        uv = p*float2(${ShaderManager.RESOLUTION}.x/${ShaderManager.RESOLUTION}.y,1.0);
    	uv *= $SCALE;
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
        time = $TIME * $SPEED * 2.0;
        uv = p*float2(${ShaderManager.RESOLUTION}.x/${ShaderManager.RESOLUTION}.y,1.0);
    	uv *= $SCALE*2.0;
        uv -= q - time;
        weight = 0.4;
        for (int i=0; i<7; i++){
    		c += weight*noise( uv );
            uv = m*uv + time;
    		weight *= 0.6;
        }
        
        //noise ridge colour
        float c1 = 0.0;
        time = $TIME * $SPEED * 3.0;
        uv = p*float2(${ShaderManager.RESOLUTION}.x/${ShaderManager.RESOLUTION}.y,1.0);
    	uv *= $SCALE*3.0;
        uv -= q - time;
        weight = 0.4;
        for (int i=0; i<7; i++){
    		c1 += abs(weight*noise( uv ));
            uv = m*uv + time;
    		weight *= 0.6;
        }
    	
        c += c1;
        
        float3 skycolour = mix(float3($SKY_1_RED, $SKY_1_GREEN, $SKY_1_BLUE), float3($SKY_2_RED, $SKY_2_GREEN, $SKY_2_BLUE), p.y);
        float3 cloudcolour = float3(1.1, 1.1, 0.9) * clamp(($DARK + $LIGHT*c), 0.0, 1.0);
       
        f = $COVER + $ALPHA*f*r;
        
        float3 result = mix(skycolour, clamp($TINT * skycolour + cloudcolour, 0.0, 1.0), clamp(f + c, 0.0, 1.0));
        
    	return float4( result, 1.0 );
    }
""".trimIndent()

    override fun applyUniforms(provider: ShaderUniformProvider) {
        provider.uniform(TIME, properties.time)
        provider.uniform(SCALE, properties.scale)
        provider.uniform(SPEED, properties.speed)
        provider.uniform(DARK, properties.dark)
        provider.uniform(LIGHT, properties.light)
        provider.uniform(COVER, properties.cover)
        provider.uniform(ALPHA, properties.alpha)
        provider.uniform(TINT, properties.tint)
        provider.uniform(SKY_1_RED, properties.sky1Red)
        provider.uniform(SKY_1_GREEN, properties.sky1Green)
        provider.uniform(SKY_1_BLUE, properties.sky1Blue)
        provider.uniform(SKY_2_RED, properties.sky2Red)
        provider.uniform(SKY_2_GREEN, properties.sky2Green)
        provider.uniform(SKY_2_BLUE, properties.sky2Blue)
    }

    data class Properties(
        val time: Float = 0f,
        val scale: Float = 1.1f,
        val speed: Float = 0.03f,
        val dark: Float = 0.5f,
        val light: Float = 0.3f,
        val cover: Float = 0.2f,
        val alpha: Float = 8.0f,
        val tint: Float = 0.5f,
        val sky1Red: Float = 0.2f,
        val sky1Green: Float = 0.4f,
        val sky1Blue: Float = 0.6f,
        val sky2Red: Float = 0.4f,
        val sky2Green: Float = 0.7f,
        val sky2Blue: Float = 1f,
    )

    companion object {
        private const val TIME = "iTime"
        private const val SCALE = "scale"
        private const val SPEED = "speed"
        private const val DARK = "dark"
        private const val LIGHT = "light"
        private const val COVER = "cover"
        private const val ALPHA = "alpha"
        private const val TINT = "tint"
        private const val SKY_1_RED = "sky1Red"
        private const val SKY_1_GREEN = "sky1Green"
        private const val SKY_1_BLUE = "sky1Blue"
        private const val SKY_2_RED = "sky2Red"
        private const val SKY_2_GREEN = "sky2Green"
        private const val SKY_2_BLUE = "sky2Blue"
    }
}