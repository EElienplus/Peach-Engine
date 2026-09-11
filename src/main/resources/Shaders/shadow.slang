#define MAX_MATERIAL_TEXTURES 15

cbuffer ShadowUniforms {
    float4x4 uLightViewProjection;
    float3   uShadowLightPos;
    float    uShadowFarPlane;
    int      uShadowMode;
};

Sampler2D uTextures[MAX_MATERIAL_TEXTURES];

struct VSInput {
    [[vk::location(0)]] float3 pos       : POSITION;
    [[vk::location(1)]] float3 normal    : NORMAL;
    [[vk::location(2)]] float4 color     : COLOR;
    [[vk::location(3)]] float2 texCoords : TEXCOORD0;
    [[vk::location(4)]] float  texID     : TEXCOORD1;
};

struct VSOutput {
    float4 position : SV_Position;
    float4 color    : COLOR;
    float2 texCoords : TEXCOORD0;
    float texID      : TEXCOORD1;
    float3 worldPos : TEXCOORD2;
};

[shader("vertex")]
VSOutput vertMain(VSInput input) {
    VSOutput output;

    output.position = mul(
        uLightViewProjection,
        float4(input.pos, 1.0)
    );

    output.worldPos = input.pos;
    output.color = input.color;
    output.texCoords = input.texCoords;
    output.texID = input.texID;

    return output;
}

[shader("fragment")]
float4 fragMain(VSOutput input) : SV_Target {
    int id = clamp((int)input.texID, 0, 14);

    float4 alphaSample =
        input.color *
        uTextures[id].Sample(input.texCoords);

    // Transparent image pixels do not become opaque shadow casters.
    if (alphaSample.a <= 0.001) {
        discard;
    }

    float value;

    if (uShadowMode == 3) {
        float3 delta =
            input.worldPos - uShadowLightPos;

        float distanceToLight =
            length(delta);

        value =
            distanceToLight
            / max(uShadowFarPlane, 0.0001);
    } else {
        float ndcDepth =
            input.position.z /
            input.position.w;

        value =
            ndcDepth * 0.5 + 0.5;
    }

    return float4(
        value,
        0.0,
        0.0,
        1.0
    );
}
