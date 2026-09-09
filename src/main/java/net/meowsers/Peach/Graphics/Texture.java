package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Utils.PeachException;
import org.lwjgl.BufferUtils;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.stb.STBImage.*;

public class Texture {
    private final int textureID;
    private int width, height;
    private String filepath;

    public Texture(int width, int height, ByteBuffer buffer) {
        this.width = width;
        this.height = height;
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
    }

    public Texture(int width, int height, Color color) {
        this(width, height, createColorBuffer(width, height, color));
    }

    private static ByteBuffer createColorBuffer(int width, int height, Color color) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        Color c = color != null ? color : Color.White;
        byte r = (byte) ((int) (c.r * 255.0f) & 0xFF);
        byte g = (byte) ((int) (c.g * 255.0f) & 0xFF);
        byte b = (byte) ((int) (c.b * 255.0f) & 0xFF);
        byte a = (byte) ((int) (c.a * 255.0f) & 0xFF);

        for (int i = 0; i < width * height; i++) {
            buffer.put(r);
            buffer.put(g);
            buffer.put(b);
            buffer.put(a);
        }
        buffer.flip();
        return buffer;
    }

    public Texture(String filepath) {
        this.filepath = filepath;
        String resolvedPath = resolveFilePath(filepath);
        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer channels = BufferUtils.createIntBuffer(1);

//        stbi_set_flip_vertically_on_load(true);
        ByteBuffer image = stbi_load(resolvedPath, w, h, channels, 0);
        if (image != null) {
            this.width = w.get(0);
            this.height = h.get(0);

            int format = switch (channels.get(0)) {
                case 3 -> GL_RGB;
                case 4 -> GL_RGBA;
                default -> throw new PeachException("Unsupported texture channels: " + channels.get(0));
            };

            textureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureID);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, format, this.width, this.height, 0, format, GL_UNSIGNED_BYTE, image);
            stbi_image_free(image);
        } else {
            throw new PeachException("Failed to load texture file: " + filepath + " (resolved to: " + resolvedPath + ")\nReason: " + stbi_failure_reason());
        }
    }

    private static String resolveFilePath(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        File resFile = new File("src/main/resources/" + filePath);
        if (resFile.exists()) {
            return resFile.getAbsolutePath();
        }

        File altRes = new File(filePath.replaceFirst("^assets/", "src/main/resources/"));
        if (altRes.exists()) {
            return altRes.getAbsolutePath();
        }

        return filePath;
    }

    public void bind(int slot) {
        org.lwjgl.opengl.GL13.glActiveTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getId() {
        return textureID;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public void destroy() {
        glDeleteTextures(textureID);
    }
}