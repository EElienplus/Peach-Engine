package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GUI.Editor;

public class CubeColliderComponent extends BehaviorComponent {

    @Editor private String message = "Not Working yet!.";
    @Editor private String otherGameObjectName = "camera";
    @Editor private GameObject otherGameObject = null;
    @Editor private boolean colliding = false;

    @Override
    public void start() {
        super.start();
        resolveOtherGameObject();
    }

    @Override
    public void update(float dt) {
        super.update(dt);

        if (otherGameObject == null && otherGameObjectName != null && !otherGameObjectName.isBlank()) {
            resolveOtherGameObject();
        }

        colliding = isColliding(otherGameObject);
    }

    private void resolveOtherGameObject() {
        if (otherGameObjectName != null && !otherGameObjectName.isBlank()) {
            otherGameObject = findGameObject(otherGameObjectName);
        }
    }

    public boolean isColliding(GameObject go) {
        if (go == null || go == getGameObject()) {
            return false;
        }

        TransformComponent thisTransform = getTransform();
        TransformComponent otherTransform = go.getComponent(TransformComponent.class);

        if (thisTransform == null || otherTransform == null) {
            return false;
        }

        boolean overlapX = Math.abs(thisTransform.position.x - otherTransform.position.x)
                < (thisTransform.scale.x + otherTransform.scale.x) / 2.0f;

        boolean overlapY = Math.abs(thisTransform.position.y - otherTransform.position.y)
                < (thisTransform.scale.y + otherTransform.scale.y) / 2.0f;

        boolean overlapZ = Math.abs(thisTransform.position.z - otherTransform.position.z)
                < (thisTransform.scale.z + otherTransform.scale.z) / 2.0f;

        return overlapX && overlapY && overlapZ;
    }

    public GameObject getOtherGameObject() {
        return otherGameObject;
    }

    public void setOtherGameObject(GameObject go) {
        this.otherGameObject = go;
        if (go != null) {
            this.otherGameObjectName = go.name;
        }
    }
}