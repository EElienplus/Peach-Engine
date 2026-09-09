package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.GUI.PeachGui;
import net.meowsers.Peach.Structures.Key;
import net.meowsers.Peach.Structures.MouseButton;
import net.meowsers.Peach.Structures.WindowParams;
import net.meowsers.Peach.Utils.Input;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position = new Vector3f(0.0f, 0.0f, 3.0f);
    private final Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);
    private final Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);

    private float yaw = -90.0f;
    private float pitch = 0.0f;
    private float fov = 60.0f;
    private float near = 0.1f;
    private float far = 10000.0f;
    private float aspectRatio = 16.0f / 9.0f;

    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Vector3f target = new Vector3f();

    private double lastMouseX = 0.0;
    private double lastMouseY = 0.0;
    private boolean firstMouse = true;
    private boolean wasRmbDown = false;
    private boolean rightClickToLook = true;

    public Camera() {
        updateCameraVectors();
    }

    public Camera(Vector3f position) {
        if (position != null) {
            this.position.set(position);
        }
        updateCameraVectors();
    }

    public Camera(Vector3f position, float fov) {
        if (position != null) {
            this.position.set(position);
        }
        this.fov = fov;
        updateCameraVectors();
    }

    public void updateCameraVectors() {
        front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.normalize();

        front.cross(worldUp, right).normalize();
        right.cross(front, up).normalize();
    }

    public Matrix4f getViewMatrix() {
        updateCameraVectors();
        position.add(front, target);
        return viewMatrix.identity().lookAt(position, target, up);
    }

    public Matrix4f getProjectionMatrix() {
        if (WindowParams.width > 0 && WindowParams.height > 0) {
            aspectRatio = (float) WindowParams.width / (float) WindowParams.height;
        }
        return projectionMatrix.identity().perspective((float) Math.toRadians(fov), aspectRatio, near, far);
    }

    /**
     * Handles Unity-like camera flythrough movement with one method call.
     * Hold Right Mouse Button to look around and use WASD / QE keys to navigate.
     *
     * @param dt delta time in seconds
     * @param moveSpeed movement speed units per second
     * @param mouseSensitivity mouse rotation sensitivity
     */
    public void handleCameraMovement(float dt, float moveSpeed, float mouseSensitivity) {
        double mouseX = Input.getMouseX();
        double mouseY = Input.getMouseY();

        boolean isRmbDown = Input.isMouseButtonDown(MouseButton.RIGHT);
        boolean guiCapturesMouse = PeachGui.isInitialized() && PeachGui.wantCaptureMouse();
        boolean guiCapturesKeyboard = PeachGui.isInitialized() && PeachGui.wantCaptureKeyboard();

        if (guiCapturesMouse && !wasRmbDown) {
            isRmbDown = false;
        }

        if (firstMouse) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            firstMouse = false;
        }

        if (rightClickToLook) {
            if (isRmbDown) {
                if (!wasRmbDown) {
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                    wasRmbDown = true;
                }
                Input.setCursorLocked(true);
            } else {
                if (wasRmbDown) {
                    wasRmbDown = false;
                    Input.setCursorLocked(false);
                }
            }
        } else {
            Input.setCursorLocked(true);
        }

        double dx = mouseX - lastMouseX;
        double dy = lastMouseY - mouseY; // Inverted because OpenGL / window coordinates
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // Unity-like rotation when RMB is held
        if (isRmbDown || !rightClickToLook) {
            if (dx != 0.0 || dy != 0.0) {
                yaw += (float) (dx * mouseSensitivity);
                pitch += (float) (dy * mouseSensitivity);

                if (pitch > 89.0f) pitch = 89.0f;
                if (pitch < -89.0f) pitch = -89.0f;

                updateCameraVectors();
            }
        }

        if (guiCapturesKeyboard || (rightClickToLook && !isRmbDown)) {
            return;
        }

        float currentSpeed = moveSpeed;
        if (Input.isShiftPressed()) {
            currentSpeed *= 2.5f; // Sprint
        }

        float velocity = currentSpeed * dt;

        // Forward / Backward
        if (Input.isKeyDown(Key.W) || Input.isKeyDown(Key.UP)) {
            position.fma(velocity, front);
        }
        if (Input.isKeyDown(Key.S) || Input.isKeyDown(Key.DOWN)) {
            position.fma(-velocity, front);
        }

        // Left / Right (Strafe)
        if (Input.isKeyDown(Key.A) || Input.isKeyDown(Key.LEFT)) {
            position.fma(-velocity, right);
        }
        if (Input.isKeyDown(Key.D) || Input.isKeyDown(Key.RIGHT)) {
            position.fma(velocity, right);
        }

        // Up / Down (Unity style: E = Up, Q = Down; also Space = Up)
        if (Input.isKeyDown(Key.E) || Input.isKeyDown(Key.SPACE)) {
            position.fma(velocity, worldUp);
        }
        if (Input.isKeyDown(Key.Q)) {
            position.fma(-velocity, worldUp);
        }

        // Scroll zoom / move
        double scrollY = Input.getMouseScrollY();
        if (scrollY != 0.0) {
            position.fma((float) (scrollY * moveSpeed * 0.3f), front);
        }
    }

    public void handleCameraMovement(float dt, float moveSpeed) {
        handleCameraMovement(dt, moveSpeed, 0.15f);
    }

    public void handleCameraMovement(float dt) {
        handleCameraMovement(dt, 5.0f, 0.15f);
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f pos) {
        if (pos != null) {
            this.position.set(pos);
        }
    }

    public Vector3f getFront() {
        return front;
    }

    public Vector3f getUp() {
        return up;
    }

    public Vector3f getRight() {
        return right;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        updateCameraVectors();
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
        updateCameraVectors();
    }

    public float getFov() {
        return fov;
    }

    public void setFov(float fov) {
        this.fov = fov;
    }

    public float getNear() {
        return near;
    }

    public void setNear(float near) {
        this.near = near;
    }

    public float getFar() {
        return far;
    }

    public void setFar(float far) {
        this.far = far;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public void lookAt(Vector3f target) {
        if (target == null) return;
        Vector3f dir = new Vector3f(target.x - position.x, target.y - position.y, target.z - position.z);
        if (dir.lengthSquared() > 0.00001f) {
            dir.normalize();
            this.pitch = (float) Math.toDegrees(Math.asin(dir.y));
            this.yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x));
            updateCameraVectors();
        }
    }

    public boolean isRightClickToLook() {
        return rightClickToLook;
    }

    public void setRightClickToLook(boolean rightClickToLook) {
        this.rightClickToLook = rightClickToLook;
        if (!rightClickToLook) {
            Input.setCursorLocked(true);
        } else if (!wasRmbDown) {
            Input.setCursorLocked(false);
        }
    }
}
