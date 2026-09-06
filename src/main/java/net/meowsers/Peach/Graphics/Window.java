package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.WindowParams;
import net.meowsers.Peach.Utils.Log;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL41.*;

public class Window {

    private long handle;
    private boolean running;

    public Window() {
        handle = glfwCreateWindow(WindowParams.width, WindowParams.height, WindowParams.title, 0, 0);
        if(handle == 0) {
            Log.fatalGlfw();
        }

        running = true;

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        int[] fWidth = new int[1];
        int[] fHeight = new int[1];
        glfwGetFramebufferSize(handle, fWidth, fHeight);
        glViewport(0, 0, fWidth[0], fHeight[0]);
        glfwSetFramebufferSizeCallback(handle, this::framebuferSizeCallback);

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_CULL_FACE);
        glEnable(GL_MULTISAMPLE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);;

        glfwSwapInterval(0);
        glfwRequestWindowAttention(handle);
    }

    private void framebuferSizeCallback(long handle, int width, int height) {
        glViewport(0, 0, width, height);
        updateProperties();
    }

    public void update() {
        running = !glfwWindowShouldClose(handle);
        glfwPollEvents();
    }

    public void updateProperties() {
        glfwSetWindowSize(handle, WindowParams.width, WindowParams.height);
        glfwSetWindowTitle(handle, WindowParams.title);
    }

    public void end() {
        glfwDestroyWindow(handle);
    }

    public void clearBackground(Color color) {
        glClearColor(color.r, color.g, color.b, color.a);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public long getHandle() {
        return handle;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public void present() {
        glfwSwapBuffers(handle);
    }

    public void setCursorLocked(boolean locked) {
        if (handle != 0) {
            glfwSetInputMode(handle, GLFW_CURSOR, locked ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
        }
    }

    public boolean isCursorLocked() {
        return handle != 0 && glfwGetInputMode(handle, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
    }
}
