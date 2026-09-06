package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.Graphics.Camera;
import net.meowsers.Peach.Graphics.Renderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class CameraComponent extends Component {

    private Camera camera;
    private boolean handleMovement = true;
    private boolean isCurrent = false;

    private float movementSpeed = 10, cameraSensitivity = .1f;

    public CameraComponent() {
        this.camera = new Camera();
    }

    public CameraComponent(boolean setCurrent) {
        this.camera = new Camera();
        if (setCurrent) {
            setCurrent();
        }
    }

    public CameraComponent(boolean setCurrent, boolean handleMovement) {
        this.camera = new Camera();
        this.handleMovement = handleMovement;
        if (setCurrent) {
            setCurrent();
        }
    }

    public CameraComponent(Camera camera) {
        this.camera = camera != null ? camera : new Camera();
    }

    public CameraComponent(Vector3f position) {
        this.camera = new Camera(position);
    }

    @Override
    public void onAdded() {
        super.onAdded();
        if (camera == null) {
            camera = new Camera();
        }
        if (transform != null && transform.position != null) {
            if (transform.position.x != 0.0f || transform.position.y != 0.0f || transform.position.z != 0.0f) {
                camera.setPosition(transform.position);
            } else {
                transform.position.set(camera.getPosition());
            }
        }
    }

    @Override
    public void start() {
        super.start();
        if (camera == null) {
            camera = new Camera();
        }
        if (transform != null && transform.position != null) {
            if (transform.position.x != 0.0f || transform.position.y != 0.0f || transform.position.z != 0.0f) {
                camera.setPosition(transform.position);
            } else {
                transform.position.set(camera.getPosition());
            }
        }
        if (isCurrent || Renderer.getCamera() == null) {
            Renderer.setCamera(camera);
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (camera == null) {
            camera = new Camera();
        }
        if (handleMovement) {
            camera.handleCameraMovement(dt, movementSpeed, cameraSensitivity);
        }
        if (transform != null && transform.position != null) {
            transform.position.set(camera.getPosition());
        }
        if (isCurrent && Renderer.getCamera() != camera) {
            Renderer.setCamera(camera);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (isCurrent) {
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
        this.isCurrent = true;
        if (camera == null) {
            camera = new Camera();
        }
        Renderer.setCamera(camera);
    }

    public boolean isCurrent() {
        return isCurrent;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera != null ? camera : new Camera();
        if (isCurrent) {
            Renderer.setCamera(this.camera);
        }
    }

    public Vector3f getPosition() {
        return camera.getPosition();
    }

    public void setPosition(Vector3f pos) {
        camera.setPosition(pos);
        if (transform != null && transform.position != null) {
            transform.position.set(camera.getPosition());
        }
    }

    public Vector3f getFront() {
        return camera.getFront();
    }

    public Vector3f getUp() {
        return camera.getUp();
    }

    public Vector3f getRight() {
        return camera.getRight();
    }

    public float getYaw() {
        return camera.getYaw();
    }

    public void setYaw(float yaw) {
        camera.setYaw(yaw);
    }

    public float getPitch() {
        return camera.getPitch();
    }

    public void setPitch(float pitch) {
        camera.setPitch(pitch);
    }

    public float getFov() {
        return camera.getFov();
    }

    public void setFov(float fov) {
        camera.setFov(fov);
    }

    public float getNear() {
        return camera.getNear();
    }

    public void setNear(float near) {
        camera.setNear(near);
    }

    public float getFar() {
        return camera.getFar();
    }

    public void setFar(float far) {
        camera.setFar(far);
    }

    public float getAspectRatio() {
        return camera.getAspectRatio();
    }

    public void setAspectRatio(float aspectRatio) {
        camera.setAspectRatio(aspectRatio);
    }

    public boolean isRightClickToLook() {
        return camera.isRightClickToLook();
    }

    public void setRightClickToLook(boolean rightClickToLook) {
        camera.setRightClickToLook(rightClickToLook);
    }

    public void lookAt(Vector3f target) {
        camera.lookAt(target);
    }

    public Matrix4f getViewMatrix() {
        return camera.getViewMatrix();
    }

    public Matrix4f getProjectionMatrix() {
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
