package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.LightType;
import org.joml.Vector3f;

public class Light {
    private LightType type = LightType.POINT;
    private final Vector3f position = new Vector3f(10.0f, 20.0f, 15.0f);
    private final Vector3f direction = new Vector3f(0.0f, -1.0f, 0.0f);
    private Color color = Color.White;
    private float intensity = 1.0f;
    private Color ambientColor = new Color(0.25f, 0.25f, 0.28f, 1.0f);
    private float ambientIntensity = 0.35f;
    private float specularIntensity = 0.5f;
    private float shininess = 32.0f;
    private float cutOff = 0.91f;        // cos(24.5 deg) ~ 0.91
    private float outerCutOff = 0.82f;   // cos(35 deg) ~ 0.82

    public Light() {
    }

    public Light(LightType type) {
        if (type != null) {
            this.type = type;
        }
    }

    public Light(Vector3f position) {
        if (position != null) {
            this.position.set(position);
        }
    }

    public Light(Vector3f position, LightType type) {
        if (position != null) {
            this.position.set(position);
        }
        if (type != null) {
            this.type = type;
        }
    }

    public Light(Vector3f position, Color color) {
        if (position != null) {
            this.position.set(position);
        }
        if (color != null) {
            this.color = color;
        }
    }

    public Light(Vector3f position, Color color, float intensity) {
        if (position != null) {
            this.position.set(position);
        }
        if (color != null) {
            this.color = color;
        }
        this.intensity = intensity;
    }

    public Light(Vector3f position, Color color, float intensity, LightType type) {
        this(position, color, intensity);
        if (type != null) {
            this.type = type;
        }
    }

    public LightType getType() {
        return type;
    }

    public void setType(LightType type) {
        if (type != null) {
            this.type = type;
        }
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f pos) {
        if (pos != null) {
            this.position.set(pos);
        }
    }

    public Vector3f getDirection() {
        return direction;
    }

    public void setDirection(Vector3f dir) {
        if (dir != null) {
            this.direction.set(dir);
        }
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        if (color != null) {
            this.color = color;
        }
    }

    public float getIntensity() {
        return intensity;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public Color getAmbientColor() {
        return ambientColor;
    }

    public void setAmbientColor(Color ambientColor) {
        if (ambientColor != null) {
            this.ambientColor = ambientColor;
        }
    }

    public float getAmbientIntensity() {
        return ambientIntensity;
    }

    public void setAmbientIntensity(float ambientIntensity) {
        this.ambientIntensity = ambientIntensity;
    }

    public float getSpecularIntensity() {
        return specularIntensity;
    }

    public void setSpecularIntensity(float specularIntensity) {
        this.specularIntensity = specularIntensity;
    }

    public float getShininess() {
        return shininess;
    }

    public void setShininess(float shininess) {
        this.shininess = shininess;
    }

    public float getCutOff() {
        return cutOff;
    }

    public void setCutOff(float cutOff) {
        this.cutOff = cutOff;
    }

    public void setCutOffDegrees(float degrees) {
        this.cutOff = (float) Math.cos(Math.toRadians(degrees));
    }

    public float getOuterCutOff() {
        return outerCutOff;
    }

    public void setOuterCutOff(float outerCutOff) {
        this.outerCutOff = outerCutOff;
    }

    public void setOuterCutOffDegrees(float degrees) {
        this.outerCutOff = (float) Math.cos(Math.toRadians(degrees));
    }

    public boolean isDirectional() {
        return type == LightType.DIRECTIONAL;
    }

    public void setDirectional(boolean directional) {
        this.type = directional ? LightType.DIRECTIONAL : LightType.POINT;
    }

    public boolean isPoint() {
        return type == LightType.POINT;
    }

    public boolean isSpot() {
        return type == LightType.SPOT;
    }
}
