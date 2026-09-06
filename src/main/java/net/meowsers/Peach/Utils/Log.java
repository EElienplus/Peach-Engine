package net.meowsers.Peach.Utils;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import static org.lwjgl.glfw.GLFW.*;

public class Log {

    public static void message(String message) {
        System.out.println(ConsoleColors.WHITE_BOLD + message + ConsoleColors.RESET);
    }

    public static void warning(String message) {
        System.out.println(ConsoleColors.YELLOW_BRIGHT + message + ConsoleColors.RESET);
    }

    public static void fatal(String message) {
        throw new PeachException(message);
    }

    public static void fatalGlfw() {
        PointerBuffer description = BufferUtils.createPointerBuffer(512);
        fatal("Glfw Error; Code: " + glfwGetError(description) + "\nDescription: " + description.getStringASCII());
    }

}
