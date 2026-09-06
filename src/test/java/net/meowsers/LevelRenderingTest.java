package net.meowsers;

import net.meowsers.Peach.ECS.Components.MeshRendererComponent;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Camera;
import net.meowsers.Peach.Graphics.Light;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Structures.Color;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import static org.junit.jupiter.api.Assertions.*;

public class LevelRenderingTest {

    private static long window;

    @BeforeAll
    public static void setUp() {
        if (!GLFW.glfwInit()) {
            fail("Failed to initialize GLFW");
        }
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(800, 600, "Headless Test", 0, 0);
        if (window == 0) {
            fail("Failed to create GLFW window");
        }
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        Renderer.init(window);
    }

    @AfterAll
    public static void tearDown() {
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    @Test
    public void testBunnyModelLoadsAndCenters() {
        Model bunny = new Model("src/main/resources/Models/bunny.obj");
        assertNotNull(bunny);
        assertFalse(bunny.getMeshes().isEmpty());
        assertTrue(bunny.getMesh(0).getVertices().size() > 1000);

        bunny.fitToSize(1.0f);
        Vector3f center = bunny.getCenter();
        assertEquals(0.0f, center.x, 0.05f);
        assertEquals(0.0f, center.y, 0.05f);
        assertEquals(0.0f, center.z, 0.05f);

        Vector3f size = bunny.getSize();
        float maxDim = Math.max(size.x, Math.max(size.y, size.z));
        assertEquals(1.0f, maxDim, 0.05f);
    }

    @Test
    public void testLevelUpdatesAndRendersBunnyGameObject() {
        MyLevel level = new MyLevel();
        level.start();
        level.internalStart();

        assertEquals(3, level.getGameObjects().size());
        GameObject bunny = level.getGameObjects().get(0);
        assertNotNull(bunny.getComponent(MeshRendererComponent.class));
        assertNotNull(bunny.transform);

        GameObject cameraObj = level.getGameObjects().get(1);
        assertNotNull(cameraObj.getComponent(net.meowsers.Peach.ECS.Components.CameraComponent.class));
        assertNotNull(Renderer.getCamera());

        GameObject lightObj = level.getGameObjects().get(2);
        assertNotNull(lightObj.getComponent(net.meowsers.Peach.ECS.Components.LightComponent.class));
        assertNotNull(Renderer.getLight());
        assertEquals(Color.White, Renderer.getLight().getColor());

        // Run level update
        level.update(0.016f);
        level.internalUpdate(0.016f);

        // Render batches
        Renderer.render();

        // Render calls should be > 0 since bunny mesh has thousands of vertices
        int renderCalls = Renderer.getRenderCallsAmount();
        assertTrue(renderCalls > 0, "Expected bunny render calls > 0, but got " + renderCalls);
    }

    @Test
    public void testCameraComponentLifecycleAndRendererIntegration() {
        GameObject camGo = new GameObject("TestCamera");
        net.meowsers.Peach.ECS.Components.CameraComponent camComp = new net.meowsers.Peach.ECS.Components.CameraComponent();
        camGo.addComponent(camComp);

        // Can set position / angle before start() without NPE
        camComp.setPosition(new Vector3f(0.0f, 2.0f, 5.0f));
        assertEquals(0.0f, camComp.getPosition().x, 0.001f);
        assertEquals(2.0f, camComp.getPosition().y, 0.001f);
        assertEquals(5.0f, camComp.getPosition().z, 0.001f);

        // Set as current before start()
        camComp.setCurrent();
        assertTrue(camComp.isCurrent());
        assertSame(camComp.getCamera(), Renderer.getCamera());

        camGo.start();
        assertSame(camComp.getCamera(), Renderer.getCamera());
        assertEquals(5.0f, camGo.transform.position.z, 0.001f);

        // Look at origin
        camComp.lookAt(new Vector3f(0.0f, 0.0f, 0.0f));
        camGo.update(0.016f);

        Renderer.render();
        assertNotNull(Renderer.getCamera());
        assertEquals(5.0f, Renderer.getCamera().getPosition().z, 0.001f);
    }
}
