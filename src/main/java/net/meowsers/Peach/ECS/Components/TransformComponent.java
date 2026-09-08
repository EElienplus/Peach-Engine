package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.GUI.Editor;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class TransformComponent extends Component {
    @Editor public Vector3f position;
    @Editor public Vector3f rotation;
    @Editor public Vector3f scale;

    public TransformComponent(Vector3f position, Vector3f rotation, Vector3f scale) {
        this.position = position != null ? position : new Vector3f(0);
        this.rotation = rotation != null ? rotation : new Vector3f(0);
        this.scale = scale != null ? scale : new Vector3f(1);
    }
    public TransformComponent(Vector3f position, Vector2f rotation, Vector3f scale) {
        this.position = position != null ? position : new Vector3f(0);
        this.rotation = rotation != null ? new Vector3f(rotation.x, rotation.y, 0.0f) : new Vector3f(0.0f);
        this.scale = scale != null ? scale : new Vector3f(1);
    }
    public TransformComponent(Vector3f position) {
        this(position, new Vector3f(0), new Vector3f(1));
    }
    public TransformComponent() {
        this(new Vector3f(0), new Vector3f(0), new Vector3f(1));
    }

    public void set(TransformComponent transformComponent) {
        if (transformComponent == null) return;
        this.position = transformComponent.position;
        this.rotation = transformComponent.rotation;
        this.scale = transformComponent.scale;
    }

    public TransformComponent setPosition(Vector3f pos) {
        if (pos != null) {
            this.position.set(pos);
        }
        return this;
    }
    public TransformComponent setRotation(Vector3f rot) {
        if (rot != null) {
            this.rotation.set(rot);
        }
        return this;
    }
    public TransformComponent setScale(Vector3f sca) {
        if (sca != null) {
            this.scale.set(sca);
        }
        return this;
    }
    public TransformComponent setScale(float uniformScale) {
        this.scale.set(uniformScale, uniformScale, uniformScale);
        return this;
    }
    public TransformComponent translate(Vector3f offset) {
        if (offset != null) {
            this.position.add(offset);
        }
        return this;
    }
    public TransformComponent rotate(Vector3f angles) {
        if (angles != null) {
            this.rotation.add(angles);
        }
        return this;
    }
}
