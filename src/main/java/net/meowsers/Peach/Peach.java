package net.meowsers.Peach;

import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Graphics.Window;
import net.meowsers.Peach.Utils.Log;

import static org.lwjgl.glfw.GLFW.*;

public class Peach {

    private Window window;

    public void start() {

        if(!glfwInit()) {
            Log.fatalGlfw();
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = new Window();
        Renderer.init(window.getHandle());

    }

    public void update() {
        window.update();
    }

    public void end() {
        window.end();
        glfwTerminate();
    }


    public Window getWindow() {
        return window;
    }
}
