#version 410 core



#line 10 0

uniform sampler2D  uTextures[15];


#line 3



#line 3

uniform mat4 uLightViewProjection;
uniform vec3 uShadowLightPos;
uniform float uShadowFarPlane;
uniform int uShadowMode;


#line 25
layout(location = 0)
out vec4 entryPointParam_fragMain_0;


#line 25
layout(location = 0)
in vec4 v2f_color_0;


#line 25
layout(location = 1)
in vec2 v2f_texCoords_0;


#line 25
layout(location = 2)
in float v2f_texID_0;


#line 25
layout(location = 3)
in vec3 v2f_worldPos_0;


#line 46
void main()
{

#line 54
    if(((v2f_color_0 * (texture((uTextures[clamp(int(v2f_texID_0), 0, 14)]), (v2f_texCoords_0)))).w) <= 0.00100000004749745)
    {

#line 55
        discard;

#line 54
    }

#line 54
    float value_0;

#line 60
    if((uShadowMode) == 3)
    {

#line 60
        value_0 = length(v2f_worldPos_0 - uShadowLightPos) / max(uShadowFarPlane, 0.00009999999747379);

#line 60
    }
    else
    {

#line 60
        value_0 = gl_FragCoord.z / gl_FragCoord.w * 0.5 + 0.5;

#line 60
    }

#line 60
    entryPointParam_fragMain_0 = vec4(value_0, 0.0, 0.0, 1.0);

#line 60
    return;
}
