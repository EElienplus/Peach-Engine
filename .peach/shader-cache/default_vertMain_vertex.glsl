#version 410 core



#line 3 0



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


#line 58
layout(location = 0)
out vec4 v2f_color_0;


#line 44
layout(location = 1)
out vec2 v2f_texCoords_0;


#line 44
layout(location = 2)
out float v2f_texID_0;


#line 44
layout(location = 3)
out vec3 v2f_fragPos_0;


#line 44
layout(location = 4)
out vec3 v2f_normal_0;


#line 44
layout(location = 0)
in vec3 input_pos_0;


#line 44
layout(location = 1)
in vec3 input_normal_0;


#line 44
layout(location = 2)
in vec4 input_color_0;


#line 44
layout(location = 3)
in vec2 input_texCoords_0;


#line 44
layout(location = 4)
in float input_texID_0;


#line 52
struct VSOutput_0
{
    vec4 position_0;
    vec4 color_0;
    vec2 texCoords_0;
    float texID_0;
    vec3 fragPos_0;
    vec3 normal_0;
};

void main()
{

#line 63
    VSOutput_0 output_0;

    output_0.color_0 = input_color_0;
    output_0.texCoords_0 = input_texCoords_0;
    output_0.texID_0 = input_texID_0;
    output_0.fragPos_0 = input_pos_0;

    float normalSq_0 = dot(input_normal_0, input_normal_0);

#line 70
    vec3 _S1;

    if(normalSq_0 > 9.99999997475242708e-07)
    {

#line 72
        _S1 = input_normal_0 * (inversesqrt((normalSq_0)));

#line 72
    }
    else
    {

#line 72
        _S1 = vec3(0.0, 0.0, 1.0);

#line 72
    }

#line 71
    output_0.normal_0 = _S1;



    output_0.position_0 = ((((((vec4(input_pos_0, 1.0)) * (uView)))) * (uProjection)));

#line 80
    VSOutput_0 _S2 = output_0;

#line 80
    gl_Position = output_0.position_0;

#line 80
    v2f_color_0 = _S2.color_0;

#line 80
    v2f_texCoords_0 = _S2.texCoords_0;

#line 80
    v2f_texID_0 = _S2.texID_0;

#line 80
    v2f_fragPos_0 = _S2.fragPos_0;

#line 80
    v2f_normal_0 = _S2.normal_0;

#line 80
    return;
}
