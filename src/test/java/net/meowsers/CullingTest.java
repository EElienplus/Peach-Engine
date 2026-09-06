package net.meowsers;

import net.meowsers.Peach.Graphics.Camera;
import net.meowsers.Peach.Graphics.Mesh;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Vertex;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.*;

public class CullingTest {

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

        window = GLFW.glfwCreateWindow(800, 600, "Culling Test", 0, 0);
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
    public void testRendererCullingStateAPIs() {
        Renderer.enableCulling();
        assertTrue(Renderer.isCullingEnabled());
        assertTrue(Renderer.isBackfaceCullingEnabled());

        Renderer.disableCulling();
        assertFalse(Renderer.isCullingEnabled());
        assertFalse(Renderer.isBackfaceCullingEnabled());

        Renderer.setBackfaceCulling(true);
        assertTrue(Renderer.isCullingEnabled());

        Renderer.setCullFace(GL_FRONT);
        assertEquals(GL_FRONT, Renderer.getCullFace());

        Renderer.setCullFace(GL_BACK);
        assertEquals(GL_BACK, Renderer.getCullFace());

        Renderer.setFrontFace(GL_CW);
        assertEquals(GL_CW, Renderer.getFrontFace());

        Renderer.setFrontFace(GL_CCW);
        assertEquals(GL_CCW, Renderer.getFrontFace());

        Renderer.enableNormalCulling();
        assertTrue(Renderer.isNormalCullingEnabled());

        Renderer.disableNormalCulling();
        assertFalse(Renderer.isNormalCullingEnabled());

        Renderer.setNormalCulling(true);
        assertTrue(Renderer.isNormalCullingEnabled());
    }

    @Test
    public void testMeshGeometricBackfaceCulling() {
        // Create a unit cube centered at (0,0,0)
        Mesh cube = Mesh.createCube(1.0f, Color.White, null);
        assertEquals(12, cube.getTriangleCount());

        // Camera placed at (0, 0, 5) looking towards -Z
        Vector3f cameraPos = new Vector3f(0.0f, 0.0f, 5.0f);
        Mesh culled = cube.cullBackfaces(cameraPos);

        // Only the front face (2 triangles) should be visible from (0, 0, 5)
        assertEquals(2, culled.getTriangleCount());
        assertEquals(6, culled.getVertexCount());

        // Face normal of front face should have Z > 0
        Vector3f fn0 = culled.getFaceNormal(0);
        assertTrue(fn0.z > 0.9f);
    }

    @Test
    public void testMeshGeometricNormalCulling() {
        Mesh cube = Mesh.createCube(1.0f, Color.White, null);

        // View direction pointing towards -Z (camera looking down -Z axis)
        Vector3f viewDir = new Vector3f(0.0f, 0.0f, -1.0f);
        Mesh culled = cube.cullNormals(viewDir);

        // Only faces whose normal opposes viewDir (i.e. normal.z > 0, front face) are kept
        assertEquals(2, culled.getTriangleCount());
        Vector3f fn = culled.getFaceNormal(0);
        assertTrue(fn.z > 0.9f);
    }

    @Test
    public void testModelGeometricCulling() {
        Mesh cube = Mesh.createCube(1.0f, Color.White, null);
        Model model = new Model(cube);
        assertEquals(1, model.getMeshes().size());
        assertEquals(12, model.getMesh(0).getTriangleCount());

        Vector3f cameraPos = new Vector3f(0.0f, 0.0f, 5.0f);
        Model culledModel = model.cullBackfaces(cameraPos);
        assertEquals(1, culledModel.getMeshes().size());
        assertEquals(2, culledModel.getMesh(0).getTriangleCount());
    }

    @Test
    public void testRenderingBackfaceCullingDraw() {
        Camera camera = new Camera(new Vector3f(0.0f, 0.0f, 3.0f), 60.0f);
        Renderer.setCamera(camera);
        Renderer.setBgColor(Color.Black);
        Renderer.enableCulling();
        Renderer.setCullFace(GL_BACK);
        Renderer.setFrontFace(GL_CCW);
        Renderer.enableNormalCulling();

        // Draw a CCW front-facing triangle (visible)
        Mesh frontMesh = new Mesh("FrontFacing");
        frontMesh.addTriangle(
                new Vertex(-0.5f, -0.5f, 0.0f, 0, 0, 1, 1.0f, 0.0f, 0.0f, 1.0f, 0, 0),
                new Vertex( 0.5f, -0.5f, 0.0f, 0, 0, 1, 1.0f, 0.0f, 0.0f, 1.0f, 0, 0),
                new Vertex( 0.0f,  0.5f, 0.0f, 0, 0, 1, 1.0f, 0.0f, 0.0f, 1.0f, 0, 0)
        );

        Renderer.drawMesh(frontMesh);
        Renderer.render();

        int calls = Renderer.getRenderCallsAmount();
        assertEquals(1, calls);

        // Read pixel at center of viewport (400, 300)
        ByteBuffer pixel = BufferUtils.createByteBuffer(4);
        glReadPixels(400, 300, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        int r = pixel.get(0) & 0xFF;
        assertTrue(r > 100, "Front-facing triangle should be rendered and visible in framebuffer");

        // Now draw a CW back-facing triangle with backface culling enabled
        Renderer.clearBackground();
        Mesh backMesh = new Mesh("BackFacing");
        // CW winding: (0, 0.5) -> (0.5, -0.5) -> (-0.5, -0.5)
        backMesh.addTriangle(
                new Vertex( 0.0f,  0.5f, 0.0f, 0, 0, -1, 0.0f, 1.0f, 0.0f, 1.0f, 0, 0),
                new Vertex( 0.5f, -0.5f, 0.0f, 0, 0, -1, 0.0f, 1.0f, 0.0f, 1.0f, 0, 0),
                new Vertex(-0.5f, -0.5f, 0.0f, 0, 0, -1, 0.0f, 1.0f, 0.0f, 1.0f, 0, 0)
        );

        Renderer.drawMesh(backMesh);
        Renderer.render();

        pixel.clear();
        glReadPixels(400, 300, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, pixel);
        int g = pixel.get(1) & 0xFF;
        assertEquals(0, g, "Back-facing triangle should be culled and NOT drawn to framebuffer");
    }
}
