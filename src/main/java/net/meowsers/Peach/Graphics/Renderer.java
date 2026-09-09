package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Vertex;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.opengl.GL11.*;

public class Renderer {
    private static final List<RenderBatch> batches = new ArrayList<>();
    private static Shader shader;
    private static long windowHandle;
    private static Texture whiteTexture;
    private static int renderCallsAmount = 0;

    private static Color bgColor = Color.Black;

    private static Camera camera;
    public static final int MAX_LIGHTS = 16;
    private static final List<Light> lights = new ArrayList<>();
    private static boolean lightingEnabled = true;
    private static Color ambientColor = new Color(0.25f, 0.25f, 0.28f, 1.0f);
    private static float ambientIntensity = 0.35f;

    static {
        lights.add(new Light(new Vector3f(10.0f, 20.0f, 15.0f), new Color(1.0f, 1.0f, 1.0f, 1.0f), 1.0f));
    }

    private static boolean cullingEnabled = true;
    private static boolean normalCullingEnabled = false;
    private static int cullFace = GL_BACK;
    private static int frontFace = GL_CCW;

    private static final Matrix4f projectionMatrix = new Matrix4f();
    private static final Matrix4f viewMatrix = new Matrix4f().identity();

    private static final int[] texSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final Vector3f DEFAULT_LIGHT_DIR = new Vector3f(0.0f, -1.0f, 0.0f);
    private static final Matrix4f tempTransform = new Matrix4f();
    private static final Matrix4f tempCombined = new Matrix4f();

    public static void init(long window) {
        windowHandle = window;
        camera = null;
        lights.clear();
        batches.clear();
        renderCallsAmount = 0;
        cullingEnabled = true;
        normalCullingEnabled = false;
        cullFace = GL_BACK;
        frontFace = GL_CCW;
        // 1x1 default white texture at slot 0
        whiteTexture = new Texture(1, 1, Color.White);
        // Compiles default.slang into GLSL and links the program
        shader = new Shader("src/main/resources/Shaders/default.slang", "vertMain", "fragMain");
        updateProjection();
    }

    public static void clearBackground() {
        glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public static Color getBgColor() {
        return bgColor;
    }

    public static void setBgColor(Color bgColor) {
        Renderer.bgColor = bgColor;
    }

    public static int getRenderCallsAmount() {
        return renderCallsAmount;
    }

    public static Texture getWhiteTexture() {
        return whiteTexture;
    }

    public static Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public static Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public static Camera getCamera() {
        return camera;
    }

    public static void setCamera(Camera cam) {
        camera = cam;
    }

    public static Light getLight() {
        return lights.isEmpty() ? null : lights.get(0);
    }

    public static Light getLight(int index) {
        if (index >= 0 && index < lights.size()) {
            return lights.get(index);
        }
        return null;
    }

    public static List<Light> getLights() {
        return lights;
    }

    public static void setLight(Light l) {
        lights.clear();
        if (l != null) {
            lights.add(l);
        }
    }

    public static void setLight(int index, Light l) {
        if (index < 0) return;
        if (l == null) {
            if (index < lights.size()) {
                lights.remove(index);
            }
            return;
        }
        while (lights.size() <= index) {
            lights.add(null);
        }
        lights.set(index, l);
    }

    public static void addLight(Light l) {
        if (l != null && !lights.contains(l)) {
            lights.add(l);
        }
    }

    public static void removeLight(Light l) {
        if (l != null) {
            lights.remove(l);
        }
    }

    public static void clearLights() {
        lights.clear();
    }

    public static void setLights(List<Light> newLights) {
        lights.clear();
        if (newLights != null) {
            for (Light l : newLights) {
                if (l != null && !lights.contains(l)) {
                    lights.add(l);
                }
            }
        }
    }

    public static void setLights(Light... newLights) {
        lights.clear();
        if (newLights != null) {
            for (Light l : newLights) {
                if (l != null && !lights.contains(l)) {
                    lights.add(l);
                }
            }
        }
    }

    public static boolean isLightingEnabled() {
        return lightingEnabled;
    }

    public static void setLightingEnabled(boolean enabled) {
        lightingEnabled = enabled;
    }

    public static Color getAmbientColor() {
        return ambientColor;
    }

    public static void setAmbientColor(Color ambientColor) {
        if (ambientColor != null) {
            Renderer.ambientColor = new Color(ambientColor);
        }
    }

    public static float getAmbientIntensity() {
        return ambientIntensity;
    }

    public static void setAmbientIntensity(float ambientIntensity) {
        Renderer.ambientIntensity = ambientIntensity;
    }

    public static void setCulling(boolean enabled) {
        cullingEnabled = enabled;
    }

    public static void setBackfaceCulling(boolean enabled) {
        cullingEnabled = enabled;
    }

    public static boolean isCullingEnabled() {
        return cullingEnabled;
    }

    public static boolean isBackfaceCullingEnabled() {
        return cullingEnabled;
    }

    public static void enableCulling() {
        cullingEnabled = true;
    }

    public static void disableCulling() {
        cullingEnabled = false;
    }

    public static void enableBackfaceCulling() {
        cullingEnabled = true;
    }

    public static void disableBackfaceCulling() {
        cullingEnabled = false;
    }

    public static void setCullFace(int face) {
        cullFace = face;
    }

    public static int getCullFace() {
        return cullFace;
    }

    public static void setFrontFace(int mode) {
        frontFace = mode;
    }

    public static int getFrontFace() {
        return frontFace;
    }

    public static void setNormalCulling(boolean enabled) {
        normalCullingEnabled = enabled;
    }

    public static boolean isNormalCullingEnabled() {
        return normalCullingEnabled;
    }

    public static void enableNormalCulling() {
        normalCullingEnabled = true;
    }

    public static void disableNormalCulling() {
        normalCullingEnabled = false;
    }

    public static void handleCameraMovement(float dt, float moveSpeed, float mouseSensitivity) {
        if (camera != null) {
            camera.handleCameraMovement(dt, moveSpeed, mouseSensitivity);
        }
    }

    public static void handleCameraMovement(float dt) {
        if (camera != null) {
            camera.handleCameraMovement(dt);
        }
    }

    public static void setProjectionMatrix(Matrix4f matrix) {
        if (matrix != null) {
            projectionMatrix.set(matrix);
        }
    }

    public static void setViewMatrix(Matrix4f matrix) {
        if (matrix != null) {
            viewMatrix.set(matrix);
        }
    }

    public static void updateProjection() {
        if (camera != null) {
            projectionMatrix.set(camera.getProjectionMatrix());
            viewMatrix.set(camera.getViewMatrix());
            return;
        }

        if (windowHandle == 0) return;
        IntBuffer width = BufferUtils.createIntBuffer(1);
        IntBuffer height = BufferUtils.createIntBuffer(1);
        glfwGetWindowSize(windowHandle, width, height);

        // Sets up 2D orthographic projection matching screen pixel dimensions with depth range for 3D meshes
        projectionMatrix.identity().ortho(0.0f, width.get(0), height.get(0), 0.0f, -10000.0f, 10000.0f);
    }

    public static void addVerts(List<Vertex> verts, Texture texture) {
        addVertices(verts, texture);
    }

    public static void addVerts(List<Vertex> verts, List<Texture> textures) {
        addVertices(verts, textures);
    }

    public static void addVerts(List<Vertex> verts) {
        addVertices(verts, (List<Texture>) null);
    }

    public static void addVerts(Texture texture, Vertex... verts) {
        addVertices(Arrays.asList(verts), texture);
    }

    public static void addVerts(List<Texture> textures, Vertex... verts) {
        addVertices(Arrays.asList(verts), textures);
    }

    public static void addVerts(Vertex... verts) {
        addVertices(Arrays.asList(verts), (List<Texture>) null);
    }

    public static void addVertices(List<Vertex> verts, List<Texture> textures) {
        if (verts == null || verts.isEmpty()) return;

        int numVerts = verts.size();

        // 1. Try to find an existing batch with room
        for (RenderBatch batch : batches) {
            if (batch.hasRoom(numVerts, numVerts) && batch.hasTextures(textures)) {
                batch.addVertices(verts, textures);
                return;
            }
        }

        // 2. If it fits in a new batch
        if (numVerts <= RenderBatch.MAX_BATCH_SIZE) {
            RenderBatch addedBatch = new RenderBatch(whiteTexture);
            addedBatch.init();
            batches.add(addedBatch);
            addedBatch.addVertices(verts, textures);
            return;
        }

        // 3. Chunk across batches if larger than MAX_BATCH_SIZE
        int step = (numVerts % 4 == 0) ? 4 : 3;
        int chunkSize = (RenderBatch.MAX_BATCH_SIZE / step) * step;
        for (int i = 0; i < numVerts; i += chunkSize) {
            int end = Math.min(i + chunkSize, numVerts);
            addVertices(verts.subList(i, end), textures);
        }
    }

    public static void addVertices(List<Vertex> verts, Texture texture) {
        if (texture != null) {
            addVertices(verts, Collections.singletonList(texture));
        } else {
            addVertices(verts, (List<Texture>) null);
        }
    }

    public static void addVertices(List<Vertex> verts) {
        addVertices(verts, (List<Texture>) null);
    }

    public static void addVertices(Texture texture, Vertex... verts) {
        addVertices(Arrays.asList(verts), texture);
    }

    public static void addVertices(List<Texture> textures, Vertex... verts) {
        addVertices(Arrays.asList(verts), textures);
    }

    public static void addVertices(Vertex... verts) {
        addVertices(Arrays.asList(verts), (List<Texture>) null);
    }

    public static void drawMesh(Mesh mesh) {
        drawMesh(mesh, (List<Texture>) null, (Matrix4f) null);
    }

    public static void drawMesh(Mesh mesh, Texture texture) {
        if (texture != null) {
            drawMesh(mesh, Collections.singletonList(texture), (Matrix4f) null);
        } else {
            drawMesh(mesh, (List<Texture>) null, (Matrix4f) null);
        }
    }

    public static void drawMesh(Mesh mesh, List<Texture> textures) {
        drawMesh(mesh, textures, (Matrix4f) null);
    }

    public static void drawMesh(Mesh mesh, Texture... textures) {
        drawMesh(mesh, textures != null ? Arrays.asList(textures) : null, (Matrix4f) null);
    }

    public static void drawMesh(Mesh mesh, Matrix4f transform) {
        drawMesh(mesh, (List<Texture>) null, transform);
    }

    public static void drawMesh(Mesh mesh, Texture texture, Matrix4f transform) {
        if (texture != null) {
            drawMesh(mesh, Collections.singletonList(texture), transform);
        } else {
            drawMesh(mesh, (List<Texture>) null, transform);
        }
    }

    public static void drawMesh(Mesh mesh, Matrix4f transform, Texture... textures) {
        drawMesh(mesh, textures != null ? Arrays.asList(textures) : null, transform);
    }

    public static void drawMesh(Mesh mesh, List<Texture> textures, Matrix4f transform) {
        if (mesh == null || mesh.getVertices().isEmpty() || (windowHandle == 0 && batches.isEmpty())) return;

        List<Texture> effectiveTextures = textures != null && !textures.isEmpty()
                ? textures
                : (mesh.getTextures() != null && !mesh.getTextures().isEmpty()
                    ? mesh.getTextures()
                    : (mesh.getTexture() != null ? Collections.singletonList(mesh.getTexture()) : Collections.emptyList()));

        int numVerts = mesh.getVertices().size();
        int numIndices = mesh.getIndices().size();

        // 1. Try to add to an existing batch with room
        for (RenderBatch batch : batches) {
            if (batch.hasRoom(numVerts, numIndices) && batch.hasTextures(effectiveTextures)) {
                batch.addMesh(mesh, effectiveTextures, transform);
                return;
            }
        }

        // 2. If it fits in a single fresh batch
        if (numVerts <= RenderBatch.MAX_BATCH_SIZE && numIndices <= RenderBatch.MAX_INDICES) {
            RenderBatch addedBatch = new RenderBatch(whiteTexture);
            addedBatch.init();
            batches.add(addedBatch);
            addedBatch.addMesh(mesh, effectiveTextures, transform);
            return;
        }

        // 3. Large mesh: chunk across batches
        drawLargeMeshChunked(mesh, effectiveTextures, transform);
    }

    private static void drawLargeMeshChunked(Mesh mesh, List<Texture> textures, Matrix4f transform) {
        List<Vertex> verts = mesh.getVertices();
        List<Integer> indices = mesh.getIndices();

        if (indices.isEmpty()) {
            // Unindexed mesh chunking
            int totalVerts = verts.size();
            int step = (totalVerts % 4 == 0) ? 4 : 3;
            int chunkSize = (RenderBatch.MAX_BATCH_SIZE / step) * step;

            for (int i = 0; i < totalVerts; i += chunkSize) {
                int end = Math.min(i + chunkSize, totalVerts);
                List<Vertex> subVerts = verts.subList(i, end);
                Mesh chunkMesh = new Mesh(mesh.getName() + "_Chunk");
                chunkMesh.setColor(mesh.getColor());
                chunkMesh.setTextures(textures);
                chunkMesh.setVertices(subVerts);
                drawMesh(chunkMesh, textures, transform);
            }
        } else {
            // Indexed triangle mesh chunking
            int totalIndices = indices.size();
            int totalTriangles = totalIndices / 3;

            Map<Integer, Integer> oldToNew = new HashMap<>();
            List<Vertex> chunkVerts = new ArrayList<>();
            List<Integer> chunkIndices = new ArrayList<>();

            int maxVertsPerChunk = RenderBatch.MAX_BATCH_SIZE - 3;
            int maxIndicesPerChunk = RenderBatch.MAX_INDICES - 3;

            for (int t = 0; t < totalTriangles; t++) {
                int i0 = indices.get(t * 3 + 0);
                int i1 = indices.get(t * 3 + 1);
                int i2 = indices.get(t * 3 + 2);

                int neededVerts = (oldToNew.containsKey(i0) ? 0 : 1) +
                        (oldToNew.containsKey(i1) ? 0 : 1) +
                        (oldToNew.containsKey(i2) ? 0 : 1);

                if (chunkVerts.size() + neededVerts > maxVertsPerChunk || chunkIndices.size() + 3 > maxIndicesPerChunk) {
                    if (!chunkVerts.isEmpty()) {
                        Mesh chunkMesh = new Mesh(mesh.getName() + "_Chunk");
                        chunkMesh.setColor(mesh.getColor());
                        chunkMesh.setTextures(textures);
                        chunkMesh.add(chunkVerts, chunkIndices);
                        drawMesh(chunkMesh, textures, transform);
                    }
                    oldToNew.clear();
                    chunkVerts.clear();
                    chunkIndices.clear();
                }

                for (int oldIdx : new int[]{i0, i1, i2}) {
                    Integer newIdx = oldToNew.get(oldIdx);
                    if (newIdx == null) {
                        newIdx = chunkVerts.size();
                        oldToNew.put(oldIdx, newIdx);
                        chunkVerts.add(verts.get(oldIdx));
                    }
                    chunkIndices.add(newIdx);
                }
            }

            if (!chunkVerts.isEmpty()) {
                Mesh chunkMesh = new Mesh(mesh.getName() + "_Chunk");
                chunkMesh.setColor(mesh.getColor());
                chunkMesh.setTextures(textures);
                chunkMesh.add(chunkVerts, chunkIndices);
                drawMesh(chunkMesh, textures, transform);
            }
        }
    }

    public static void drawMesh(Mesh mesh, Vector3f position) {
        drawMesh(mesh, (List<Texture>) null, position, null, null);
    }

    public static void drawMesh(Mesh mesh, Texture texture, Vector3f position) {
        drawMesh(mesh, texture, position, null, null);
    }

    public static void drawMesh(Mesh mesh, List<Texture> textures, Vector3f position) {
        drawMesh(mesh, textures, position, null, null);
    }

    public static void drawMesh(Mesh mesh, Vector3f position, Vector3f rotation, Vector3f scale) {
        drawMesh(mesh, (List<Texture>) null, position, rotation, scale);
    }

    public static void drawMesh(Mesh mesh, Texture texture, Vector3f position, Vector3f rotation, Vector3f scale) {
        if (texture != null) {
            drawMesh(mesh, Collections.singletonList(texture), position, rotation, scale);
        } else {
            drawMesh(mesh, (List<Texture>) null, position, rotation, scale);
        }
    }

    public static void drawMesh(Mesh mesh, List<Texture> textures, Vector3f position, Vector3f rotation, Vector3f scale) {
        tempTransform.identity();
        if (position != null) tempTransform.translate(position);
        if (rotation != null) {
            tempTransform.rotate((float) Math.toRadians(rotation.x), 1, 0, 0);
            tempTransform.rotate((float) Math.toRadians(rotation.y), 0, 1, 0);
            tempTransform.rotate((float) Math.toRadians(rotation.z), 0, 0, 1);
        }
        if (scale != null) tempTransform.scale(scale);
        drawMesh(mesh, textures, tempTransform);
    }

    public static void drawMesh(Mesh mesh, Vector3f position, Vector3f rotation, Vector3f scale, Texture... textures) {
        drawMesh(mesh, textures != null ? Arrays.asList(textures) : null, position, rotation, scale);
    }

    public static void drawModel(Model model) {
        drawModel(model, (List<Texture>) null, (Matrix4f) null);
    }

    public static void drawModel(Model model, Matrix4f transform) {
        drawModel(model, (List<Texture>) null, transform);
    }

    public static void drawModel(Model model, Texture texture) {
        if (texture != null) {
            drawModel(model, Collections.singletonList(texture), (Matrix4f) null);
        } else {
            drawModel(model, (List<Texture>) null, (Matrix4f) null);
        }
    }

    public static void drawModel(Model model, Texture texture, Matrix4f transform) {
        if (texture != null) {
            drawModel(model, Collections.singletonList(texture), transform);
        } else {
            drawModel(model, (List<Texture>) null, transform);
        }
    }

    public static void drawModel(Model model, List<Texture> textures) {
        drawModel(model, textures, (Matrix4f) null);
    }

    public static void drawModel(Model model, Texture... textures) {
        drawModel(model, textures != null ? Arrays.asList(textures) : null, (Matrix4f) null);
    }

    public static void drawModel(Model model, Matrix4f transform, Texture... textures) {
        drawModel(model, textures != null ? Arrays.asList(textures) : null, transform);
    }

    public static void drawModel(Model model, List<Texture> textures, Matrix4f transform) {
        if (model == null) return;

        Matrix4f modelMat = model.getModelMatrix();
        Matrix4f combined = transform != null ? tempCombined.set(transform).mul(modelMat) : modelMat;

        List<Mesh> meshes = model.getMeshes();
        for (int i = 0; i < meshes.size(); i++) {
            Mesh mesh = meshes.get(i);
            Texture tex = null;
            if (textures != null && !textures.isEmpty()) {
                if (i < textures.size() && textures.get(i) != null) {
                    tex = textures.get(i);
                } else if (textures.size() == 1) {
                    tex = textures.get(0);
                }
            }
            if (tex == null) {
                tex = mesh.getTexture() != null ? mesh.getTexture() : model.getTexture(i);
            }
            drawMesh(mesh, tex, combined);
        }
    }

    public static void drawModel(Model model, Vector3f position, Vector3f rotation, Vector3f scale) {
        drawModel(model, (List<Texture>) null, position, rotation, scale);
    }

    public static void drawModel(Model model, Texture texture, Vector3f position, Vector3f rotation, Vector3f scale) {
        if (texture != null) {
            drawModel(model, Collections.singletonList(texture), position, rotation, scale);
        } else {
            drawModel(model, (List<Texture>) null, position, rotation, scale);
        }
    }

    public static void drawModel(Model model, List<Texture> textures, Vector3f position, Vector3f rotation, Vector3f scale) {
        tempTransform.identity();
        if (position != null) tempTransform.translate(position);
        if (rotation != null) {
            tempTransform.rotate((float) Math.toRadians(rotation.x), 1, 0, 0);
            tempTransform.rotate((float) Math.toRadians(rotation.y), 0, 1, 0);
            tempTransform.rotate((float) Math.toRadians(rotation.z), 0, 0, 1);
        }
        if (scale != null) tempTransform.scale(scale);
        drawModel(model, textures, tempTransform);
    }

    public static void drawModel(Model model, Vector3f position, Vector3f rotation, Vector3f scale, Texture... textures) {
        drawModel(model, textures != null ? Arrays.asList(textures) : null, position, rotation, scale);
    }

    public static void render() {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        clearBackground();
        if (shader == null) {
            renderCallsAmount = 0;
            return;
        }

        updateProjection(); // Automatically updates projection if window resized

        shader.use();
        shader.uploadMatrix4f("uProjection", projectionMatrix);
        shader.uploadMatrix4f("uView", viewMatrix);

        shader.uploadIntArray("uTextures", texSlots);

        shader.uploadInt("uNormalCulling", normalCullingEnabled ? 1 : 0);
        if (camera != null) {
            shader.uploadVec3f("uCameraPos", camera.getPosition());
        } else {
            shader.uploadVec3f("uCameraPos", 0.0f, 0.0f, 1000.0f);
        }

        boolean useLighting = lightingEnabled && !lights.isEmpty() && (camera != null);
        if (useLighting) {
            int numLights = Math.min(lights.size(), MAX_LIGHTS);
            shader.uploadInt("uUseLighting", 1);
            shader.uploadInt("uNumLights", numLights);

            Light firstLight = !lights.isEmpty() ? lights.get(0) : null;
            Color aColor = (firstLight != null && firstLight.getAmbientColor() != null) ? firstLight.getAmbientColor() : ambientColor;
            float aIntensity = firstLight != null ? firstLight.getAmbientIntensity() : ambientIntensity;
            shader.uploadVec3f("uAmbientColor", aColor.r, aColor.g, aColor.b);
            shader.uploadFloat("uAmbientIntensity", aIntensity);

            for (int i = 0; i < numLights; i++) {
                Light l = lights.get(i);
                if (l == null) continue;
                shader.uploadInt("uLightType[" + i + "]", l.getType() != null ? l.getType().getId() : (l.isDirectional() ? 1 : 0));
                shader.uploadInt("uIsDirectional[" + i + "]", l.isDirectional() ? 1 : 0);
                shader.uploadVec3f("uLightPos[" + i + "]", l.getPosition());
                shader.uploadVec3f("uLightDir[" + i + "]", l.getDirection() != null ? l.getDirection() : DEFAULT_LIGHT_DIR);
                shader.uploadFloat("uLightIntensity[" + i + "]", l.getIntensity());
                Color lColor = l.getColor() != null ? l.getColor() : Color.White;
                shader.uploadVec4f("uLightColor[" + i + "]", lColor.r, lColor.g, lColor.b, lColor.a);
                shader.uploadFloat("uSpecularIntensity[" + i + "]", l.getSpecularIntensity());
                shader.uploadFloat("uShininess[" + i + "]", l.getShininess());
                shader.uploadFloat("uCutOff[" + i + "]", l.getCutOff());
                shader.uploadFloat("uOuterCutOff[" + i + "]", l.getOuterCutOff());
            }
        } else {
            shader.uploadInt("uUseLighting", 0);
            shader.uploadInt("uNumLights", 0);
        }

        int drawCalls = 0;
        for (int i = 0; i < batches.size(); i++) {
            RenderBatch batch = batches.get(i);
            if (batch.flush()) {
                drawCalls++;
            }
            batch.reset();
        }
        renderCallsAmount = drawCalls;

        shader.detach();
    }
}