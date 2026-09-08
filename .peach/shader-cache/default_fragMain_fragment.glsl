#version 410 core



#line 3 0



#line 3

uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uCameraPos;
uniform int uNumLights;
uniform int uUseLighting;
uniform int uNormalCulling;
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


#line 24

uniform sampler2D  uTextures[16];


#line 40
layout(location = 0)
out vec4 entryPointParam_fragMain_0;


#line 40
layout(location = 0)
in vec4 v2f_color_0;


#line 40
layout(location = 1)
in vec2 v2f_texCoords_0;


#line 40
layout(location = 2)
in float v2f_texID_0;


#line 40
layout(location = 3)
in vec3 v2f_fragPos_0;


#line 40
layout(location = 4)
in vec3 v2f_normal_0;


#line 57
void main()
{

#line 58
    vec3 norm_0 = normalize(v2f_normal_0);

#line 58
    vec3 norm_1;
    if((dot(norm_0, norm_0)) < 0.00009999999747379)
    {

#line 59
        norm_1 = vec3(0.0, 0.0, 1.0);

#line 59
    }
    else
    {

#line 59
        norm_1 = norm_0;

#line 59
    }



    if((uNormalCulling) != 0)
    {
        if((dot(norm_1, normalize(uCameraPos - v2f_fragPos_0))) < 0.0)
        {

#line 66
            discard;

#line 65
        }

#line 63
    }

#line 71
    vec4 texColor_0 = v2f_color_0 * (texture((uTextures[clamp(int(v2f_texID_0), 0, 15)]), (v2f_texCoords_0)));

#line 71
    bool _S1;

    if((uUseLighting) == 0)
    {

#line 73
        _S1 = true;

#line 73
    }
    else
    {

#line 73
        _S1 = (uNumLights) <= 0;

#line 73
    }

#line 73
    if(_S1)
    {

#line 73
        entryPointParam_fragMain_0 = texColor_0;

#line 73
        return;
    }


    vec3 _S2 = normalize(uCameraPos - v2f_fragPos_0);
    vec3 ambient_0 = uAmbientColor * uAmbientIntensity;
    const vec3 _S3 = vec3(0.0);



    int _S4 = min(uNumLights, 16);

#line 83
    int i_0 = 0;

#line 83
    vec3 totalDiffuse_0 = _S3;

#line 83
    vec3 totalSpecular_0 = _S3;

#line 83
    vec3 totalBack_0 = _S3;
    for(;;)
    {

#line 84
        if(i_0 < _S4)
        {
        }
        else
        {

#line 84
            break;
        }



        if((uLightType[i_0]) == 1)
        {

#line 89
            _S1 = true;

#line 89
        }
        else
        {

#line 89
            _S1 = (uIsDirectional[i_0]) != 0;

#line 89
        }

#line 89
        vec3 lightDir_0;

#line 89
        float attenuation_0;

#line 89
        float spotIntensity_0;

#line 89
        if(_S1)
        {
            if((dot(uLightDir[i_0], uLightDir[i_0])) > 0.00009999999747379)
            {

#line 91
                lightDir_0 = normalize(- uLightDir[i_0]);

#line 91
            }
            else
            {

#line 91
                lightDir_0 = normalize(uLightPos[i_0]);

#line 91
            }

#line 91
            attenuation_0 = 1.0;

#line 91
            spotIntensity_0 = 1.0;

#line 89
        }
        else
        {

#line 99
            vec3 toLight_0 = uLightPos[i_0] - v2f_fragPos_0;
            float dist_0 = length(toLight_0);
            if(dist_0 > 0.00009999999747379)
            {

#line 101
                lightDir_0 = toLight_0 / dist_0;

#line 101
            }
            else
            {

#line 101
                lightDir_0 = vec3(0.0, 1.0, 0.0);

#line 101
            }
            float _S5 = 1.0 / (1.0 + 0.00139999995008111 * dist_0 + 7.00000009601353668e-06 * dist_0 * dist_0);

            if((uLightType[i_0]) == 2)
            {

#line 104
                attenuation_0 = clamp((dot(lightDir_0, normalize(- uLightDir[i_0])) - uOuterCutOff[i_0]) / max(uCutOff[i_0] - uOuterCutOff[i_0], 0.00009999999747379), 0.0, 1.0);

#line 104
            }
            else
            {

#line 104
                attenuation_0 = 1.0;

#line 104
            }

#line 87
            float _S6 = attenuation_0;

#line 87
            attenuation_0 = _S5;

#line 87
            spotIntensity_0 = _S6;

        }

#line 113
        float factor_0 = attenuation_0 * spotIntensity_0;

        vec3 totalDiffuse_1 = totalDiffuse_0 + max(dot(norm_1, lightDir_0), 0.0) * uLightColor[i_0].xyz * uLightIntensity[i_0] * factor_0;



        vec3 totalSpecular_1 = totalSpecular_0 + uSpecularIntensity[i_0] * pow(max(dot(norm_1, normalize(lightDir_0 + _S2)), 0.0), max(uShininess[i_0], 1.0)) * uLightColor[i_0].xyz * uLightIntensity[i_0] * factor_0;


        vec3 totalBack_1 = totalBack_0 + max(dot(- norm_1, lightDir_0), 0.0) * 0.15000000596046448 * uLightColor[i_0].xyz * uLightIntensity[i_0] * factor_0;

#line 84
        i_0 = i_0 + 1;

#line 84
        totalDiffuse_0 = totalDiffuse_1;

#line 84
        totalSpecular_0 = totalSpecular_1;

#line 84
        totalBack_0 = totalBack_1;

#line 84
    }

#line 84
    entryPointParam_fragMain_0 = vec4(texColor_0.xyz * (ambient_0 + totalDiffuse_0 + totalSpecular_0 + totalBack_0), texColor_0.w);

#line 84
    return;
}
