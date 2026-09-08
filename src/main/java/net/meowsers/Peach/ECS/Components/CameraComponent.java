package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.GUI.Editor;
import net.meowsers.Peach.Graphics.Camera;
import net.meowsers.Peach.Graphics.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CameraComponent extends BehaviorComponent {

    private Camera camera;

    @Editor private boolean isCurrent = false;
    @Editor private boolean handleMovement = true;

    @Editor public Vector3f cameraPosition = new Vector3f(0.0f, 0.0f, 3.0f);
    @Editor public float cameraYaw = -90.0f;
    @Editor public float cameraPitch = 0.0f;
    @Editor public float cameraFov = 60.0f;
    @Editor public float cameraNear = 0.1f;
    @Editor public float cameraFar = 10000.0f;
    @Editor public float cameraAspectRatio = 16.0f / 9.0f;
    @Editor public boolean rightClickToLook = true;

    @Editor public float movementSpeed = 10.0f;
    @Editor public float cameraSensitivity = 0.1f;

    private final Vector3f lastTransformPos = new Vector3f(Float.NaN);
    private final Vector3f lastFieldPos = new Vector3f(Float.NaN);
    private final Vector3f lastTransformRot = new Vector3f(Float.NaN);
    private float lastPitch = Float.NaN;
    private float lastYaw = Float.NaN;

    public CameraComponent() {
        setCamera(new Camera());
    }

    public CameraComponent(boolean setCurrent) {
        setCamera(new Camera());
        if (setCurrent) {
            setCurrent();
        }
    }

    public CameraComponent(boolean setCurrent, boolean handleMovement) {
        setCamera(new Camera());
        this.handleMovement = handleMovement;
        if (setCurrent) {
            setCurrent();
        }
    }

    public CameraComponent(Camera camera) {
        setCamera(camera);
    }

    public CameraComponent(Vector3f position) {
        setCamera(new Camera(position));
    }

    private void ensureCamera() {
        if (camera == null) {
            setCamera(new Camera());
        }
    }

    private void pullFieldsFromCamera() {
        ensureCamera();

        cameraPosition.set(camera.getPosition());
        cameraYaw = camera.getYaw();
        cameraPitch = camera.getPitch();
        cameraFov = camera.getFov();
        cameraNear = camera.getNear();
        cameraFar = camera.getFar();
        cameraAspectRatio = camera.getAspectRatio();
        rightClickToLook = camera.isRightClickToLook();
    }

    private void pushFieldsToCamera() {
        ensureCamera();

        camera.setPosition(cameraPosition);
        camera.setYaw(cameraYaw);
        camera.setPitch(cameraPitch);
        camera.setFov(cameraFov);
        camera.setNear(cameraNear);
        camera.setFar(cameraFar);
        camera.setAspectRatio(cameraAspectRatio);
        camera.setRightClickToLook(rightClickToLook);
    }

    private void syncTransform() {
        ensureCamera();

        TransformComponent t = getTransform();
        if (t != null && t.position != null) {
            boolean hasLast = !Float.isNaN(lastTransformPos.x);
            if (!hasLast) {
                if (t.position.x != 0.0f || t.position.y != 0.0f || t.position.z != 0.0f) {
                    cameraPosition.set(t.position);
                    camera.setPosition(cameraPosition);
                } else {
                    t.position.set(cameraPosition);
                    camera.setPosition(cameraPosition);
                }
                if (t.rotation != null) {
                    if (t.rotation.x != 0.0f || t.rotation.y != 0.0f || t.rotation.z != 0.0f) {
                        cameraPitch = t.rotation.x;
                        cameraYaw = t.rotation.y;
                        camera.setPitch(cameraPitch);
                        camera.setYaw(cameraYaw);
                    } else {
                        t.rotation.set(cameraPitch, cameraYaw, t.rotation.z);
                    }
                }
            } else {
                if (!t.position.equals(lastTransformPos)) {
                    cameraPosition.set(t.position);
                    camera.setPosition(cameraPosition);
                } else if (!cameraPosition.equals(lastFieldPos)) {
                    camera.setPosition(cameraPosition);
                    t.position.set(cameraPosition);
                }

                if (t.rotation != null) {
                    if (!t.rotation.equals(lastTransformRot)) {
                        cameraPitch = t.rotation.x;
                        cameraYaw = t.rotation.y;
                        camera.setPitch(cameraPitch);
                        camera.setYaw(cameraYaw);
                    } else if (cameraPitch != lastPitch || cameraYaw != lastYaw) {
                        camera.setPitch(cameraPitch);
                        camera.setYaw(cameraYaw);
                        t.rotation.set(cameraPitch, cameraYaw, t.rotation.z);
                    }
                }
            }

            lastTransformPos.set(t.position);
            lastFieldPos.set(cameraPosition);
            if (t.rotation != null) {
                lastTransformRot.set(t.rotation);
            }
            lastPitch = cameraPitch;
            lastYaw = cameraYaw;
        }
    }

    @Override
    public void onAdded() {
        super.onAdded();
        syncTransform();
    }

    @Override
    public void start() {
        super.start();
        syncTransform();

        if (isCurrent || Renderer.getCamera() == null) {
            setCurrent(true);
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);

        ensureCamera();
        syncTransform();
        pushFieldsToCamera();

        if (handleMovement) {
            camera.handleCameraMovement(dt, movementSpeed, cameraSensitivity);
            pullFieldsFromCamera();
            TransformComponent t = getTransform();
            if (t != null) {
                if (t.position != null) {
                    t.position.set(cameraPosition);
                    lastTransformPos.set(t.position);
                }
                if (t.rotation != null) {
                    t.rotation.set(cameraPitch, cameraYaw, t.rotation.z);
                    lastTransformRot.set(t.rotation);
                }
            }
            lastFieldPos.set(cameraPosition);
            lastPitch = cameraPitch;
            lastYaw = cameraYaw;
        }

        if (isCurrent && Renderer.getCamera() != camera) {
            Renderer.setCamera(camera);
        } else if (!isCurrent && Renderer.getCamera() == camera) {
            Renderer.setCamera(null);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (isCurrent) {
            ensureCamera();
            Renderer.setCamera(camera);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        if (Renderer.getCamera() == camera) {
            Renderer.setCamera(null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (Renderer.getCamera() == camera) {
            Renderer.setCamera(null);
        }
    }

    public void setCurrent() {
        setCurrent(true);
    }

    public void setCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
        ensureCamera();

        if (isCurrent) {
            Renderer.setCamera(camera);
        } else if (Renderer.getCamera() == camera) {
            Renderer.setCamera(null);
        }
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public Camera getCamera() {
        ensureCamera();
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera != null ? camera : new Camera();
        pullFieldsFromCamera();

        if (isCurrent) {
            Renderer.setCamera(this.camera);
        }
    }

    public Vector3f getPosition() {
        return cameraPosition;
    }

    public void setPosition(Vector3f pos) {
        if (pos != null) {
            cameraPosition.set(pos);
        }

        ensureCamera();
        camera.setPosition(cameraPosition);

        TransformComponent t = getTransform();
        if (t != null && t.position != null) {
            t.position.set(cameraPosition);
            lastTransformPos.set(t.position);
        }
        lastFieldPos.set(cameraPosition);
    }

    public Vector3f getFront() {
        ensureCamera();
        pushFieldsToCamera();
        return camera.getFront();
    }

    public Vector3f getUp() {
        ensureCamera();
        pushFieldsToCamera();
        return camera.getUp();
    }

    public Vector3f getRight() {
        ensureCamera();
        pushFieldsToCamera();
        return camera.getRight();
    }

    public float getYaw() {
        return cameraYaw;
    }

    public void setYaw(float yaw) {
        this.cameraYaw = yaw;
        ensureCamera();
        camera.setYaw(cameraYaw);
        TransformComponent t = getTransform();
        if (t != null && t.rotation != null) {
            t.rotation.y = cameraYaw;
            lastTransformRot.set(t.rotation);
        }
        lastYaw = cameraYaw;
    }

    public float getPitch() {
        return cameraPitch;
    }

    public void setPitch(float pitch) {
        this.cameraPitch = pitch;
        ensureCamera();
        camera.setPitch(cameraPitch);
        TransformComponent t = getTransform();
        if (t != null && t.rotation != null) {
            t.rotation.x = cameraPitch;
            lastTransformRot.set(t.rotation);
        }
        lastPitch = cameraPitch;
    }

    public float getFov() {
        return cameraFov;
    }

    public void setFov(float fov) {
        this.cameraFov = fov;
        ensureCamera();
        camera.setFov(cameraFov);
    }

    public float getNear() {
        return cameraNear;
    }

    public void setNear(float near) {
        this.cameraNear = near;
        ensureCamera();
        camera.setNear(cameraNear);
    }

    public float getFar() {
        return cameraFar;
    }

    public void setFar(float far) {
        this.cameraFar = far;
        ensureCamera();
        camera.setFar(cameraFar);
    }

    public float getAspectRatio() {
        return cameraAspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.cameraAspectRatio = aspectRatio;
        ensureCamera();
        camera.setAspectRatio(cameraAspectRatio);
    }

    public boolean isRightClickToLook() {
        return rightClickToLook;
    }

    public void setRightClickToLook(boolean rightClickToLook) {
        this.rightClickToLook = rightClickToLook;
        ensureCamera();
        camera.setRightClickToLook(rightClickToLook);
    }

    public void lookAt(Vector3f target) {
        ensureCamera();
        camera.setPosition(cameraPosition);
        camera.lookAt(target);
        pullFieldsFromCamera();
        TransformComponent t = getTransform();
        if (t != null && t.rotation != null) {
            t.rotation.set(cameraPitch, cameraYaw, t.rotation.z);
            lastTransformRot.set(t.rotation);
        }
        lastPitch = cameraPitch;
        lastYaw = cameraYaw;
    }

    public Matrix4f getViewMatrix() {
        ensureCamera();
        pushFieldsToCamera();
        return camera.getViewMatrix();
    }

    public Matrix4f getProjectionMatrix() {
        ensureCamera();
        pushFieldsToCamera();
        return camera.getProjectionMatrix();
    }

    public void handleMovement(boolean val) {
        handleMovement = val;
    }

    public boolean isHandlingMovement() {
        return handleMovement;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public float getCameraSensitivity() {
        return cameraSensitivity;
    }

    public void setCameraSensitivity(float cameraSensitivity) {
        this.cameraSensitivity = cameraSensitivity;
    }
}
