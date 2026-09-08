#define MAX_LIGHTS 16

cbuffer Uniforms {
    float4x4 uProjection;
    float4x4 uView;
    float3   uCameraPos;
    int      uNumLights;
    int      uUseLighting;
    int      uNormalCulling;
    float3   uAmbientColor;
    float    uAmbientIntensity;
    float3   uLightPos[MAX_LIGHTS];
    float    uLightIntensity[MAX_LIGHTS];
    float4   uLightColor[MAX_LIGHTS];
    float3   uLightDir[MAX_LIGHTS];
    int      uLightType[MAX_LIGHTS];
    int      uIsDirectional[MAX_LIGHTS];
    float    uCutOff[MAX_LIGHTS];
    float    uOuterCutOff[MAX_LIGHTS];
    float    uSpecularIntensity[MAX_LIGHTS];
    float    uShininess[MAX_LIGHTS];
};

Sampler2D uTextures[16];

struct VSInput {
    [[vk::location(0)]] float3 pos       : POSITION;
    [[vk::location(1)]] float3 normal    : NORMAL;
    [[vk::location(2)]] float4 color     : COLOR;
    [[vk::location(3)]] float2 texCoords : TEXCOORD0;
    [[vk::location(4)]] float  texID     : TEXCOORD1;
};

struct VSOutput {
    float4 position   : SV_Position;
    float4 color      : COLOR;
    float2 texCoords  : TEXCOORD0;
    float  texID      : TEXCOORD1;
    float3 fragPos    : TEXCOORD2;
    float3 normal     : NORMAL;
};

[shader("vertex")]
VSOutput vertMain(VSInput input) {
    VSOutput output;
    output.color = input.color;
    output.texCoords = input.texCoords;
    output.texID = input.texID;
    output.fragPos = input.pos;
    output.normal = input.normal;

    output.position = mul(uProjection, mul(uView, float4(input.pos, 1.0)));
    return output;
}

[shader("fragment")]
float4 fragMain(VSOutput input) : SV_Target {
    float3 norm = normalize(input.normal);
    if (dot(norm, norm) < 0.0001) {
        norm = float3(0.0, 0.0, 1.0);
    }

    if (uNormalCulling != 0) {
        float3 viewDir = normalize(uCameraPos - input.fragPos);
        if (dot(norm, viewDir) < 0.0) {
            discard;
        }
    }

    int id = clamp(int(input.texID), 0, 15);
    float4 texColor = input.color * uTextures[id].Sample(input.texCoords);

    if (uUseLighting == 0 || uNumLights <= 0) {
        return texColor;
    }

    float3 viewDir = normalize(uCameraPos - input.fragPos);
    float3 ambient = uAmbientColor * uAmbientIntensity;
    float3 totalDiffuse = float3(0.0);
    float3 totalSpecular = float3(0.0);
    float3 totalBack = float3(0.0);

    int count = min(uNumLights, MAX_LIGHTS);
    for (int i = 0; i < count; i++) {
        float3 lightDir;
        float attenuation = 1.0;
        float spotIntensity = 1.0;

        if (uLightType[i] == 1 || uIsDirectional[i] != 0) {
            // Directional Light
            if (dot(uLightDir[i], uLightDir[i]) > 0.0001) {
                lightDir = normalize(-uLightDir[i]);
            } else {
                lightDir = normalize(uLightPos[i]);
            }
            attenuation = 1.0;
        } else {
            // Point or Spot Light
            float3 toLight = uLightPos[i] - input.fragPos;
            float dist = length(toLight);
            lightDir = (dist > 0.0001) ? (toLight / dist) : float3(0.0, 1.0, 0.0);
            attenuation = 1.0 / (1.0 + 0.0014 * dist + 0.000007 * dist * dist);

            if (uLightType[i] == 2) {
                // Spot Light
                float3 spotDir = normalize(-uLightDir[i]);
                float theta = dot(lightDir, spotDir);
                float epsilon = max(uCutOff[i] - uOuterCutOff[i], 0.0001);
                spotIntensity = clamp((theta - uOuterCutOff[i]) / epsilon, 0.0, 1.0);
            }
        }

        float factor = attenuation * spotIntensity;
        float diff = max(dot(norm, lightDir), 0.0);
        totalDiffuse += diff * uLightColor[i].rgb * uLightIntensity[i] * factor;

        float3 halfwayDir = normalize(lightDir + viewDir);
        float spec = pow(max(dot(norm, halfwayDir), 0.0), max(uShininess[i], 1.0));
        totalSpecular += uSpecularIntensity[i] * spec * uLightColor[i].rgb * uLightIntensity[i] * factor;

        float backDiff = max(dot(-norm, lightDir), 0.0) * 0.15;
        totalBack += backDiff * uLightColor[i].rgb * uLightIntensity[i] * factor;
    }

    float3 lighting = ambient + totalDiffuse + totalSpecular + totalBack;
    return float4(texColor.rgb * lighting, texColor.a);
}
