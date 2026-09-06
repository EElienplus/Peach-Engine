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


#line 37
layout(location = 0)
out vec4 v2f_color_0;


#line 23
layout(location = 1)
out vec2 v2f_texCoords_0;


#line 23
layout(location = 2)
out float v2f_texID_0;


#line 23
layout(location = 3)
out vec3 v2f_fragPos_0;


#line 23
layout(location = 4)
out vec3 v2f_normal_0;


#line 23
layout(location = 0)
in vec3 input_pos_0;


#line 23
layout(location = 1)
in vec3 input_normal_0;


#line 23
layout(location = 2)
in vec4 input_color_0;


#line 23
layout(location = 3)
in vec2 input_texCoords_0;


#line 23
layout(location = 4)
in float input_texID_0;


#line 31
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

#line 42
    VSOutput_0 output_0;
    output_0.color_0 = input_color_0;
    output_0.texCoords_0 = input_texCoords_0;
    output_0.texID_0 = input_texID_0;
    output_0.fragPos_0 = input_pos_0;
    output_0.normal_0 = input_normal_0;

    output_0.position_0 = ((((((vec4(input_pos_0, 1.0)) * (uView)))) * (uProjection)));
    VSOutput_0 _S1 = output_0;

#line 50
    gl_Position = output_0.position_0;

#line 50
    v2f_color_0 = _S1.color_0;

#line 50
    v2f_texCoords_0 = _S1.texCoords_0;

#line 50
    v2f_texID_0 = _S1.texID_0;

#line 50
    v2f_fragPos_0 = _S1.fragPos_0;

#line 50
    v2f_normal_0 = _S1.normal_0;

#line 50
    return;
}
