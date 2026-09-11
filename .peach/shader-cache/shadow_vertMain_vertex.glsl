#version 410 core



#line 3 0



#line 3

uniform mat4 uLightViewProjection;
uniform vec3 uShadowLightPos;
uniform float uShadowFarPlane;
uniform int uShadowMode;


#line 25
layout(location = 0)
out vec4 v2f_color_0;


#line 12
layout(location = 1)
out vec2 v2f_texCoords_0;


#line 12
layout(location = 2)
out float v2f_texID_0;


#line 12
layout(location = 3)
out vec3 v2f_worldPos_0;


#line 12
layout(location = 0)
in vec3 input_pos_0;


#line 12
layout(location = 2)
in vec4 input_color_0;


#line 12
layout(location = 3)
in vec2 input_texCoords_0;


#line 12
layout(location = 4)
in float input_texID_0;


#line 20
struct VSOutput_0
{
    vec4 position_0;
    vec4 color_0;
    vec2 texCoords_0;
    float texID_0;
    vec3 worldPos_0;
};

void main()
{

#line 30
    VSOutput_0 output_0;

    output_0.position_0 = (((vec4(input_pos_0, 1.0)) * (uLightViewProjection)));

#line 37
    output_0.worldPos_0 = input_pos_0;
    output_0.color_0 = input_color_0;
    output_0.texCoords_0 = input_texCoords_0;
    output_0.texID_0 = input_texID_0;

    VSOutput_0 _S1 = output_0;

#line 42
    gl_Position = output_0.position_0;

#line 42
    v2f_color_0 = _S1.color_0;

#line 42
    v2f_texCoords_0 = _S1.texCoords_0;

#line 42
    v2f_texID_0 = _S1.texID_0;

#line 42
    v2f_worldPos_0 = _S1.worldPos_0;

#line 42
    return;
}
