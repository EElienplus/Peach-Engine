package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.Graphics.Light;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.LightType;
import org.joml.Vector3f;

public class LightComponent extends Component {

    private Light light;
    private boolean isCurrent = true;

    public LightComponent() {
        this.light = new Light();
    }

    public LightComponent(LightType type) {
        this.light = new Light(type);
    }

    public LightComponent(Light light) {
        this.light = light != null ? light : new Light();
    }

    public LightComponent(Vector3f position) {
        this.light = new Light(position);
    }

    public LightComponent(Vector3f position, LightType type) {
        this.light = new Light(position, type);
    }

    public LightComponent(Color color, float intensity) {
        this.light = new Light(new Vector3f(10.0f, 20.0f, 15.0f), color, intensity);
    }

    public LightComponent(Color color, float intensity, LightType type) {
        this.light = new Light(new Vector3f(10.0f, 20.0f, 15.0f), color, intensity, type);
    }

    public LightComponent(Vector3f position, Color color, float intensity) {
        this.light = new Light(position, color, intensity);
    }

    public LightComponent(Vector3f position, Color color, float intensity, LightType type) {
        this.light = new Light(position, color, intensity, type);
    }

    @Override
    public void onAdded() {
        super.onAdded();
        if (light == null) {
            light = new Light();
        }
        if (transform != null && transform.position != null) {
            if (transform.position.x != 0.0f || transform.position.y != 0.0f || transform.position.z != 0.0f) {
                light.setPosition(transform.position);
            } else {
                transform.position.set(light.getPosition());
            }
        }
    }

    @Override
    public void start() {
        super.start();
        if (light == null) {
            light = new Light();
        }
        if (transform != null && transform.position != null) {
            if (transform.position.x != 0.0f || transform.position.y != 0.0f || transform.position.z != 0.0f) {
                light.setPosition(transform.position);
            } else {
                transform.position.set(light.getPosition());
            }
        }
        if (isCurrent || Renderer.getLight() == null) {
            Renderer.setLight(light);
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (light == null) {
            light = new Light();
        }
        if (transform != null && transform.position != null) {
            transform.position.set(light.getPosition());
        }
        if (isCurrent && Renderer.getLight() != light) {
            Renderer.setLight(light);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (isCurrent) {
            Renderer.setLight(light);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (Renderer.getLight() == light) {
            Renderer.setLight(null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Renderer.getLight() == light) {
            Renderer.setLight(null);
        }
    }

    public void setCurrent() {
        this.isCurrent = true;
        if (light == null) {
            light = new Light();
        }
        Renderer.setLight(light);
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public Light getLight() {
        return light;
    }

    public void setLight(Light light) {
        this.light = light != null ? light : new Light();
        if (isCurrent) {
            Renderer.setLight(this.light);
        }
    }

    public LightType getType() {
        return light.getType();
    }

    public void setType(LightType type) {
        light.setType(type);
    }

    public Vector3f getPosition() {
        return light.getPosition();
    }

    public void setPosition(Vector3f pos) {
        light.setPosition(pos);
        if (transform != null && transform.position != null) {
            transform.position.set(light.getPosition());
        }
    }

    public Vector3f getDirection() {
        return light.getDirection();
    }

    public void setDirection(Vector3f dir) {
        light.setDirection(dir);
    }

    public Color getColor() {
        return light.getColor();
    }

    public void setColor(Color color) {
        light.setColor(color);
    }

    public float getIntensity() {
        return light.getIntensity();
    }

    public void setIntensity(float intensity) {
        light.setIntensity(intensity);
    }

    public Color getAmbientColor() {
        return light.getAmbientColor();
    }

    public void setAmbientColor(Color ambientColor) {
        light.setAmbientColor(ambientColor);
    }

    public float getAmbientIntensity() {
        return light.getAmbientIntensity();
    }

    public void setAmbientIntensity(float ambientIntensity) {
        light.setAmbientIntensity(ambientIntensity);
    }

    public float getSpecularIntensity() {
        return light.getSpecularIntensity();
    }

    public void setSpecularIntensity(float specularIntensity) {
        light.setSpecularIntensity(specularIntensity);
    }

    public float getShininess() {
        return light.getShininess();
    }

    public void setShininess(float shininess) {
        light.setShininess(shininess);
    }

    public float getCutOff() {
        return light.getCutOff();
    }

    public void setCutOff(float cutOff) {
        light.setCutOff(cutOff);
    }

    public void setCutOffDegrees(float degrees) {
        light.setCutOffDegrees(degrees);
    }

    public float getOuterCutOff() {
        return light.getOuterCutOff();
    }

    public void setOuterCutOff(float outerCutOff) {
        light.setOuterCutOff(outerCutOff);
    }

    public void setOuterCutOffDegrees(float degrees) {
        light.setOuterCutOffDegrees(degrees);
    }

    public boolean isDirectional() {
        return light.isDirectional();
    }

    public void setDirectional(boolean directional) {
        light.setDirectional(directional);
    }

    public boolean isPoint() {
        return light.isPoint();
    }

    public boolean isSpot() {
        return light.isSpot();
    }
}
