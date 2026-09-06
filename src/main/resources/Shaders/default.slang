cbuffer Uniforms {
    float4x4 uProjection;
    float4x4 uView;
    float3   uLightPos;
    float    uLightIntensity;
    float4   uLightColor;
    float3   uAmbientColor;
    float    uAmbientIntensity;
    float3   uCameraPos;
    float    uSpecularIntensity;
    float    uShininess;
    int      uLightType;
    int      uIsDirectional;
    int      uUseLighting;
    int      uNormalCulling;
    float3   uLightDir;
    float    uCutOff;
    float    uOuterCutOff;
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

    if (uUseLighting == 0) {
        return texColor;
    }

    // Light direction and attenuation / spot effect
    float3 lightDir;
    float attenuation = 1.0;
    float spotIntensity = 1.0;

    if (uLightType == 1 || uIsDirectional != 0) {
        // Directional Light
        if (dot(uLightDir, uLightDir) > 0.0001) {
            lightDir = normalize(-uLightDir);
        } else {
            lightDir = normalize(uLightPos);
        }
        attenuation = 1.0;
    } else {
        // Point or Spot Light
        float3 toLight = uLightPos - input.fragPos;
        float dist = length(toLight);
        lightDir = (dist > 0.0001) ? (toLight / dist) : float3(0.0, 1.0, 0.0);
        // Distance attenuation
        attenuation = 1.0 / (1.0 + 0.0014 * dist + 0.000007 * dist * dist);

        if (uLightType == 2) {
            // Spot Light
            float3 spotDir = normalize(-uLightDir);
            float theta = dot(lightDir, spotDir);
            float epsilon = max(uCutOff - uOuterCutOff, 0.0001);
            spotIntensity = clamp((theta - uOuterCutOff) / epsilon, 0.0, 1.0);
        }
    }

    // Ambient
    float3 ambient = uAmbientColor * uAmbientIntensity;

    // Diffuse
    float diff = max(dot(norm, lightDir), 0.0);
    float3 diffuse = diff * uLightColor.rgb * uLightIntensity;

    // Specular (Blinn-Phong)
    float3 viewDir = normalize(uCameraPos - input.fragPos);
    float3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(norm, halfwayDir), 0.0), max(uShininess, 1.0));
    float3 specular = uSpecularIntensity * spec * uLightColor.rgb * uLightIntensity;

    // Subtle backfill diffuse for softer shadows / ambient wrap
    float backDiff = max(dot(-norm, lightDir), 0.0) * 0.15;
    float3 backLighting = backDiff * uLightColor.rgb * uLightIntensity;

    float3 lighting = ambient + (diffuse + specular + backLighting) * (attenuation * spotIntensity);

    return float4(texColor.rgb * lighting, texColor.a);
}
