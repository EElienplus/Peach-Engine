#define MAX_LIGHTS 16

cbuffer Uniforms {
    float4x4 uProjection;
    float4x4 uView;

    float3   uCameraPos;
    int      uNumLights;

    int      uUseLighting;
    int      uNormalCulling;

    int      uShadowsEnabled;
    int      uShadowMode;

    float4x4 uShadowViewProjection;
    float4x4 uPointShadowMatrices[6];
    float3   uShadowLightPos;
    float    uShadowBias;
    float    uShadowNormalBias;
    float    uShadowTexelSize;
    float    uShadowFarPlane;
    float    uShadowNearPlane;

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

// Unit 15 is reserved for the shadow map.
Sampler2D uTextures[15];
Sampler2DArray uShadowMap;

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

    float normalSq = dot(input.normal, input.normal);
    output.normal = normalSq > 0.000001
        ? input.normal * rsqrt(normalSq)
        : float3(0.0, 0.0, 1.0);

    output.position = mul(
        uProjection,
        mul(uView, float4(input.pos, 1.0))
    );

    return output;
}

float samplePointShadow(
    float2 uv,
    int layer,
    float compareDepth
) {
    float stored =
        uShadowMap.Sample(
            float3(uv, layer)
        ).r;

    return stored > compareDepth ? 1.0 : 0.0;
}

float visibility2D(
    float3 worldPos,
    float3 normal,
    float3 lightDirection
) {
    float4 clip = mul(
        uShadowViewProjection,
        float4(worldPos, 1.0)
    );

    if (clip.w <= 0.000001) {
        return 1.0;
    }

    float3 ndc = clip.xyz / clip.w;
    float2 uv = ndc.xy * 0.5 + 0.5;

    if (uv.x < 0.0 || uv.x > 1.0 ||
        uv.y < 0.0 || uv.y > 1.0 ||
        ndc.z < -1.0 || ndc.z > 1.0) {
        return 1.0;
    }

    float receiverDepth =
        ndc.z * 0.5 + 0.5;

    float nDotL =
        max(dot(normal, lightDirection), 0.0);

    float bias =
        uShadowBias
        + uShadowNormalBias * (1.0 - nDotL);

    float compareDepth =
        receiverDepth - bias;

    float2 texel =
        float2(uShadowTexelSize, uShadowTexelSize);

    float lit = 0.0;

    lit += uShadowMap.Sample(float3(uv, 0)).r > compareDepth ? 1.0 : 0.0;
    lit += uShadowMap.Sample(float3(uv + texel * float2(1, 0), 0)).r > compareDepth ? 1.0 : 0.0;
    lit += uShadowMap.Sample(float3(uv + texel * float2(-1, 0), 0)).r > compareDepth ? 1.0 : 0.0;
    lit += uShadowMap.Sample(float3(uv + texel * float2(0, 1), 0)).r > compareDepth ? 1.0 : 0.0;
    lit += uShadowMap.Sample(float3(uv + texel * float2(0, -1), 0)).r > compareDepth ? 1.0 : 0.0;

    return lit * 0.2;
}

float visibilityPoint(
    float3 worldPos,
    float3 normal,
    float3 lightPos,
    float3 lightDirection
) {
    float3 fromLight =
        worldPos - lightPos;

    float distanceSq =
        dot(fromLight, fromLight);

    if (distanceSq <= 0.000001) {
        return 1.0;
    }

    float distanceToLight =
        sqrt(distanceSq);

    if (distanceToLight >= uShadowFarPlane) {
        return 1.0;
    }

    float3 direction =
        fromLight * rsqrt(distanceSq);

    float3 a = abs(direction);

    int layer;

    if (a.x >= a.y && a.x >= a.z) {
        layer = direction.x >= 0.0 ? 0 : 1;
    } else if (a.y >= a.x && a.y >= a.z) {
        layer = direction.y >= 0.0 ? 2 : 3;
    } else {
        layer = direction.z >= 0.0 ? 4 : 5;
    }

    float4 clip = mul(
        uPointShadowMatrices[layer],
        float4(worldPos, 1.0)
    );

    if (clip.w <= 0.000001) {
        return 1.0;
    }

    float3 ndc = clip.xyz / clip.w;
    float2 uv = ndc.xy * 0.5 + 0.5;

    if (uv.x < 0.0 || uv.x > 1.0 ||
        uv.y < 0.0 || uv.y > 1.0 ||
        ndc.z < -1.0 || ndc.z > 1.0) {
        return 1.0;
    }

    // Shadow pass writes linear distance / farPlane.
    float receiverDepth =
        distanceToLight
        / max(uShadowFarPlane, 0.0001);

    float nDotL =
        max(dot(normal, lightDirection), 0.0);

    float bias =
        uShadowBias
        / max(uShadowFarPlane, 0.0001)
        + uShadowNormalBias
        * (1.0 - nDotL)
        / max(uShadowFarPlane, 0.0001);

    float compareDepth =
        receiverDepth - bias;

    float2 texel =
        float2(uShadowTexelSize, uShadowTexelSize);

    float lit = 0.0;

    lit += samplePointShadow(uv, layer, compareDepth);
    lit += samplePointShadow(uv + texel * float2(1, 0), layer, compareDepth);
    lit += samplePointShadow(uv + texel * float2(-1, 0), layer, compareDepth);
    lit += samplePointShadow(uv + texel * float2(0, 1), layer, compareDepth);
    lit += samplePointShadow(uv + texel * float2(0, -1), layer, compareDepth);

    return lit * 0.2;
}

[shader("fragment")]
float4 fragMain(VSOutput input) : SV_Target {
    int id = clamp((int)input.texID, 0, 14);

    float4 texColor =
        input.color *
        uTextures[id].Sample(input.texCoords);

    // Correct alpha handling: don't draw invisible texture pixels and preserve
    // the source image's alpha for the normal GL blend pass.
    if (texColor.a <= 0.001) {
        discard;
    }

    if (uUseLighting == 0 || uNumLights <= 0) {
        return texColor;
    }

    float3 norm = input.normal;
    float normSq = dot(norm, norm);
    norm = normSq > 0.000001
        ? norm * rsqrt(normSq)
        : float3(0.0, 0.0, 1.0);

    float3 toCamera =
        uCameraPos - input.fragPos;

    float cameraSq = dot(toCamera, toCamera);

    float3 viewDir = cameraSq > 0.000001
        ? toCamera * rsqrt(cameraSq)
        : float3(0.0, 0.0, 1.0);

    if (uNormalCulling != 0 &&
        dot(norm, viewDir) < 0.0) {
        discard;
    }

    float3 lighting =
        uAmbientColor * uAmbientIntensity;

    int count = min(uNumLights, MAX_LIGHTS);

    for (int i = 0; i < count; ++i) {
        float3 lightDir;
        float factor = 1.0;

        bool directional =
            uLightType[i] == 1 ||
            uIsDirectional[i] != 0;

        if (directional) {
            lightDir = -uLightDir[i];
        } else {
            float3 toLight =
                uLightPos[i] - input.fragPos;

            float distSq = dot(toLight, toLight);

            if (distSq <= 0.000001) {
                continue;
            }

            float invDist = rsqrt(distSq);
            lightDir = toLight * invDist;

            float dist = sqrt(distSq);

            factor = 1.0 / (
                1.0
                + 0.0014 * dist
                + 0.000007 * distSq
            );

            if (uLightType[i] == 2) {
                float theta =
                    dot(lightDir, -uLightDir[i]);

                float epsilon = max(
                    uCutOff[i] - uOuterCutOff[i],
                    0.0001
                );

                factor *= clamp(
                    (theta - uOuterCutOff[i]) / epsilon,
                    0.0,
                    1.0
                );
            }
        }

        if (factor <= 0.0001) {
            continue;
        }

        float visibility = 1.0;

        // First active light is the shadow-casting light.
        if (uShadowsEnabled != 0 && i == 0) {
            visibility = uShadowMode == 3
                ? visibilityPoint(
                    input.fragPos,
                    norm,
                    uLightPos[i],
                    lightDir
                  )
                : visibility2D(
                    input.fragPos,
                    norm,
                    lightDir
                  );
        }

        float3 lightColor =
            uLightColor[i].rgb
            * uLightIntensity[i]
            * factor;

        float diffuseAmount =
            max(dot(norm, lightDir), 0.0);

        lighting +=
            diffuseAmount
            * lightColor
            * visibility;

        if (diffuseAmount > 0.0 &&
            uSpecularIntensity[i] > 0.0001) {

            float3 halfway =
                lightDir + viewDir;

            float halfwaySq =
                dot(halfway, halfway);

            if (halfwaySq > 0.000001) {
                halfway *= rsqrt(halfwaySq);

                float spec =
                    max(dot(norm, halfway), 0.0);

                spec = pow(
                    spec,
                    max(uShininess[i], 1.0)
                );

                lighting +=
                    uSpecularIntensity[i]
                    * spec
                    * lightColor
                    * visibility;
            }
        }

        lighting +=
            max(dot(-norm, lightDir), 0.0)
            * 0.15
            * lightColor
            * visibility;
    }

    return float4(
        texColor.rgb * lighting,
        texColor.a
    );
}
