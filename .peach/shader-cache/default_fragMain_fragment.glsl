#version 410 core



#line 1 0



#line 1

uniform mat4 uProjection;
uniform mat4 uView;
uniform vec3 uLightPos;
uniform float uLightIntensity;
uniform vec4 uLightColor;
uniform vec3 uAmbientColor;
uniform float uAmbientIntensity;
uniform vec3 uCameraPos;
uniform float uSpecularIntensity;
uniform float uShininess;
uniform int uLightType;
uniform int uIsDirectional;
uniform int uUseLighting;
uniform int uNormalCulling;
uniform vec3 uLightDir;
uniform float uCutOff;
uniform float uOuterCutOff;


#line 21

uniform sampler2D  uTextures[16];


#line 37
layout(location = 0)
out vec4 entryPointParam_fragMain_0;


#line 37
layout(location = 0)
in vec4 v2f_color_0;


#line 37
layout(location = 1)
in vec2 v2f_texCoords_0;


#line 37
layout(location = 2)
in float v2f_texID_0;


#line 37
layout(location = 3)
in vec3 v2f_fragPos_0;


#line 37
layout(location = 4)
in vec3 v2f_normal_0;


#line 54
void main()
{

#line 55
    vec3 norm_0 = normalize(v2f_normal_0);

#line 55
    vec3 norm_1;
    if((dot(norm_0, norm_0)) < 0.00009999999747379)
    {

#line 56
        norm_1 = vec3(0.0, 0.0, 1.0);

#line 56
    }
    else
    {

#line 56
        norm_1 = norm_0;

#line 56
    }



    if((uNormalCulling) != 0)
    {
        if((dot(norm_1, normalize(uCameraPos - v2f_fragPos_0))) < 0.0)
        {

#line 63
            discard;

#line 62
        }

#line 60
    }

#line 68
    vec4 texColor_0 = v2f_color_0 * (texture((uTextures[clamp(int(v2f_texID_0), 0, 15)]), (v2f_texCoords_0)));

    if((uUseLighting) == 0)
    {

#line 70
        entryPointParam_fragMain_0 = texColor_0;

#line 70
        return;
    }

#line 70
    bool _S1;

#line 79
    if((uLightType) == 1)
    {

#line 79
        _S1 = true;

#line 79
    }
    else
    {

#line 79
        _S1 = (uIsDirectional) != 0;

#line 79
    }

#line 79
    vec3 lightDir_0;

#line 79
    float attenuation_0;

#line 79
    float spotIntensity_0;

#line 79
    if(_S1)
    {
        if((dot(uLightDir, uLightDir)) > 0.00009999999747379)
        {

#line 81
            lightDir_0 = normalize(- uLightDir);

#line 81
        }
        else
        {

#line 81
            lightDir_0 = normalize(uLightPos);

#line 81
        }

#line 81
        attenuation_0 = 1.0;

#line 81
        spotIntensity_0 = 1.0;

#line 79
    }
    else
    {

#line 89
        vec3 toLight_0 = uLightPos - v2f_fragPos_0;
        float dist_0 = length(toLight_0);
        if(dist_0 > 0.00009999999747379)
        {

#line 91
            lightDir_0 = toLight_0 / dist_0;

#line 91
        }
        else
        {

#line 91
            lightDir_0 = vec3(0.0, 1.0, 0.0);

#line 91
        }

        float _S2 = 1.0 / (1.0 + 0.00139999995008111 * dist_0 + 7.00000009601353668e-06 * dist_0 * dist_0);

        if((uLightType) == 2)
        {

#line 95
            attenuation_0 = clamp((dot(lightDir_0, normalize(- uLightDir)) - uOuterCutOff) / max(uCutOff - uOuterCutOff, 0.00009999999747379), 0.0, 1.0);

#line 95
        }
        else
        {

#line 95
            attenuation_0 = 1.0;

#line 95
        }

#line 77
        float _S3 = attenuation_0;

#line 77
        attenuation_0 = _S2;

#line 77
        spotIntensity_0 = _S3;

    }

#line 79
    entryPointParam_fragMain_0 = vec4(texColor_0.xyz * (uAmbientColor * uAmbientIntensity + (max(dot(norm_1, lightDir_0), 0.0) * uLightColor.xyz * uLightIntensity + uSpecularIntensity * pow(max(dot(norm_1, normalize(lightDir_0 + normalize(uCameraPos - v2f_fragPos_0))), 0.0), max(uShininess, 1.0)) * uLightColor.xyz * uLightIntensity + max(dot(- norm_1, lightDir_0), 0.0) * 0.15000000596046448 * uLightColor.xyz * uLightIntensity) * (attenuation_0 * spotIntensity_0)), texColor_0.w);

#line 79
    return;
}
