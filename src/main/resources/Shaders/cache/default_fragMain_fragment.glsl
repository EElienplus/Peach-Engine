#version 410 core



#line 41 0

uniform sampler2D  uTextures[15];


#line 3



#line 3

uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uCameraPos;
uniform int uNumLights;
uniform int uUseLighting;
uniform int uNormalCulling;
uniform int uShadowsEnabled;
uniform int uShadowMode;
uniform mat4 uShadowViewProjection;
uniform mat4 uPointShadowMatrices[6];
uniform vec3 uShadowLightPos;
uniform float uShadowBias;
uniform float uShadowNormalBias;
uniform float uShadowTexelSize;
uniform float uShadowFarPlane;
uniform float uShadowNearPlane;
uniform vec3 uAmbientColor;
uniform float uAmbientIntensity;
uniform vec3 uLightPos[16];
uniform float uLightIntensity[16];
uniform vec4 uLightColor[16];
uniform vec3 uLightDir[16];
uniform int uLightType[16];
uniform int uIsDirectional[16];
uniform float uCutOff[16];
uniform float uOuterCutOff[16];
uniform float uSpecularIntensity[16];
uniform float uShininess[16];


#line 42

uniform sampler2DArray uShadowMap_0;


#line 83
float samplePointShadow_0(vec2 uv_0, int layer_0, float compareDepth_0)
{

    float _S1;

#line 93
    if(((texture((uShadowMap_0), (vec3(uv_0, float(layer_0))))).x) > compareDepth_0)
    {

#line 93
        _S1 = 1.0;

#line 93
    }
    else
    {

#line 93
        _S1 = 0.0;

#line 93
    }

#line 93
    return _S1;
}


#line 146
float visibilityPoint_0(vec3 worldPos_0, vec3 normal_0, vec3 lightPos_0, vec3 lightDirection_0)
{

#line 153
    vec3 fromLight_0 = worldPos_0 - lightPos_0;


    float distanceSq_0 = dot(fromLight_0, fromLight_0);

    if(distanceSq_0 <= 9.99999997475242708e-07)
    {

#line 159
        return 1.0;
    }


    float distanceToLight_0 = sqrt(distanceSq_0);

    if(distanceToLight_0 >= (uShadowFarPlane))
    {

#line 166
        return 1.0;
    }


    vec3 direction_0 = fromLight_0 * (inversesqrt((distanceSq_0)));

    vec3 a_0 = abs(direction_0);



    float _S2 = a_0.x;

#line 176
    float _S3 = a_0.y;

#line 176
    bool _S4;

#line 176
    if(_S2 >= _S3)
    {

#line 176
        _S4 = _S2 >= (a_0.z);

#line 176
    }
    else
    {

#line 176
        _S4 = false;

#line 176
    }

#line 176
    int layer_1;

#line 176
    if(_S4)
    {

#line 177
        if((direction_0.x) >= 0.0)
        {

#line 177
            layer_1 = 0;

#line 177
        }
        else
        {

#line 177
            layer_1 = 1;

#line 177
        }

#line 176
    }
    else
    {

#line 178
        if(_S3 >= _S2)
        {

#line 178
            _S4 = _S3 >= (a_0.z);

#line 178
        }
        else
        {

#line 178
            _S4 = false;

#line 178
        }

#line 178
        if(_S4)
        {

#line 179
            if((direction_0.y) >= 0.0)
            {

#line 179
                layer_1 = 2;

#line 179
            }
            else
            {

#line 179
                layer_1 = 3;

#line 179
            }

#line 178
        }
        else
        {
            if((direction_0.z) >= 0.0)
            {

#line 181
                layer_1 = 4;

#line 181
            }
            else
            {

#line 181
                layer_1 = 5;

#line 181
            }

#line 178
        }

#line 176
    }

#line 184
    vec4 clip_0 = (((vec4(worldPos_0, 1.0)) * (uPointShadowMatrices[layer_1])));

#line 189
    float _S5 = clip_0.w;

#line 189
    if(_S5 <= 9.99999997475242708e-07)
    {

#line 190
        return 1.0;
    }

    vec3 ndc_0 = clip_0.xyz / _S5;
    vec2 uv_1 = ndc_0.xy * 0.5 + 0.5;

    float _S6 = uv_1.x;

#line 196
    if(_S6 < 0.0)
    {

#line 196
        _S4 = true;

#line 196
    }
    else
    {

#line 196
        _S4 = _S6 > 1.0;

#line 196
    }

#line 196
    if(_S4)
    {

#line 196
        _S4 = true;

#line 196
    }
    else
    {

#line 196
        _S4 = (uv_1.y) < 0.0;

#line 196
    }
    if(_S4)
    {

#line 197
        _S4 = true;

#line 197
    }
    else
    {

#line 197
        _S4 = (uv_1.y) > 1.0;

#line 197
    }

#line 197
    if(_S4)
    {

#line 197
        _S4 = true;

#line 197
    }
    else
    {

#line 197
        _S4 = (ndc_0.z) < -1.0;

#line 197
    }
    if(_S4)
    {

#line 198
        _S4 = true;

#line 198
    }
    else
    {

#line 198
        _S4 = (ndc_0.z) > 1.0;

#line 198
    }

#line 196
    if(_S4)
    {

        return 1.0;
    }

#line 218
    float compareDepth_1 = distanceToLight_0 / max(uShadowFarPlane, 0.00009999999747379) - (uShadowBias / max(uShadowFarPlane, 0.00009999999747379) + uShadowNormalBias * (1.0 - max(dot(normal_0, lightDirection_0), 0.0)) / max(uShadowFarPlane, 0.00009999999747379));


    vec2 texel_0 = vec2(uShadowTexelSize, uShadowTexelSize);

#line 231
    return (samplePointShadow_0(uv_1, layer_1, compareDepth_1) + samplePointShadow_0(uv_1 + texel_0 * vec2(1.0, 0.0), layer_1, compareDepth_1) + samplePointShadow_0(uv_1 + texel_0 * vec2(-1.0, 0.0), layer_1, compareDepth_1) + samplePointShadow_0(uv_1 + texel_0 * vec2(0.0, 1.0), layer_1, compareDepth_1) + samplePointShadow_0(uv_1 + texel_0 * vec2(0.0, -1.0), layer_1, compareDepth_1)) * 0.20000000298023224;
}


#line 96
float visibility2D_0(vec3 worldPos_1, vec3 normal_1, vec3 lightDirection_1)
{



    vec4 clip_1 = (((vec4(worldPos_1, 1.0)) * (uShadowViewProjection)));

#line 106
    float _S7 = clip_1.w;

#line 106
    if(_S7 <= 9.99999997475242708e-07)
    {

#line 107
        return 1.0;
    }

    vec3 ndc_1 = clip_1.xyz / _S7;
    vec2 uv_2 = ndc_1.xy * 0.5 + 0.5;

    float _S8 = uv_2.x;

#line 113
    bool _S9;

#line 113
    if(_S8 < 0.0)
    {

#line 113
        _S9 = true;

#line 113
    }
    else
    {

#line 113
        _S9 = _S8 > 1.0;

#line 113
    }

#line 113
    if(_S9)
    {

#line 113
        _S9 = true;

#line 113
    }
    else
    {

#line 113
        _S9 = (uv_2.y) < 0.0;

#line 113
    }
    if(_S9)
    {

#line 114
        _S9 = true;

#line 114
    }
    else
    {

#line 114
        _S9 = (uv_2.y) > 1.0;

#line 114
    }

#line 114
    if(_S9)
    {

#line 114
        _S9 = true;

#line 114
    }
    else
    {

#line 114
        _S9 = (ndc_1.z) < -1.0;

#line 114
    }
    if(_S9)
    {

#line 115
        _S9 = true;

#line 115
    }
    else
    {

#line 115
        _S9 = (ndc_1.z) > 1.0;

#line 115
    }

#line 113
    if(_S9)
    {

        return 1.0;
    }

#line 130
    float compareDepth_2 = ndc_1.z * 0.5 + 0.5 - (uShadowBias + uShadowNormalBias * (1.0 - max(dot(normal_1, lightDirection_1), 0.0)));


    vec2 texel_1 = vec2(uShadowTexelSize, uShadowTexelSize);

#line 133
    float _S10;



    if(((texture((uShadowMap_0), (vec3(uv_2, 0.0)))).x) > compareDepth_2)
    {

#line 137
        _S10 = 1.0;

#line 137
    }
    else
    {

#line 137
        _S10 = 0.0;

#line 137
    }

#line 137
    float _S11;
    if(((texture((uShadowMap_0), (vec3(uv_2 + texel_1 * vec2(1.0, 0.0), 0.0)))).x) > compareDepth_2)
    {

#line 138
        _S11 = 1.0;

#line 138
    }
    else
    {

#line 138
        _S11 = 0.0;

#line 138
    }

#line 138
    float lit_0 = _S10 + _S11;
    if(((texture((uShadowMap_0), (vec3(uv_2 + texel_1 * vec2(-1.0, 0.0), 0.0)))).x) > compareDepth_2)
    {

#line 139
        _S10 = 1.0;

#line 139
    }
    else
    {

#line 139
        _S10 = 0.0;

#line 139
    }

#line 139
    float lit_1 = lit_0 + _S10;
    if(((texture((uShadowMap_0), (vec3(uv_2 + texel_1 * vec2(0.0, 1.0), 0.0)))).x) > compareDepth_2)
    {

#line 140
        _S10 = 1.0;

#line 140
    }
    else
    {

#line 140
        _S10 = 0.0;

#line 140
    }

#line 140
    float lit_2 = lit_1 + _S10;
    if(((texture((uShadowMap_0), (vec3(uv_2 + texel_1 * vec2(0.0, -1.0), 0.0)))).x) > compareDepth_2)
    {

#line 141
        _S10 = 1.0;

#line 141
    }
    else
    {

#line 141
        _S10 = 0.0;

#line 141
    }

    return (lit_2 + _S10) * 0.20000000298023224;
}


#line 143
layout(location = 0)
out vec4 entryPointParam_fragMain_0;


#line 143
layout(location = 0)
in vec4 v2f_color_0;


#line 143
layout(location = 1)
in vec2 v2f_texCoords_0;


#line 143
layout(location = 2)
in float v2f_texID_0;


#line 143
layout(location = 3)
in vec3 v2f_fragPos_0;


#line 143
layout(location = 4)
in vec3 v2f_normal_0;


#line 235
void main()
{


    vec4 texColor_0 = v2f_color_0 * (texture((uTextures[clamp(int(v2f_texID_0), 0, 14)]), (v2f_texCoords_0)));

#line 244
    float _S12 = texColor_0.w;

#line 244
    if(_S12 <= 0.00100000004749745)
    {

#line 245
        discard;

#line 244
    }

#line 244
    bool directional_0;



    if((uUseLighting) == 0)
    {

#line 248
        directional_0 = true;

#line 248
    }
    else
    {

#line 248
        directional_0 = (uNumLights) <= 0;

#line 248
    }

#line 248
    if(directional_0)
    {

#line 248
        entryPointParam_fragMain_0 = texColor_0;

#line 248
        return;
    }



    float normSq_0 = dot(v2f_normal_0, v2f_normal_0);

#line 253
    vec3 norm_0;

    if(normSq_0 > 9.99999997475242708e-07)
    {

#line 255
        norm_0 = v2f_normal_0 * (inversesqrt((normSq_0)));

#line 255
    }
    else
    {

#line 255
        norm_0 = vec3(0.0, 0.0, 1.0);

#line 255
    }



    vec3 toCamera_0 = uCameraPos - v2f_fragPos_0;

    float cameraSq_0 = dot(toCamera_0, toCamera_0);

#line 261
    vec3 viewDir_0;


    if(cameraSq_0 > 9.99999997475242708e-07)
    {

#line 264
        viewDir_0 = toCamera_0 * (inversesqrt((cameraSq_0)));

#line 264
    }
    else
    {

#line 264
        viewDir_0 = vec3(0.0, 0.0, 1.0);

#line 264
    }


    if((uNormalCulling) != 0)
    {

#line 267
        directional_0 = (dot(norm_0, viewDir_0)) < 0.0;

#line 267
    }
    else
    {

#line 267
        directional_0 = false;

#line 267
    }

#line 267
    if(directional_0)
    {
        discard;

#line 267
    }

#line 273
    vec3 _S13 = uAmbientColor * uAmbientIntensity;

    int _S14 = min(uNumLights, 16);

#line 275
    int i_0 = 0;

#line 275
    vec3 lighting_0 = _S13;

    for(;;)
    {

#line 277
        if(i_0 < _S14)
        {
        }
        else
        {

#line 277
            break;
        }



        if((uLightType[i_0]) == 1)
        {

#line 282
            directional_0 = true;

#line 282
        }
        else
        {

#line 282
            directional_0 = (uIsDirectional[i_0]) != 0;

#line 282
        }

#line 282
        vec3 lightDir_0;

#line 282
        vec3 lighting_1;

#line 282
        float factor_0;


        if(directional_0)
        {

#line 285
            lightDir_0 = - uLightDir[i_0];

#line 285
            factor_0 = 1.0;

#line 285
        }
        else
        {

            vec3 toLight_0 = uLightPos[i_0] - v2f_fragPos_0;

            float distSq_0 = dot(toLight_0, toLight_0);

            if(distSq_0 <= 9.99999997475242708e-07)
            {

#line 293
                lighting_1 = lighting_0;
                i_0 = i_0 + 1;

#line 294
                lighting_0 = lighting_1;

#line 277
                continue;
            }

#line 298
            vec3 lightDir_1 = toLight_0 * (inversesqrt((distSq_0)));



            float factor_1 = 1.0 / (1.0 + 0.00139999995008111 * sqrt(distSq_0) + 7.00000009601353668e-06 * distSq_0);

#line 308
            if((uLightType[i_0]) == 2)
            {

#line 308
                factor_0 = factor_1 * clamp((dot(lightDir_1, - uLightDir[i_0]) - uOuterCutOff[i_0]) / max(uCutOff[i_0] - uOuterCutOff[i_0], 0.00009999999747379), 0.0, 1.0);

#line 308
            }
            else
            {

#line 308
                factor_0 = factor_1;

#line 308
            }

#line 308
            lightDir_0 = lightDir_1;

#line 285
        }

#line 325
        if(factor_0 <= 0.00009999999747379)
        {

#line 325
            lighting_1 = lighting_0;
            i_0 = i_0 + 1;

#line 326
            lighting_0 = lighting_1;

#line 277
            continue;
        }

#line 277
        bool _S15;

#line 332
        if((uShadowsEnabled) != 0)
        {

#line 332
            _S15 = i_0 == 0;

#line 332
        }
        else
        {

#line 332
            _S15 = false;

#line 332
        }

#line 332
        float visibility_0;

#line 332
        if(_S15)
        {
            if((uShadowMode) == 3)
            {

#line 334
                visibility_0 = visibilityPoint_0(v2f_fragPos_0, norm_0, uLightPos[i_0], lightDir_0);

#line 334
            }
            else
            {

#line 334
                visibility_0 = visibility2D_0(v2f_fragPos_0, norm_0, lightDir_0);

#line 334
            }

#line 332
        }
        else
        {

#line 332
            visibility_0 = 1.0;

#line 332
        }

#line 350
        vec3 lightColor_0 = uLightColor[i_0].xyz * uLightIntensity[i_0] * factor_0;


        float _S16 = max(dot(norm_0, lightDir_0), 0.0);

        vec3 lighting_2 = lighting_0 + _S16 * lightColor_0 * visibility_0;

#line 355
        bool _S17;

#line 360
        if(_S16 > 0.0)
        {

#line 360
            _S17 = (uSpecularIntensity[i_0]) > 0.00009999999747379;

#line 360
        }
        else
        {

#line 360
            _S17 = false;

#line 360
        }

#line 360
        if(_S17)
        {


            vec3 halfway_0 = lightDir_0 + viewDir_0;


            float halfwaySq_0 = dot(halfway_0, halfway_0);

            if(halfwaySq_0 > 9.99999997475242708e-07)
            {

#line 369
                lighting_1 = lighting_2 + uSpecularIntensity[i_0] * pow(max(dot(norm_0, halfway_0 * (inversesqrt((halfwaySq_0)))), 0.0), max(uShininess[i_0], 1.0)) * lightColor_0 * visibility_0;

#line 369
            }
            else
            {

#line 369
                lighting_1 = lighting_2;

#line 369
            }

#line 360
        }
        else
        {

#line 360
            lighting_1 = lighting_2;

#line 360
        }

#line 360
        lighting_1 = lighting_1 + max(dot(- norm_0, lightDir_0), 0.0) * 0.15000000596046448 * lightColor_0 * visibility_0;

#line 277
        i_0 = i_0 + 1;

#line 277
        lighting_0 = lighting_1;

#line 277
    }

#line 277
    entryPointParam_fragMain_0 = vec4(texColor_0.xyz * lighting_0, _S12);

#line 277
    return;
}
