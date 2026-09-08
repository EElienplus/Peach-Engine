package net.meowsers;

import net.meowsers.Peach.ECS.Components.LightComponent;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.Graphics.Light;
import net.meowsers.Peach.Graphics.Mesh;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Graphics.Shader;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.LightType;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MultiLightTest {

    private static long windowHandle = 0;

    @BeforeAll
    public static void initGL() {
        if (GLFW.glfwInit()) {
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            windowHandle = GLFW.glfwCreateWindow(640, 480, "Headless Test", 0, 0);
            if (windowHandle != 0) {
                GLFW.glfwMakeContextCurrent(windowHandle);
                GL.createCapabilities();
            }
        }
    }

    @AfterAll
    public static void cleanupGL() {
        if (windowHandle != 0) {
            GLFW.glfwDestroyWindow(windowHandle);
        }
        GLFW.glfwTerminate();
    }

    @BeforeEach
    public void setup() {
        Renderer.clearLights();
    }

    @Test
    public void testRendererMultiLightManagement() {
        assertEquals(0, Renderer.getLights().size());
        assertNull(Renderer.getLight());

        Light l1 = new Light(new Vector3f(0, 5, 0), Color.Red, 1.0f, LightType.POINT);
        Light l2 = new Light(new Vector3f(10, 5, 0), Color.Green, 2.0f, LightType.DIRECTIONAL);
        Light l3 = new Light(new Vector3f(20, 5, 0), Color.Blue, 3.0f, LightType.SPOT);

        Renderer.addLight(l1);
        Renderer.addLight(l2);
        Renderer.addLight(l3);

        assertEquals(3, Renderer.getLights().size());
        assertEquals(l1, Renderer.getLight());
        assertEquals(l1, Renderer.getLight(0));
        assertEquals(l2, Renderer.getLight(1));
        assertEquals(l3, Renderer.getLight(2));
        assertNull(Renderer.getLight(3));

        Renderer.removeLight(l2);
        assertEquals(2, Renderer.getLights().size());
        assertEquals(l1, Renderer.getLight(0));
        assertEquals(l3, Renderer.getLight(1));

        Renderer.setLight(new Light(Color.Yellow, 1.5f));
        assertEquals(1, Renderer.getLights().size());
        assertEquals(Color.Yellow, Renderer.getLight().getColor());

        Renderer.setLights(Arrays.asList(l1, l2, l3));
        assertEquals(3, Renderer.getLights().size());

        Renderer.clearLights();
        assertEquals(0, Renderer.getLights().size());
    }

    @Test
    public void testLightComponentLifecycleAndMultiLight() {
        GameObject obj1 = new GameObject("LightObj1");
        LightComponent lc1 = new LightComponent(new Vector3f(1, 2, 3), Color.Red, 1.0f);
        obj1.addComponent(lc1);

        assertEquals(1, Renderer.getLights().size());
        assertTrue(Renderer.getLights().contains(lc1.getLight()));

        GameObject obj2 = new GameObject("LightObj2");
        LightComponent lc2 = new LightComponent(new Vector3f(4, 5, 6), Color.Blue, 2.0f);
        obj2.addComponent(lc2);

        assertEquals(2, Renderer.getLights().size());
        assertTrue(Renderer.getLights().contains(lc1.getLight()));
        assertTrue(Renderer.getLights().contains(lc2.getLight()));

        // Disable lc1
        lc1.setEnabled(false);
        assertEquals(1, Renderer.getLights().size());
        assertFalse(Renderer.getLights().contains(lc1.getLight()));
        assertTrue(Renderer.getLights().contains(lc2.getLight()));

        // Re-enable lc1
        lc1.setEnabled(true);
        assertEquals(2, Renderer.getLights().size());
        assertTrue(Renderer.getLights().contains(lc1.getLight()));

        // Destroy obj2
        obj2.destroy();
        assertEquals(1, Renderer.getLights().size());
        assertFalse(Renderer.getLights().contains(lc2.getLight()));
        assertTrue(Renderer.getLights().contains(lc1.getLight()));
    }

    @Test
    public void testLightComponentDebugBall() {
        LightComponent lc = new LightComponent(new Vector3f(1, 2, 3), Color.Cyan, 1.0f);
        assertTrue(lc.isDrawDebugBall());
        assertTrue(lc.getDrawDebugBall());

        lc.drawDebugBall(false);
        assertFalse(lc.isDrawDebugBall());

        lc.setDrawDebugBall(true);
        assertTrue(lc.isDrawDebugBall());

        assertEquals(0.5f, lc.getDebugBallRadius());
        lc.setDebugBallRadius(1.2f);
        assertEquals(1.2f, lc.getDebugBallRadius());

        // Calling update should update position and draw debug ball without errors
        GameObject go = new GameObject("Test");
        go.addComponent(lc);
        lc.update(0.016f);
    }

    @Test
    public void testMeshCreateSphereOverloads() {
        Mesh m1 = Mesh.createSphere();
        assertNotNull(m1);
        assertFalse(m1.getVertices().isEmpty());
        assertFalse(m1.getIndices().isEmpty());

        Mesh m2 = Mesh.createSphere(1.0f);
        assertNotNull(m2);

        Mesh m3 = Mesh.createSphere(2.0f, Color.Green);
        assertNotNull(m3);
        assertEquals(Color.Green, m3.getColor());

        Mesh m4 = Mesh.createSphere(0.5f, 8, 8);
        assertNotNull(m4);

        Mesh m5 = Mesh.createSphere(0.5f, 8, 8, Color.Red);
        assertNotNull(m5);
    }

    @Test
    public void testShaderMultiLightGlslGeneration() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        Shader shader = (Shader) unsafe.allocateInstance(Shader.class);

        Method compileMethod = Shader.class.getDeclaredMethod("compileSlangToGLSL", String.class, String.class, String.class);
        compileMethod.setAccessible(true);
        String fragGLSL = (String) compileMethod.invoke(shader, "src/main/resources/Shaders/default.slang", "fragMain", "fragment");

        assertNotNull(fragGLSL);
        assertTrue(fragGLSL.contains("uniform vec3 uLightPos[16];"));
        assertTrue(fragGLSL.contains("uniform float uLightIntensity[16];"));
        assertTrue(fragGLSL.contains("uniform vec4 uLightColor[16];"));
        assertTrue(fragGLSL.contains("uniform vec3 uLightDir[16];"));
        assertTrue(fragGLSL.contains("uniform int uLightType[16];"));
        assertTrue(fragGLSL.contains("uniform int uIsDirectional[16];"));
        assertTrue(fragGLSL.contains("uniform float uCutOff[16];"));
        assertTrue(fragGLSL.contains("uniform float uOuterCutOff[16];"));
        assertTrue(fragGLSL.contains("uniform float uSpecularIntensity[16];"));
        assertTrue(fragGLSL.contains("uniform float uShininess[16];"));
        assertTrue(fragGLSL.contains("uniform int uNumLights;"));
    }
}
