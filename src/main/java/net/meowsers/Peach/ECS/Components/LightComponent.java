package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.GUI.Editor;
import net.meowsers.Peach.Graphics.Light;
import net.meowsers.Peach.Graphics.Mesh;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.LightType;
import org.joml.Vector3f;

public class LightComponent extends BehaviorComponent {

    private Light light;

    @Editor private boolean isCurrent = true;
    @Editor private boolean drawDebugBall = true;

    @Editor public LightType lightType = LightType.POINT;
    @Editor public Vector3f lightPosition = new Vector3f(10.0f, 20.0f, 15.0f);
    @Editor public Vector3f lightDirection = new Vector3f(0.0f, -1.0f, 0.0f);
    @Editor public Color lightColor = Color.White;
    @Editor public float lightIntensity = 1.0f;
    @Editor public Color ambientColor = new Color(0.25f, 0.25f, 0.28f, 1.0f);
    @Editor public float ambientIntensity = 0.35f;
    @Editor public float specularIntensity = 0.5f;
    @Editor public float shininess = 32.0f;
    @Editor public float cutOff = 0.91f;
    @Editor public float outerCutOff = 0.82f;
    @Editor public float debugBallRadius = 0.5f;

    private Mesh debugSphereMesh;
    private float lastDebugBallRadius = debugBallRadius;

    private final Vector3f lastTransformPos = new Vector3f(Float.NaN);
    private final Vector3f lastFieldPos = new Vector3f(Float.NaN);
    private final Vector3f lastTransformRot = new Vector3f(Float.NaN);

    public LightComponent() {
        setLight(new Light());
    }

    public LightComponent(LightType type) {
        setLight(new Light(type));
    }

    public LightComponent(Light light) {
        setLight(light);
    }

    public LightComponent(Vector3f position) {
        setLight(new Light(position));
    }

    public LightComponent(Vector3f position, LightType type) {
        setLight(new Light(position, type));
    }

    public LightComponent(Color color, float intensity) {
        setLight(new Light(new Vector3f(10.0f, 20.0f, 15.0f), color, intensity));
    }

    public LightComponent(Color color, float intensity, LightType type) {
        setLight(new Light(new Vector3f(10.0f, 20.0f, 15.0f), color, intensity, type));
    }

    public LightComponent(Vector3f position, Color color, float intensity) {
        setLight(new Light(position, color, intensity));
    }

    public LightComponent(Vector3f position, Color color, float intensity, LightType type) {
        setLight(new Light(position, color, intensity, type));
    }

    private void ensureLight() {
        if (light == null) {
            setLight(new Light());
        }
    }

    private void pullFieldsFromLight() {
        ensureLight();

        lightType = light.getType();
        lightPosition.set(light.getPosition());
        lightDirection.set(light.getDirection());
        lightColor = light.getColor() != null ? light.getColor() : Color.White;
        lightIntensity = light.getIntensity();
        ambientColor = light.getAmbientColor() != null ? light.getAmbientColor() : new Color(0.25f, 0.25f, 0.28f, 1.0f);
        ambientIntensity = light.getAmbientIntensity();
        specularIntensity = light.getSpecularIntensity();
        shininess = light.getShininess();
        cutOff = light.getCutOff();
        outerCutOff = light.getOuterCutOff();
    }

    private void pushFieldsToLight() {
        ensureLight();

        light.setType(lightType);
        light.setPosition(lightPosition);
        light.setDirection(lightDirection);
        light.setColor(lightColor != null ? lightColor : Color.White);
        light.setIntensity(lightIntensity);
        light.setAmbientColor(ambientColor != null ? ambientColor : new Color(0.25f, 0.25f, 0.28f, 1.0f));
        light.setAmbientIntensity(ambientIntensity);
        light.setSpecularIntensity(specularIntensity);
        light.setShininess(shininess);
        light.setCutOff(cutOff);
        light.setOuterCutOff(outerCutOff);
    }

    private void syncTransform() {
        ensureLight();

        TransformComponent t = getTransform();
        if (t != null && t.position != null) {
            boolean hasLast = !Float.isNaN(lastTransformPos.x);
            if (!hasLast) {
                if (t.position.x != 0.0f || t.position.y != 0.0f || t.position.z != 0.0f) {
                    lightPosition.set(t.position);
                    light.setPosition(lightPosition);
                } else {
                    t.position.set(lightPosition);
                    light.setPosition(lightPosition);
                }
                if (t.rotation != null && (t.rotation.x != 0.0f || t.rotation.y != 0.0f || t.rotation.z != 0.0f)) {
                    float pitch = (float) Math.toRadians(t.rotation.x);
                    float yaw = (float) Math.toRadians(t.rotation.y);
                    Vector3f dir = new Vector3f(
                        (float) (Math.cos(yaw) * Math.cos(pitch)),
                        (float) Math.sin(pitch),
                        (float) (Math.sin(yaw) * Math.cos(pitch))
                    );
                    if (dir.lengthSquared() > 0.0001f) {
                        lightDirection.set(dir.normalize());
                        light.setDirection(lightDirection);
                    }
                }
            } else {
                if (!t.position.equals(lastTransformPos)) {
                    lightPosition.set(t.position);
                    light.setPosition(lightPosition);
                } else if (!lightPosition.equals(lastFieldPos)) {
                    light.setPosition(lightPosition);
                    t.position.set(lightPosition);
                }

                if (t.rotation != null && !t.rotation.equals(lastTransformRot)) {
                    float pitch = (float) Math.toRadians(t.rotation.x);
                    float yaw = (float) Math.toRadians(t.rotation.y);
                    Vector3f dir = new Vector3f(
                        (float) (Math.cos(yaw) * Math.cos(pitch)),
                        (float) Math.sin(pitch),
                        (float) (Math.sin(yaw) * Math.cos(pitch))
                    );
                    if (dir.lengthSquared() > 0.0001f) {
                        lightDirection.set(dir.normalize());
                        light.setDirection(lightDirection);
                    }
                }
            }

            lastTransformPos.set(t.position);
            lastFieldPos.set(lightPosition);
            if (t.rotation != null) {
                lastTransformRot.set(t.rotation);
            }
        }
    }

    @Override
    public void onAdded() {
        super.onAdded();
        syncTransform();

        if (isCurrent) {
            Renderer.addLight(light);
        }
    }

    @Override
    public void start() {
        super.start();
        syncTransform();

        if (isCurrent) {
            Renderer.addLight(light);
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);

        ensureLight();
        syncTransform();
        pushFieldsToLight();

        if (isCurrent && !Renderer.getLights().contains(light)) {
            Renderer.addLight(light);
        } else if (!isCurrent && Renderer.getLights().contains(light)) {
            Renderer.removeLight(light);
        }

        if (lastDebugBallRadius != debugBallRadius) {
            debugSphereMesh = null;
            lastDebugBallRadius = debugBallRadius;
        }

        if (drawDebugBall && isEnabled()) {
            Color c = lightColor != null ? lightColor : Color.White;

            if (debugSphereMesh == null) {
                debugSphereMesh = Mesh.createSphere(debugBallRadius, 16, 16, c);
            } else {
                debugSphereMesh.setColor(c);
            }

            Renderer.drawMesh(debugSphereMesh, lightPosition);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (isCurrent) {
            ensureLight();
            Renderer.addLight(light);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Renderer.removeLight(light);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Renderer.removeLight(light);
    }

    public void setCurrent() {
        setCurrent(true);
    }

    public void setCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
        ensureLight();

        if (isCurrent) {
            Renderer.addLight(light);
        } else {
            Renderer.removeLight(light);
        }
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public Light getLight() {
        ensureLight();
        pushFieldsToLight();
        return light;
    }

    public void setLight(Light light) {
        if (this.light != null) {
            Renderer.removeLight(this.light);
        }

        this.light = light != null ? light : new Light();
        pullFieldsFromLight();

        if (isCurrent) {
            Renderer.addLight(this.light);
        }
    }

    public LightType getType() {
        return lightType;
    }

    public void setType(LightType type) {
        if (type != null) {
            this.lightType = type;
        }

        ensureLight();
        light.setType(lightType);
    }

    public Vector3f getPosition() {
        return lightPosition;
    }

    public void setPosition(Vector3f pos) {
        if (pos != null) {
            lightPosition.set(pos);
        }

        ensureLight();
        light.setPosition(lightPosition);

        TransformComponent t = getTransform();
        if (t != null && t.position != null) {
            t.position.set(lightPosition);
            lastTransformPos.set(t.position);
        }
        lastFieldPos.set(lightPosition);
    }

    public Vector3f getDirection() {
        return lightDirection;
    }

    public void setDirection(Vector3f dir) {
        if (dir != null) {
            lightDirection.set(dir);
        }

        ensureLight();
        light.setDirection(lightDirection);
    }

    public Color getColor() {
        return lightColor;
    }

    public void setColor(Color color) {
        this.lightColor = color != null ? color : Color.White;

        ensureLight();
        light.setColor(lightColor);

        if (debugSphereMesh != null) {
            debugSphereMesh.setColor(lightColor);
        }
    }

    public float getIntensity() {
        return lightIntensity;
    }

    public void setIntensity(float intensity) {
        this.lightIntensity = intensity;

        ensureLight();
        light.setIntensity(lightIntensity);
    }

    public Color getAmbientColor() {
        return ambientColor;
    }

    public void setAmbientColor(Color ambientColor) {
        this.ambientColor = ambientColor != null ? ambientColor : new Color(0.25f, 0.25f, 0.28f, 1.0f);

        ensureLight();
        light.setAmbientColor(this.ambientColor);
    }

    public float getAmbientIntensity() {
        return ambientIntensity;
    }

    public void setAmbientIntensity(float ambientIntensity) {
        this.ambientIntensity = ambientIntensity;

        ensureLight();
        light.setAmbientIntensity(this.ambientIntensity);
    }

    public float getSpecularIntensity() {
        return specularIntensity;
    }

    public void setSpecularIntensity(float specularIntensity) {
        this.specularIntensity = specularIntensity;

        ensureLight();
        light.setSpecularIntensity(this.specularIntensity);
    }

    public float getShininess() {
        return shininess;
    }

    public void setShininess(float shininess) {
        this.shininess = shininess;

        ensureLight();
        light.setShininess(this.shininess);
    }

    public float getCutOff() {
        return cutOff;
    }

    public void setCutOff(float cutOff) {
        this.cutOff = cutOff;

        ensureLight();
        light.setCutOff(this.cutOff);
    }

    public void setCutOffDegrees(float degrees) {
        this.cutOff = (float) Math.cos(Math.toRadians(degrees));

        ensureLight();
        light.setCutOff(cutOff);
    }

    public float getOuterCutOff() {
        return outerCutOff;
    }

    public void setOuterCutOff(float outerCutOff) {
        this.outerCutOff = outerCutOff;

        ensureLight();
        light.setOuterCutOff(this.outerCutOff);
    }

    public void setOuterCutOffDegrees(float degrees) {
        this.outerCutOff = (float) Math.cos(Math.toRadians(degrees));

        ensureLight();
        light.setOuterCutOff(outerCutOff);
    }

    public boolean isDirectional() {
        return lightType == LightType.DIRECTIONAL;
    }

    public void setDirectional(boolean directional) {
        setType(directional ? LightType.DIRECTIONAL : LightType.POINT);
    }

    public boolean isPoint() {
        return lightType == LightType.POINT;
    }

    public boolean isSpot() {
        return lightType == LightType.SPOT;
    }

    public void drawDebugBall(boolean value) {
        this.drawDebugBall = value;
    }

    public void setDrawDebugBall(boolean value) {
        this.drawDebugBall = value;
    }

    public boolean isDrawDebugBall() {
        return drawDebugBall;
    }

    public boolean getDrawDebugBall() {
        return drawDebugBall;
    }

    public float getDebugBallRadius() {
        return debugBallRadius;
    }

    public void setDebugBallRadius(float debugBallRadius) {
        this.debugBallRadius = debugBallRadius;
        this.lastDebugBallRadius = debugBallRadius;
        this.debugSphereMesh = null;
    }
}
