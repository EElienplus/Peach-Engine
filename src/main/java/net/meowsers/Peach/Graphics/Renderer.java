package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Vertex;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glFramebufferTextureLayer;

public class Renderer {
    private static final List<RenderBatch> batches = new ArrayList<>();
    private static Shader shader;
    private static Shader shadowShader;

    private static boolean shadowsEnabled = true;
    private static int shadowMapResolution = 1024;
    private static float shadowDistance = 100.0f;
    private static float shadowNearPlane = 0.1f;
    private static float shadowFarPlane = 100.0f;
    private static float shadowBias = 0.0025f;
    private static float shadowNormalBias = 0.01f;

    private static int shadowFbo = 0;
    private static int shadowMapTexture = 0;
    private static int shadowDepthBuffer = 0;
    private static boolean shadowResourcesReady = false;
    private static final Matrix4f shadowViewProjection = new Matrix4f();
    private static final Matrix4f shadowProjection = new Matrix4f();
    private static final Matrix4f shadowView = new Matrix4f();
    private static final Matrix4f[] pointShadowMatrices = new Matrix4f[6];
    private static final Vector3f shadowUp = new Vector3f();
    private static final Vector3f shadowTarget = new Vector3f();
    private static final Vector3f shadowEye = new Vector3f();
    private static final Vector3f shadowDirection = new Vector3f();
    private static final int MATERIAL_TEXTURE_SLOTS = 15;
    private static final int SHADOW_TEXTURE_UNIT = 15;

    static {
        for (int i = 0; i < pointShadowMatrices.length; i++) {
            pointShadowMatrices[i] = new Matrix4f();
        }
    }
    private static long windowHandle;
    private static Texture whiteTexture;
    private static int renderCallsAmount = 0;
    private static RenderBatch lastBatch;

    private static Color bgColor = Color.Black;

    private static Camera camera;
    public static final int MAX_LIGHTS = 16;
    private static final List<Light> lights = new ArrayList<>();
    private static boolean lightingEnabled = true;
    private static Color ambientColor = new Color(0.25f, 0.25f, 0.28f, 1.0f);
    private static float ambientIntensity = 0.35f;

    static {
        lights.add(new Light(
                new Vector3f(10.0f, 20.0f, 15.0f),
                new Color(1.0f, 1.0f, 1.0f, 1.0f),
                1.0f
        ));
    }

    private static boolean cullingEnabled = true;
    private static boolean normalCullingEnabled = false;
    private static int cullFace = GL_BACK;
    private static int frontFace = GL_CCW;

    private static final Matrix4f projectionMatrix = new Matrix4f();
    private static final Matrix4f viewMatrix = new Matrix4f().identity();
    private static final Matrix4f identityMatrix = new Matrix4f().identity();
    private static final Matrix4f frustumMatrix = new Matrix4f();

    private static final float[] frustumPlanes = new float[24];

    private static final int[] texSlots = {
            0, 1, 2, 3, 4,
            5, 6, 7, 8, 9,
            10, 11, 12, 13, 14
    };

    private static final Vector3f DEFAULT_LIGHT_DIR =
            new Vector3f(0.0f, -1.0f, 0.0f);

    private static final Vector3f tempLightDir = new Vector3f();
    private static final Matrix4f tempTransform = new Matrix4f();
    private static final Matrix4f tempCombined = new Matrix4f();

    private static final IntBuffer windowWidthBuffer =
            BufferUtils.createIntBuffer(1);
    private static final IntBuffer windowHeightBuffer =
            BufferUtils.createIntBuffer(1);

    /*
     * Dense meshes use their own persistent GPU buffers.
     *
     * This is deliberately below RenderBatch.MAX_BATCH_SIZE so large/complex
     * meshes avoid RenderBatch's per-frame vertex transformation and VBO/EBO
     * uploads altogether.
     */
    private static final int DIRECT_RENDER_VERTEX_THRESHOLD = 16000;
    private static final int DIRECT_RENDER_INDEX_THRESHOLD = 75000;

    private static final IdentityHashMap<Mesh, GpuMesh> gpuMeshes =
            new IdentityHashMap<>();

    private static final List<DirectDraw> directDraws =
            new ArrayList<>();

    private static final int[] currentlyBoundDirectTextures =
            new int[16];

    private static final boolean[] directTextureWasBound =
            new boolean[16];

    private static final class Bounds {
        final List<Vertex> sourceVertices;
        final int vertexCount;
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;
        final long fingerprint;

        Bounds(
                List<Vertex> sourceVertices,
                int vertexCount,
                float minX,
                float minY,
                float minZ,
                float maxX,
                float maxY,
                float maxZ,
                long fingerprint
        ) {
            this.sourceVertices = sourceVertices;
            this.vertexCount = vertexCount;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.fingerprint = fingerprint;
        }
    }

    private static final IdentityHashMap<Mesh, Bounds> boundsCache =
            new IdentityHashMap<>();

    private static final class DirectDraw {
        final Mesh mesh;
        final List<Texture> textures;
        final Matrix4f transform;
        final boolean shadowOnly;

        DirectDraw(
                Mesh mesh,
                List<Texture> textures,
                Matrix4f transform,
                boolean shadowOnly
        ) {
            this.mesh = mesh;
            this.textures = textures;
            this.transform = transform;
            this.shadowOnly = shadowOnly;
        }
    }

    private static final class GpuMesh {
        int vao;
        int vbo;
        int ebo;

        int vertexCount;
        int indexCount;

        long geometryFingerprint = Long.MIN_VALUE;
        int cachedVertexCount = -1;
        int cachedIndexCount = -1;

        boolean hasCachedTransform = false;
        final Matrix4f cachedTransform = new Matrix4f();

        int cachedTextureCount = -1;
        final int[] cachedTextureIds = new int[MATERIAL_TEXTURE_SLOTS];

        boolean uploaded;

        final Texture[] textures = new Texture[MATERIAL_TEXTURE_SLOTS];
        int textureCount = 1;

        void ensureUploaded(
                Mesh mesh,
                List<Texture> requestedTextures,
                Matrix4f transform,
                Texture white
        ) {
            List<Vertex> vertices = mesh.getVertices();
            List<Integer> meshIndices = mesh.getIndices();

            long geometryFingerprint =
                    calculateGeometryFingerprint(
                            vertices,
                            meshIndices
                    );

            boolean sameGeometry =
                    uploaded
                    && cachedVertexCount == vertices.size()
                    && cachedIndexCount == meshIndices.size()
                    && this.geometryFingerprint == geometryFingerprint;

            boolean sameTextures =
                    sameTextures(requestedTextures, white);

            boolean sameTransform =
                    (transform == null && !hasCachedTransform)
                    || (transform != null
                        && hasCachedTransform
                        && cachedTransform.equals(transform));

            if (sameGeometry && sameTextures && sameTransform) {
                return;
            }

            if (!uploaded) {
                vao = glGenVertexArrays();
                vbo = glGenBuffers();
                ebo = glGenBuffers();
                uploaded = true;
            }

            this.vertexCount = vertices.size();

            int actualIndexCount;

            if (meshIndices != null && !meshIndices.isEmpty()) {
                actualIndexCount = meshIndices.size();
            } else if (vertices.size() % 4 == 0) {
                actualIndexCount = (vertices.size() / 4) * 6;
            } else {
                actualIndexCount = vertices.size();
            }

            this.indexCount = actualIndexCount;

            prepareTextureSlots(
                    requestedTextures,
                    white
            );

            FloatBuffer vertexBuffer =
                    BufferUtils.createFloatBuffer(
                            vertices.size()
                                    * Vertex.ELEMENT_SIZE
                    );

            Color meshColor = mesh.getColor();
            boolean useMeshColor =
                    meshColor != null;

            Vector3f transformedPos =
                    new Vector3f();
            Vector3f transformedNormal =
                    new Vector3f();

            for (
                    int i = 0;
                    i < vertices.size();
                    i++
            ) {
                Vertex v = vertices.get(i);

                float vx = v.x;
                float vy = v.y;
                float vz = v.z;

                float vnx = v.nx;
                float vny = v.ny;
                float vnz = v.nz;

                /*
                 * The existing shader expects world-space positions/normals
                 * because it has no model matrix. Bake the transform only when
                 * this cached GPU representation is created/changed.
                 */
                if (transform != null) {
                    transformedPos.set(
                            vx,
                            vy,
                            vz
                    );

                    transform.transformPosition(
                            transformedPos
                    );

                    vx = transformedPos.x;
                    vy = transformedPos.y;
                    vz = transformedPos.z;

                    transformedNormal.set(
                            vnx,
                            vny,
                            vnz
                    );

                    transform.transformDirection(
                            transformedNormal
                    );

                    float normalLengthSq =
                            transformedNormal.lengthSquared();

                    if (normalLengthSq > 0.000001f) {
                        transformedNormal.mul(
                                (float) (
                                        1.0
                                        / Math.sqrt(
                                                normalLengthSq
                                        )
                                )
                        );
                    } else {
                        transformedNormal.set(
                                0.0f,
                                0.0f,
                                1.0f
                        );
                    }

                    vnx = transformedNormal.x;
                    vny = transformedNormal.y;
                    vnz = transformedNormal.z;
                }

                float r = v.r;
                float g = v.g;
                float b = v.b;
                float a = v.a;

                if (useMeshColor
                        && r == 1.0f
                        && g == 1.0f
                        && b == 1.0f
                        && a == 1.0f) {

                    r = meshColor.r;
                    g = meshColor.g;
                    b = meshColor.b;
                    a = meshColor.a;
                }

                int rawTexId =
                        (int) v.texID;

                int mappedTexId =
                        mapTextureSlot(
                                requestedTextures,
                                rawTexId
                        );

                vertexBuffer.put(vx);
                vertexBuffer.put(vy);
                vertexBuffer.put(vz);

                vertexBuffer.put(vnx);
                vertexBuffer.put(vny);
                vertexBuffer.put(vnz);

                vertexBuffer.put(r);
                vertexBuffer.put(g);
                vertexBuffer.put(b);
                vertexBuffer.put(a);

                vertexBuffer.put(v.u);
                vertexBuffer.put(v.v);

                vertexBuffer.put(
                        (float) mappedTexId
                );
            }

            vertexBuffer.flip();

            IntBuffer indexBuffer =
                    BufferUtils.createIntBuffer(
                            actualIndexCount
                    );

            if (meshIndices != null
                    && !meshIndices.isEmpty()) {

                for (int idx : meshIndices) {
                    indexBuffer.put(idx);
                }

            } else if (vertices.size() % 4 == 0) {

                for (
                        int i = 0;
                        i < vertices.size();
                        i += 4
                ) {
                    indexBuffer.put(i);
                    indexBuffer.put(i + 1);
                    indexBuffer.put(i + 2);

                    indexBuffer.put(i + 2);
                    indexBuffer.put(i + 3);
                    indexBuffer.put(i);
                }

            } else {
                for (
                        int i = 0;
                        i < vertices.size();
                        i++
                ) {
                    indexBuffer.put(i);
                }
            }

            indexBuffer.flip();

            glBindVertexArray(vao);

            glBindBuffer(
                    GL_ARRAY_BUFFER,
                    vbo
            );

            glBufferData(
                    GL_ARRAY_BUFFER,
                    vertexBuffer,
                    GL_STATIC_DRAW
            );

            glBindBuffer(
                    GL_ELEMENT_ARRAY_BUFFER,
                    ebo
            );

            glBufferData(
                    GL_ELEMENT_ARRAY_BUFFER,
                    indexBuffer,
                    GL_STATIC_DRAW
            );

            setupVertexLayout();

            glBindVertexArray(0);

            this.geometryFingerprint =
                    geometryFingerprint;

            this.cachedVertexCount =
                    vertices.size();

            this.cachedIndexCount =
                    meshIndices.size();

            if (transform != null) {
                cachedTransform.set(transform);
                hasCachedTransform = true;
            } else {
                hasCachedTransform = false;
            }
        }

        private void prepareTextureSlots(
                List<Texture> requestedTextures,
                Texture white
        ) {
            Arrays.fill(
                    textures,
                    null
            );

            Arrays.fill(
                    cachedTextureIds,
                    Integer.MIN_VALUE
            );

            textures[0] = white;
            textureCount = 1;

            if (requestedTextures == null
                    || requestedTextures.isEmpty()) {

                cachedTextureCount = 1;
                cachedTextureIds[0] =
                        white != null
                                ? white.getId()
                                : -1;

                return;
            }

            /*
             * Keep the same effective behavior as RenderBatch:
             * white is always slot 0, real textures start at slot 1.
             */
            for (
                    int i = 0;
                    i < requestedTextures.size()
                            && i < MATERIAL_TEXTURE_SLOTS;
                    i++
            ) {
                Texture texture =
                        requestedTextures.get(i);

                if (texture == null) {
                    continue;
                }

                if (white != null
                        && texture.getId()
                            == white.getId()) {
                    continue;
                }

                int existing = findTexture(
                        texture
                );

                if (existing >= 0) {
                    continue;
                }

                if (textureCount >= MATERIAL_TEXTURE_SLOTS) {
                    break;
                }

                textures[textureCount++] =
                        texture;
            }

            cachedTextureCount =
                    textureCount;

            for (int i = 0; i < textureCount; i++) {
                cachedTextureIds[i] =
                        textures[i] != null
                                ? textures[i].getId()
                                : -1;
            }
        }

        private boolean sameTextures(
                List<Texture> requestedTextures,
                Texture white
        ) {
            /*
             * Build a cheap signature without rebuilding the GPU buffer if
             * the material is unchanged.
             */
            int[] ids = new int[MATERIAL_TEXTURE_SLOTS];
            Arrays.fill(
                    ids,
                    Integer.MIN_VALUE
            );

            ids[0] =
                    white != null
                            ? white.getId()
                            : -1;

            int count = 1;

            if (requestedTextures != null) {
                for (
                        Texture texture :
                        requestedTextures
                ) {
                    if (texture == null) {
                        continue;
                    }

                    int id =
                            texture.getId();

                    if (white != null
                            && id == white.getId()) {
                        continue;
                    }

                    boolean duplicate = false;

                    for (int i = 1; i < count; i++) {
                        if (ids[i] == id) {
                            duplicate = true;
                            break;
                        }
                    }

                    if (duplicate) {
                        continue;
                    }

                    if (count >= MATERIAL_TEXTURE_SLOTS) {
                        return false;
                    }

                    ids[count++] = id;
                }
            }

            if (count != cachedTextureCount) {
                return false;
            }

            for (int i = 0; i < count; i++) {
                if (ids[i] != cachedTextureIds[i]) {
                    return false;
                }
            }

            return true;
        }

        private int findTexture(Texture texture) {
            if (texture == null) {
                return 0;
            }

            int id = texture.getId();

            for (int i = 1; i < textureCount; i++) {
                Texture existing =
                        textures[i];

                if (existing != null
                        && existing.getId() == id) {
                    return i;
                }
            }

            return -1;
        }

        private int mapTextureSlot(
                List<Texture> requestedTextures,
                int rawTexId
        ) {
            if (requestedTextures == null
                    || requestedTextures.isEmpty()) {
                return 0;
            }

            /*
             * A single override texture replaces all source texture IDs,
             * matching RenderBatch's behavior.
             */
            if (requestedTextures.size() == 1) {
                Texture target = requestedTextures.get(0);
                if (target == null) return 0;
                int slot = findTexture(target);
                return slot >= 0 ? slot : 0;
            }

            if (rawTexId < 0
                    || rawTexId >= requestedTextures.size()) {
                return 0;
            }

            Texture target =
                    requestedTextures.get(rawTexId);

            if (target == null) {
                return 0;
            }

            if (textures[0] != null
                    && target.getId()
                        == textures[0].getId()) {
                return 0;
            }

            for (int i = 1; i < textureCount; i++) {
                if (textures[i] != null
                        && textures[i].getId()
                            == target.getId()) {
                    return i;
                }
            }

            return 0;
        }

        void destroy() {
            if (!uploaded) {
                return;
            }

            if (vao != 0) {
                glDeleteVertexArrays(vao);
            }

            if (vbo != 0) {
                glDeleteBuffers(vbo);
            }

            if (ebo != 0) {
                glDeleteBuffers(ebo);
            }

            vao = 0;
            vbo = 0;
            ebo = 0;
            uploaded = false;
        }
    }

    public static void init(long window) {
        destroyGpuMeshes();

        windowHandle = window;
        camera = null;

        lights.clear();
        batches.clear();
        directDraws.clear();

        lastBatch = null;

        Arrays.fill(
                currentlyBoundDirectTextures,
                0
        );

        Arrays.fill(
                directTextureWasBound,
                false
        );

        renderCallsAmount = 0;

        cullingEnabled = true;
        normalCullingEnabled = false;
        cullFace = GL_BACK;
        frontFace = GL_CCW;

        whiteTexture =
                new Texture(
                        1,
                        1,
                        Color.White
                );

        shader =
                new Shader(
                        "src/main/resources/Shaders/default.slang",
                        "vertMain",
                        "fragMain"
                );

        shadowShader =
                new Shader(
                        "src/main/resources/Shaders/shadow.slang",
                        "vertMain",
                        "fragMain"
                );

        createShadowResources();
        updateProjection();
    }

    public static boolean isShadowsEnabled() {
        return shadowsEnabled;
    }

    public static void setShadowsEnabled(boolean enabled) {
        shadowsEnabled = enabled;
    }

    public static int getShadowMapResolution() {
        return shadowMapResolution;
    }

    public static void setShadowMapResolution(int resolution) {
        int clamped = Math.max(256, Math.min(4096, resolution));
        if (shadowMapResolution == clamped) return;
        shadowMapResolution = clamped;
        createShadowResources();
    }

    public static float getShadowDistance() {
        return shadowDistance;
    }

    public static void setShadowDistance(float distance) {
        shadowDistance = Math.max(1.0f, distance);
    }

    public static float getShadowNearPlane() {
        return shadowNearPlane;
    }

    public static void setShadowNearPlane(float nearPlane) {
        shadowNearPlane = Math.max(0.001f, nearPlane);
    }

    public static float getShadowFarPlane() {
        return shadowFarPlane;
    }

    public static void setShadowFarPlane(float farPlane) {
        shadowFarPlane = Math.max(shadowNearPlane + 0.01f, farPlane);
    }

    public static float getShadowBias() {
        return shadowBias;
    }

    public static void setShadowBias(float bias) {
        shadowBias = Math.max(0.0f, bias);
    }

    public static float getShadowNormalBias() {
        return shadowNormalBias;
    }

    public static void setShadowNormalBias(float bias) {
        shadowNormalBias = Math.max(0.0f, bias);
    }

    public static void clearBackground() {
        glClearColor(
                bgColor.r,
                bgColor.g,
                bgColor.b,
                bgColor.a
        );

        glClear(
                GL_COLOR_BUFFER_BIT
                        | GL_DEPTH_BUFFER_BIT
        );
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
        return lights.isEmpty()
                ? null
                : lights.get(0);
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

    public static void setLight(
            int index,
            Light l
    ) {
        if (index < 0) {
            return;
        }

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

    public static void setLights(
            List<Light> newLights
    ) {
        lights.clear();

        if (newLights != null) {
            for (Light l : newLights) {
                if (l != null && !lights.contains(l)) {
                    lights.add(l);
                }
            }
        }
    }

    public static void setLights(
            Light... newLights
    ) {
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

    public static void setLightingEnabled(
            boolean enabled
    ) {
        lightingEnabled = enabled;
    }

    public static Color getAmbientColor() {
        return ambientColor;
    }

    public static void setAmbientColor(
            Color ambientColor
    ) {
        if (ambientColor != null) {
            Renderer.ambientColor =
                    new Color(ambientColor);
        }
    }

    public static float getAmbientIntensity() {
        return ambientIntensity;
    }

    public static void setAmbientIntensity(
            float ambientIntensity
    ) {
        Renderer.ambientIntensity =
                ambientIntensity;
    }

    public static void setCulling(
            boolean enabled
    ) {
        cullingEnabled = enabled;
    }

    public static void setBackfaceCulling(
            boolean enabled
    ) {
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

    public static void setNormalCulling(
            boolean enabled
    ) {
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

    public static void handleCameraMovement(
            float dt,
            float moveSpeed,
            float mouseSensitivity
    ) {
        if (camera != null) {
            camera.handleCameraMovement(
                    dt,
                    moveSpeed,
                    mouseSensitivity
            );
        }
    }

    public static void handleCameraMovement(
            float dt
    ) {
        if (camera != null) {
            camera.handleCameraMovement(dt);
        }
    }

    public static void setProjectionMatrix(
            Matrix4f matrix
    ) {
        if (matrix != null) {
            projectionMatrix.set(matrix);
        }
    }

    public static void setViewMatrix(
            Matrix4f matrix
    ) {
        if (matrix != null) {
            viewMatrix.set(matrix);
        }
    }

    public static void updateProjection() {
        if (camera != null) {
            projectionMatrix.set(
                    camera.getProjectionMatrix()
            );
            viewMatrix.set(
                    camera.getViewMatrix()
            );
            return;
        }

        if (windowHandle == 0) {
            return;
        }

        windowWidthBuffer.clear();
        windowHeightBuffer.clear();

        glfwGetFramebufferSize(
                windowHandle,
                windowWidthBuffer,
                windowHeightBuffer
        );

        int width =
                windowWidthBuffer.get(0);

        int height =
                windowHeightBuffer.get(0);

        if (width <= 0 || height <= 0) {
            return;
        }

        projectionMatrix
                .identity()
                .ortho(
                        0.0f,
                        width,
                        height,
                        0.0f,
                        -10000.0f,
                        10000.0f
                );
    }

    public static void addVerts(
            List<Vertex> verts,
            Texture texture
    ) {
        addVertices(verts, texture);
    }

    public static void addVerts(
            List<Vertex> verts,
            List<Texture> textures
    ) {
        addVertices(verts, textures);
    }

    public static void addVerts(
            List<Vertex> verts
    ) {
        addVertices(
                verts,
                (List<Texture>) null
        );
    }

    public static void addVerts(
            Texture texture,
            Vertex... verts
    ) {
        addVertices(
                Arrays.asList(verts),
                texture
        );
    }

    public static void addVerts(
            List<Texture> textures,
            Vertex... verts
    ) {
        addVertices(
                Arrays.asList(verts),
                textures
        );
    }

    public static void addVerts(
            Vertex... verts
    ) {
        addVertices(
                Arrays.asList(verts),
                (List<Texture>) null
        );
    }

    public static void addVertices(
            List<Vertex> verts,
            List<Texture> textures
    ) {
        if (verts == null || verts.isEmpty()) {
            return;
        }

        int numVerts = verts.size();

        if (lastBatch != null
                && lastBatch.hasRoom(
                        numVerts,
                        numVerts
                )
                && lastBatch.hasTextures(
                        textures
                )) {

            lastBatch.addVertices(
                    verts,
                    textures
            );

            return;
        }

        for (
                int i = batches.size() - 1;
                i >= 0;
                i--
        ) {
            RenderBatch batch =
                    batches.get(i);

            if (batch.hasRoom(
                    numVerts,
                    numVerts
            ) && batch.hasTextures(
                    textures
            )) {

                batch.addVertices(
                        verts,
                        textures
                );

                lastBatch = batch;
                return;
            }
        }

        if (numVerts <= RenderBatch.MAX_BATCH_SIZE) {
            RenderBatch batch =
                    new RenderBatch(whiteTexture);

            batch.init();
            batch.addVertices(
                    verts,
                    textures
            );

            batches.add(batch);
            lastBatch = batch;
            return;
        }

        int primitiveStep =
                numVerts % 4 == 0
                        ? 4
                        : 3;

        int chunkSize =
                Math.max(
                        primitiveStep,
                        (RenderBatch.MAX_BATCH_SIZE
                                / primitiveStep)
                                * primitiveStep
                );

        for (
                int start = 0;
                start < numVerts;
                start += chunkSize
        ) {
            int end =
                    Math.min(
                            start + chunkSize,
                            numVerts
                    );

            addVertices(
                    verts.subList(
                            start,
                            end
                    ),
                    textures
            );
        }
    }

    public static void addVertices(
            List<Vertex> verts,
            Texture texture
    ) {
        if (texture != null) {
            addVertices(
                    verts,
                    Collections.singletonList(
                            texture
                    )
            );
        } else {
            addVertices(
                    verts,
                    (List<Texture>) null
            );
        }
    }

    public static void addVertices(
            List<Vertex> verts
    ) {
        addVertices(
                verts,
                (List<Texture>) null
        );
    }

    public static void addVertices(
            Texture texture,
            Vertex... verts
    ) {
        addVertices(
                Arrays.asList(verts),
                texture
        );
    }

    public static void addVertices(
            List<Texture> textures,
            Vertex... verts
    ) {
        addVertices(
                Arrays.asList(verts),
                textures
        );
    }

    public static void addVertices(
            Vertex... verts
    ) {
        addVertices(
                Arrays.asList(verts),
                (List<Texture>) null
        );
    }

    public static void drawMesh(
            Mesh mesh
    ) {
        drawMesh(
                mesh,
                (List<Texture>) null,
                (Matrix4f) null
        );
    }

    public static void drawMesh(
            Mesh mesh,
            Texture texture
    ) {
        if (texture != null) {
            drawMesh(
                    mesh,
                    Collections.singletonList(
                            texture
                    ),
                    (Matrix4f) null
            );
        } else {
            drawMesh(
                    mesh,
                    (List<Texture>) null,
                    (Vector3f) null
            );
        }
    }

    public static void drawMesh(
            Mesh mesh,
            List<Texture> textures
    ) {
        drawMesh(
                mesh,
                textures,
                (Vector3f) null
        );
    }

    public static void drawMesh(
            Mesh mesh,
            Texture... textures
    ) {
        drawMesh(
                mesh,
                textures != null
                        ? Arrays.asList(textures)
                        : null,
                (Vector3f)null
        );
    }

    public static void drawMesh(
            Mesh mesh,
            Matrix4f transform
    ) {
        drawMesh(
                mesh,
                (List<Texture>) null,
                transform
        );
    }

    public static void drawMesh(
            Mesh mesh,
            Texture texture,
            Matrix4f transform
    ) {
        if (texture != null) {
            drawMesh(
                    mesh,
                    Collections.singletonList(
                            texture
                    ),
                    transform
            );
        } else {
            drawMesh(
                    mesh,
                    (List<Texture>) null,
                    transform
            );
        }
    }

    public static void drawMesh(
            Mesh mesh,
            Matrix4f transform,
            Texture... textures
    ) {
        drawMesh(
                mesh,
                textures != null
                        ? Arrays.asList(textures)
                        : null,
                transform
        );
    }

    public static void drawMesh(
            Mesh mesh,
            List<Texture> textures,
            Matrix4f transform
    ) {
        if (mesh == null) {
            return;
        }

        List<Vertex> vertices =
                mesh.getVertices();

        if (vertices == null
                || vertices.isEmpty()) {
            return;
        }

        List<Integer> indices =
                mesh.getIndices();

        if (indices == null) {
            indices =
                    Collections.emptyList();
        }

        List<Texture> effectiveTextures =
                resolveTextures(
                        mesh,
                        textures
                );

        if (cullingEnabled
                && camera != null
                && isMeshCulled(
                        mesh,
                        transform
                )) {

            // It can be outside the camera frustum and still cast onto a
            // visible receiver. Preserve it for the shadow pass only.
            if (shadowsEnabled
                    && shadowResourcesReady
                    && shadowShader != null) {
                queueDirectDraw(
                        mesh,
                        effectiveTextures,
                        transform,
                        true
                );
            }

            return;
        }

        int vertexCount =
                vertices.size();

        int indexCount =
                indices.size();

        /*
         * The dense path never goes through RenderBatch. This is the crucial
         * difference from the old implementation: vertex transformation and
         * GPU buffer upload happen once, not once per frame.
         */
        if (vertexCount >= DIRECT_RENDER_VERTEX_THRESHOLD
                || indexCount >= DIRECT_RENDER_INDEX_THRESHOLD
                || vertexCount > RenderBatch.MAX_BATCH_SIZE
                || indexCount > RenderBatch.MAX_INDICES) {

            queueDirectDraw(
                    mesh,
                    effectiveTextures,
                    transform,
                    false
            );

            return;
        }

        if (lastBatch != null
                && lastBatch.hasRoom(
                        vertexCount,
                        indexCount
                )
                && lastBatch.hasTextures(
                        effectiveTextures
                )) {

            lastBatch.addMesh(
                    mesh,
                    effectiveTextures,
                    transform
            );

            return;
        }

        for (
                int i = batches.size() - 1;
                i >= 0;
                i--
        ) {
            RenderBatch batch =
                    batches.get(i);

            if (batch.hasRoom(
                    vertexCount,
                    indexCount
            ) && batch.hasTextures(
                    effectiveTextures
            )) {

                batch.addMesh(
                        mesh,
                        effectiveTextures,
                        transform
                );

                lastBatch = batch;
                return;
            }
        }

        RenderBatch batch =
                new RenderBatch(whiteTexture);

        batch.init();

        batch.addMesh(
                mesh,
                effectiveTextures,
                transform
        );

        batches.add(batch);
        lastBatch = batch;
    }

    private static void queueDirectDraw(
            Mesh mesh,
            List<Texture> textures,
            Matrix4f transform,
            boolean shadowOnly
    ) {
        Matrix4f storedTransform =
                transform != null
                        ? new Matrix4f(transform)
                        : identityMatrix;

        directDraws.add(
                new DirectDraw(
                        mesh,
                        textures,
                        storedTransform,
                        shadowOnly
                )
        );
    }

    private static List<Texture> resolveTextures(
            Mesh mesh,
            List<Texture> textures
    ) {
        if (textures != null
                && !textures.isEmpty()) {
            return textures;
        }

        List<Texture> meshTextures =
                mesh.getTextures();

        if (meshTextures != null
                && !meshTextures.isEmpty()) {
            return meshTextures;
        }

        Texture texture =
                mesh.getTexture();

        if (texture != null) {
            return Collections.singletonList(
                    texture
            );
        }

        return Collections.emptyList();
    }

    public static void drawMesh(
            Mesh mesh,
            Vector3f position
    ) {
        drawMesh(
                mesh,
                (List<Texture>) null,
                position,
                null,
                null
        );
    }

    public static void drawMesh(
            Mesh mesh,
            Texture texture,
            Vector3f position
    ) {
        drawMesh(
                mesh,
                texture,
                position,
                null,
                null
        );
    }

    public static void drawMesh(
            Mesh mesh,
            List<Texture> textures,
            Vector3f position
    ) {
        drawMesh(
                mesh,
                textures,
                position,
                null,
                null
        );
    }

    public static void drawMesh(
            Mesh mesh,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale
    ) {
        drawMesh(
                mesh,
                (List<Texture>) null,
                position,
                rotation,
                scale
        );
    }

    public static void drawMesh(
            Mesh mesh,
            Texture texture,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale
    ) {
        if (texture != null) {
            drawMesh(
                    mesh,
                    Collections.singletonList(
                            texture
                    ),
                    position,
                    rotation,
                    scale
            );
        } else {
            drawMesh(
                    mesh,
                    (List<Texture>) null,
                    position,
                    rotation,
                    scale
            );
        }
    }

    public static void drawMesh(
            Mesh mesh,
            List<Texture> textures,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale
    ) {
        tempTransform.identity();

        if (position != null) {
            tempTransform.translate(position);
        }

        if (rotation != null) {
            tempTransform.rotate(
                    (float) Math.toRadians(
                            rotation.x
                    ),
                    1,
                    0,
                    0
            );

            tempTransform.rotate(
                    (float) Math.toRadians(
                            rotation.y
                    ),
                    0,
                    1,
                    0
            );

            tempTransform.rotate(
                    (float) Math.toRadians(
                            rotation.z
                    ),
                    0,
                    0,
                    1
            );
        }

        if (scale != null) {
            tempTransform.scale(scale);
        }

        drawMesh(
                mesh,
                textures,
                tempTransform
        );
    }

    public static void drawMesh(
            Mesh mesh,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale,
            Texture... textures
    ) {
        drawMesh(
                mesh,
                textures != null
                        ? Arrays.asList(textures)
                        : null,
                position,
                rotation,
                scale
        );
    }

    public static void drawModel(
            Model model
    ) {
        drawModel(
                model,
                (List<Texture>) null,
                null
        );
    }

    public static void drawModel(
            Model model,
            Matrix4f transform
    ) {
        drawModel(
                model,
                (List<Texture>) null,
                transform
        );
    }

    public static void drawModel(
            Model model,
            Texture texture
    ) {
        if (texture != null) {
            drawModel(
                    model,
                    Collections.singletonList(
                            texture
                    ),
                    null
            );
        } else {
            drawModel(
                    model,
                    (List<Texture>) null,
                    null
            );
        }
    }

    public static void drawModel(
            Model model,
            Texture texture,
            Matrix4f transform
    ) {
        if (texture != null) {
            drawModel(
                    model,
                    Collections.singletonList(
                            texture
                    ),
                    transform
            );
        } else {
            drawModel(
                    model,
                    (List<Texture>) null,
                    transform
            );
        }
    }

    public static void drawModel(
            Model model,
            List<Texture> textures
    ) {
        drawModel(
                model,
                textures,
                null
        );
    }

    public static void drawModel(
            Model model,
            Texture... textures
    ) {
        drawModel(
                model,
                textures != null
                        ? Arrays.asList(textures)
                        : null,
                null
        );
    }

    public static void drawModel(
            Model model,
            Matrix4f transform,
            Texture... textures
    ) {
        drawModel(
                model,
                textures != null
                        ? Arrays.asList(textures)
                        : null,
                transform
        );
    }

    public static void drawModel(
            Model model,
            List<Texture> textures,
            Matrix4f transform
    ) {
        if (model == null) {
            return;
        }

        Matrix4f modelMatrix =
                model.getModelMatrix();

        Matrix4f combined =
                transform != null
                        ? tempCombined
                                .set(transform)
                                .mul(modelMatrix)
                        : modelMatrix;

        List<Mesh> meshes =
                model.getMeshes();

        for (
                int i = 0;
                i < meshes.size();
                i++
        ) {
            Mesh mesh =
                    meshes.get(i);

            Texture texture = null;

            if (textures != null
                    && !textures.isEmpty()) {

                if (i < textures.size()
                        && textures.get(i) != null) {

                    texture =
                            textures.get(i);

                } else if (textures.size() == 1) {

                    texture =
                            textures.get(0);
                }
            }

            if (texture == null) {
                Texture meshTexture =
                        mesh.getTexture();

                texture =
                        meshTexture != null
                                ? meshTexture
                                : model.getTexture(i);
            }

            drawMesh(
                    mesh,
                    texture,
                    combined
            );
        }
    }

    public static void drawModel(
            Model model,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale
    ) {
        drawModel(
                model,
                (List<Texture>) null,
                position,
                rotation,
                scale
        );
    }

    public static void drawModel(
            Model model,
            Texture texture,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale
    ) {
        if (texture != null) {
            drawModel(
                    model,
                    Collections.singletonList(
                            texture
                    ),
                    position,
                    rotation,
                    scale
            );
        } else {
            drawModel(
                    model,
                    (List<Texture>) null,
                    position,
                    rotation,
                    scale
            );
        }
    }

    public static void drawModel(
            Model model,
            List<Texture> textures,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale
    ) {
        tempTransform.identity();

        if (position != null) {
            tempTransform.translate(position);
        }

        if (rotation != null) {
            tempTransform.rotate(
                    (float) Math.toRadians(
                            rotation.x
                    ),
                    1,
                    0,
                    0
            );

            tempTransform.rotate(
                    (float) Math.toRadians(
                            rotation.y
                    ),
                    0,
                    1,
                    0
            );

            tempTransform.rotate(
                    (float) Math.toRadians(
                            rotation.z
                    ),
                    0,
                    0,
                    1
            );
        }

        if (scale != null) {
            tempTransform.scale(scale);
        }

        drawModel(
                model,
                textures,
                tempTransform
        );
    }

    public static void drawModel(
            Model model,
            Vector3f position,
            Vector3f rotation,
            Vector3f scale,
            Texture... textures
    ) {
        drawModel(
                model,
                textures != null
                        ? Arrays.asList(textures)
                        : null,
                position,
                rotation,
                scale
        );
    }

    public static void render() {
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        applyCullingState();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glClearColor(
                bgColor.r,
                bgColor.g,
                bgColor.b,
                bgColor.a
        );

        glClear(
                GL_COLOR_BUFFER_BIT
                        | GL_DEPTH_BUFFER_BIT
        );

        if (shader == null) {
            renderCallsAmount = 0;
            directDraws.clear();
            return;
        }

        updateProjection();
        buildFrustum();

        if (shadowsEnabled && shadowResourcesReady && shadowShader != null) {
            renderShadows();
            restoreMainViewport();
            glDepthFunc(GL_LEQUAL);
            applyCullingState();
            glColorMask(true, true, true, true);
            glDrawBuffer(GL_BACK);
            glDepthMask(true);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        shader.use();

        shader.uploadMatrix4f(
                "uProjection",
                projectionMatrix
        );

        shader.uploadMatrix4f(
                "uView",
                viewMatrix
        );

        shader.uploadIntArray(
                "uTextures",
                texSlots
        );

        /*
         * Hardware back-face culling makes the old per-fragment normal discard
         * redundant whenever culling is enabled. Avoid paying for both.
         */
        shader.uploadInt(
                "uNormalCulling",
                normalCullingEnabled && !cullingEnabled
                        ? 1
                        : 0
        );

        if (camera != null) {
            shader.uploadVec3f(
                    "uCameraPos",
                    camera.getPosition()
            );
        } else {
            shader.uploadVec3f(
                    "uCameraPos",
                    0.0f,
                    0.0f,
                    1000.0f
            );
        }

        uploadShadowUniforms();
        uploadLighting();

        int drawCalls = 0;

        /*
         * Dense meshes are rendered from persistent GPU buffers.
         * This eliminates RenderBatch's per-frame CPU vertex transforms and
         * glBufferSubData calls for the expensive models.
         */
        for (
                int i = 0;
                i < directDraws.size();
                i++
        ) {
            DirectDraw draw =
                    directDraws.get(i);

            if (draw.shadowOnly) {
                continue;
            }

            GpuMesh gpuMesh =
                    gpuMeshes.get(draw.mesh);

            if (gpuMesh == null) {
                gpuMesh = new GpuMesh();
                gpuMeshes.put(
                        draw.mesh,
                        gpuMesh
                );
            }

            gpuMesh.ensureUploaded(
                    draw.mesh,
                    draw.textures,
                    draw.transform,
                    whiteTexture
            );

            bindDirectTextures(
                    gpuMesh
            );

            glBindVertexArray(
                    gpuMesh.vao
            );

            glDrawElements(
                    GL_TRIANGLES,
                    gpuMesh.indexCount,
                    GL_UNSIGNED_INT,
                    0
            );

            glBindVertexArray(0);

            drawCalls++;
        }

        unbindDirectTextures();

        /*
         * Batch path remains for smaller geometry because it reduces draw calls
         * and preserves the existing material batching behavior.
         */
        for (
                int i = 0;
                i < batches.size();
                i++
        ) {
            RenderBatch batch =
                    batches.get(i);

            if (batch.flush()) {
                drawCalls++;
            }

            batch.reset();
        }

        renderCallsAmount =
                drawCalls;

        lastBatch = null;
        directDraws.clear();

        shader.detach();
    }

    private static void restoreMainViewport() {
        if (windowHandle == 0) return;

        windowWidthBuffer.clear();
        windowHeightBuffer.clear();

        glfwGetFramebufferSize(
                windowHandle,
                windowWidthBuffer,
                windowHeightBuffer
        );

        int width = Math.max(1, windowWidthBuffer.get(0));
        int height = Math.max(1, windowHeightBuffer.get(0));

        glViewport(0, 0, width, height);
    }

    private static void createShadowResources() {
        if (!glLoaded()) return;

        destroyShadowResources();

        try {
            int maxTextureUnits =
                    glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS);

            if (maxTextureUnits < SHADOW_TEXTURE_UNIT + 1) {
                shadowResourcesReady = false;
                return;
            }

            shadowFbo = glGenFramebuffers();
            shadowMapTexture = glGenTextures();
            shadowDepthBuffer = glGenRenderbuffers();

            glBindTexture(
                    GL_TEXTURE_2D_ARRAY,
                    shadowMapTexture
            );

            glTexImage3D(
                    GL_TEXTURE_2D_ARRAY,
                    0,
                    GL_R32F,
                    shadowMapResolution,
                    shadowMapResolution,
                    6,
                    0,
                    GL_RED,
                    GL_FLOAT,
                    (FloatBuffer) null
            );

            glTexParameteri(
                    GL_TEXTURE_2D_ARRAY,
                    GL_TEXTURE_MIN_FILTER,
                    GL_LINEAR
            );
            glTexParameteri(
                    GL_TEXTURE_2D_ARRAY,
                    GL_TEXTURE_MAG_FILTER,
                    GL_LINEAR
            );
            glTexParameteri(
                    GL_TEXTURE_2D_ARRAY,
                    GL_TEXTURE_WRAP_S,
                    GL_CLAMP_TO_BORDER
            );
            glTexParameteri(
                    GL_TEXTURE_2D_ARRAY,
                    GL_TEXTURE_WRAP_T,
                    GL_CLAMP_TO_BORDER
            );
            glTexParameteri(
                    GL_TEXTURE_2D_ARRAY,
                    GL_TEXTURE_WRAP_R,
                    GL_CLAMP_TO_EDGE
            );
            glTexParameterfv(
                    GL_TEXTURE_2D_ARRAY,
                    GL_TEXTURE_BORDER_COLOR,
                    new float[]{1f, 1f, 1f, 1f}
            );

            glBindTexture(
                    GL_TEXTURE_2D_ARRAY,
                    0
            );

            glBindRenderbuffer(
                    GL_RENDERBUFFER,
                    shadowDepthBuffer
            );

            glRenderbufferStorage(
                    GL_RENDERBUFFER,
                    GL_DEPTH_COMPONENT24,
                    shadowMapResolution,
                    shadowMapResolution
            );

            glBindRenderbuffer(
                    GL_RENDERBUFFER,
                    0
            );

            glBindFramebuffer(
                    GL_FRAMEBUFFER,
                    shadowFbo
            );

            glFramebufferTextureLayer(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    shadowMapTexture,
                    0,
                    0
            );

            glFramebufferRenderbuffer(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_ATTACHMENT,
                    GL_RENDERBUFFER,
                    shadowDepthBuffer
            );

            glDrawBuffer(GL_COLOR_ATTACHMENT0);
            glReadBuffer(GL_NONE);

            shadowResourcesReady =
                    glCheckFramebufferStatus(
                            GL_FRAMEBUFFER
                    ) == GL_FRAMEBUFFER_COMPLETE;

            glBindFramebuffer(
                    GL_FRAMEBUFFER,
                    0
            );

        } catch (RuntimeException e) {
            shadowResourcesReady = false;
            destroyShadowResources();
        }
    }

    private static boolean glLoaded() {
        try {
            glGetString(GL_VERSION);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void destroyShadowResources() {
        if (shadowMapTexture != 0) {
            glDeleteTextures(shadowMapTexture);
            shadowMapTexture = 0;
        }

        if (shadowDepthBuffer != 0) {
            glDeleteRenderbuffers(shadowDepthBuffer);
            shadowDepthBuffer = 0;
        }

        if (shadowFbo != 0) {
            glDeleteFramebuffers(shadowFbo);
            shadowFbo = 0;
        }

        shadowResourcesReady = false;
    }

    private static void uploadShadowUniforms() {
        Light light = firstShadowLight();
        boolean enabled = shadowsEnabled && light != null && shadowResourcesReady;

        shader.uploadInt("uShadowsEnabled", enabled ? 1 : 0);

        if (!enabled) {
            shader.uploadInt("uShadowMode", 0);
            return;
        }

        int mode = light.isDirectional() ? 1 : (light.isSpot() ? 2 : 3);

        shader.uploadInt("uShadowMode", mode);
        shader.uploadFloat("uShadowBias", shadowBias);
        shader.uploadFloat("uShadowNormalBias", shadowNormalBias);
        shader.uploadFloat("uShadowFarPlane", shadowFarPlane);
        shader.uploadFloat("uShadowNearPlane", shadowNearPlane);
        shader.uploadFloat("uShadowTexelSize", 1.0f / shadowMapResolution);
        shader.uploadVec3f("uShadowLightPos", light.getPosition());

        glActiveTexture(GL_TEXTURE0 + SHADOW_TEXTURE_UNIT);
        glBindTexture(GL_TEXTURE_2D_ARRAY, shadowMapTexture);
        shader.uploadInt("uShadowMap", SHADOW_TEXTURE_UNIT);

        if (mode == 3) {
            for (int i = 0; i < 6; i++) {
                shader.uploadMatrix4f(
                        "uPointShadowMatrices[" + i + "]",
                        pointShadowMatrices[i]
                );
            }
        } else {
            shader.uploadMatrix4f(
                    "uShadowViewProjection",
                    shadowViewProjection
            );
        }
    }

    private static Light firstShadowLight() {
        for (int i = 0; i < lights.size() && i < MAX_LIGHTS; i++) {
            Light light = lights.get(i);
            if (light != null && light.getIntensity() > 0.0f) {
                return light;
            }
        }
        return null;
    }

    private static void renderShadows() {
        Light light = firstShadowLight();
        if (light == null || !shadowResourcesReady) {
            return;
        }

        if (light.isPoint()) {
            renderPointShadow(light);
        } else {
            render2DShadow(light);
        }
    }

    private static void render2DShadow(Light light) {
        Vector3f direction = light.getDirection();
        if (direction == null || direction.lengthSquared() < 0.000001f) {
            direction = new Vector3f(0.0f, -1.0f, 0.0f);
        } else {
            shadowDirection.set(direction).normalize();
            direction = shadowDirection;
        }

        if (camera != null) {
            shadowTarget.set(camera.getPosition());
        } else {
            shadowTarget.set(0.0f, 0.0f, 0.0f);
        }

        float half = shadowDistance * 0.5f;
        float eyeDistance = Math.max(shadowDistance, 10.0f);

        shadowEye.set(
                shadowTarget.x - direction.x * eyeDistance,
                shadowTarget.y - direction.y * eyeDistance,
                shadowTarget.z - direction.z * eyeDistance
        );

        chooseUpVector(direction, shadowUp);

        shadowView.identity().lookAt(
                shadowEye,
                shadowTarget,
                shadowUp
        );

        if (light.isDirectional()) {
            shadowProjection
                    .identity()
                    .ortho(
                            -half,
                            half,
                            -half,
                            half,
                            0.1f,
                            shadowDistance * 2.0f
                    );
        } else {
            float outer = clamp(
                    light.getOuterCutOff(),
                    0.05f,
                    0.9999f
            );
            float angle =
                    (float) Math.acos(outer) * 2.0f;

            float fov = clamp(
                    (float) Math.toDegrees(angle),
                    1.0f,
                    170.0f
            );

            shadowEye.set(light.getPosition());
            shadowTarget.set(
                    shadowEye.x - direction.x,
                    shadowEye.y - direction.y,
                    shadowEye.z - direction.z
            );

            shadowView.identity().lookAt(
                    shadowEye,
                    shadowTarget,
                    shadowUp
            );

            shadowProjection
                    .identity()
                    .perspective(
                            (float) Math.toRadians(fov),
                            1.0f,
                            shadowNearPlane,
                            shadowFarPlane
                    );
        }

        shadowViewProjection
                .set(shadowProjection)
                .mul(shadowView);

        renderDepthTarget(
                GL_TEXTURE_2D,
                0,
                shadowViewProjection
        );
    }

    private static void renderPointShadow(Light light) {
        Vector3f pos = light.getPosition();
        if (pos == null) return;

        shadowProjection
                .identity()
                .perspective(
                        (float) Math.toRadians(90.0f),
                        1.0f,
                        shadowNearPlane,
                        shadowFarPlane
                );

        final float[][] directions = {
                { 1, 0, 0 },
                {-1, 0, 0 },
                { 0, 1, 0 },
                { 0,-1, 0 },
                { 0, 0, 1 },
                { 0, 0,-1 }
        };

        final float[][] ups = {
                {0,-1, 0},
                {0,-1, 0},
                {0, 0, 1},
                {0, 0,-1},
                {0,-1, 0},
                {0,-1, 0}
        };

        glBindFramebuffer(
                GL_FRAMEBUFFER,
                shadowFbo
        );

        glViewport(
                0,
                0,
                shadowMapResolution,
                shadowMapResolution
        );

        glDrawBuffer(GL_COLOR_ATTACHMENT0);
        glReadBuffer(GL_NONE);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDepthMask(true);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);

        glDisable(GL_BLEND);
        glColorMask(true, false, false, false);

        glClearColor(1f, 1f, 1f, 1f);

        shadowShader.use();
        shadowShader.uploadInt(
                "uShadowMode",
                3
        );
        shadowShader.uploadVec3f(
                "uShadowLightPos",
                pos
        );
        shadowShader.uploadFloat(
                "uShadowFarPlane",
                shadowFarPlane
        );

        bindShadowMaterialTextures();

        for (int face = 0; face < 6; face++) {
            glFramebufferTextureLayer(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    shadowMapTexture,
                    0,
                    face
            );

            glClear(
                    GL_COLOR_BUFFER_BIT
                            | GL_DEPTH_BUFFER_BIT
            );

            shadowEye.set(pos);
            shadowTarget.set(
                    pos.x + directions[face][0],
                    pos.y + directions[face][1],
                    pos.z + directions[face][2]
            );

            shadowUp.set(
                    ups[face][0],
                    ups[face][1],
                    ups[face][2]
            );

            shadowView
                    .identity()
                    .lookAt(
                            shadowEye,
                            shadowTarget,
                            shadowUp
                    );

            shadowViewProjection
                    .set(shadowProjection)
                    .mul(shadowView);

            pointShadowMatrices[face]
                    .set(shadowViewProjection);

            shadowShader.uploadMatrix4f(
                    "uLightViewProjection",
                    shadowViewProjection
            );

            renderShadowGeometry();
        }

        glBindVertexArray(0);
        glColorMask(true, true, true, true);
        glCullFace(cullFace);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private static void renderDepthTarget(
            int textureTarget,
            int face,
            Matrix4f matrix
    ) {
        glBindFramebuffer(
                GL_FRAMEBUFFER,
                shadowFbo
        );

        glFramebufferTextureLayer(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                shadowMapTexture,
                0,
                0
        );

        glDrawBuffer(GL_COLOR_ATTACHMENT0);
        glReadBuffer(GL_NONE);

        glViewport(
                0,
                0,
                shadowMapResolution,
                shadowMapResolution
        );

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDepthMask(true);

        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);

        glDisable(GL_BLEND);
        glColorMask(true, false, false, false);

        glClearColor(1f, 1f, 1f, 1f);
        glClear(
                GL_COLOR_BUFFER_BIT
                        | GL_DEPTH_BUFFER_BIT
        );

        shadowShader.use();
        Light light = firstShadowLight();

        shadowShader.uploadInt(
                "uShadowMode",
                light != null && light.isSpot()
                        ? 2
                        : 1
        );

        if (light != null) {
            shadowShader.uploadVec3f(
                    "uShadowLightPos",
                    light.getPosition()
            );
        }

        shadowShader.uploadFloat(
                "uShadowFarPlane",
                shadowFarPlane
        );

        shadowShader.uploadMatrix4f(
                "uLightViewProjection",
                matrix
        );

        bindShadowMaterialTextures();
        renderShadowGeometry();

        glBindVertexArray(0);
        glColorMask(true, true, true, true);
        glCullFace(cullFace);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    private static void bindShadowMaterialTextures() {
        // Per-draw binding is used in renderShadowGeometry().
    }

    private static void renderShadowGeometry() {
        // Dense persistent meshes never touch the CPU geometry again.
        for (int i = 0; i < directDraws.size(); i++) {
            DirectDraw draw = directDraws.get(i);

            GpuMesh gpu = gpuMeshes.get(draw.mesh);
            if (gpu == null) {
                gpu = new GpuMesh();
                gpuMeshes.put(draw.mesh, gpu);
            }

            gpu.ensureUploaded(
                    draw.mesh,
                    draw.textures,
                    draw.transform,
                    whiteTexture
            );

            bindDirectTextures(gpu);

            glBindVertexArray(gpu.vao);
            glDrawElements(
                    GL_TRIANGLES,
                    gpu.indexCount,
                    GL_UNSIGNED_INT,
                    0
            );
        }

        glBindVertexArray(0);

        // Existing small-mesh batches are still supported without any new API.
        for (int i = 0; i < batches.size(); i++) {
            RenderBatch batch = batches.get(i);
            batch.flush();
        }
    }

    private static void chooseUpVector(
            Vector3f direction,
            Vector3f out
    ) {
        float ax = Math.abs(direction.x);
        float ay = Math.abs(direction.y);
        float az = Math.abs(direction.z);

        if (ax <= ay && ax <= az) {
            out.set(1, 0, 0);
        } else if (ay <= az) {
            out.set(0, 1, 0);
        } else {
            out.set(0, 0, 1);
        }
    }

    private static float clamp(
            float value,
            float min,
            float max
    ) {
        return Math.max(min, Math.min(max, value));
    }

    private static void uploadLighting() {
        boolean useLighting =
                lightingEnabled
                        && camera != null
                        && !lights.isEmpty();

        shader.uploadInt(
                "uUseLighting",
                useLighting ? 1 : 0
        );

        if (!useLighting) {
            shader.uploadInt(
                    "uNumLights",
                    0
            );
            return;
        }

        int numLights =
                Math.min(
                        lights.size(),
                        MAX_LIGHTS
                );

        shader.uploadInt(
                "uNumLights",
                numLights
        );

        Light firstLight =
                lights.get(0);

        Color aColor =
                firstLight != null
                        && firstLight.getAmbientColor() != null
                        ? firstLight.getAmbientColor()
                        : ambientColor;

        float aIntensity =
                firstLight != null
                        ? firstLight.getAmbientIntensity()
                        : ambientIntensity;

        shader.uploadVec3f(
                "uAmbientColor",
                aColor.r,
                aColor.g,
                aColor.b
        );

        shader.uploadFloat(
                "uAmbientIntensity",
                aIntensity
        );

        for (
                int i = 0;
                i < numLights;
                i++
        ) {
            Light light =
                    lights.get(i);

            if (light == null) {
                continue;
            }

            shader.uploadInt(
                    "uLightType[" + i + "]",
                    light.getType() != null
                            ? light.getType().getId()
                            : (light.isDirectional()
                                ? 1
                                : 0)
            );

            shader.uploadInt(
                    "uIsDirectional[" + i + "]",
                    light.isDirectional()
                            ? 1
                            : 0
            );

            shader.uploadVec3f(
                    "uLightPos[" + i + "]",
                    light.getPosition()
            );

            Vector3f direction =
                    light.getDirection();

            if (direction != null
                    && direction.lengthSquared()
                        > 0.000001f) {

                tempLightDir
                        .set(direction)
                        .normalize();

            } else {
                tempLightDir.set(
                        DEFAULT_LIGHT_DIR
                );
            }

            shader.uploadVec3f(
                    "uLightDir[" + i + "]",
                    tempLightDir
            );

            shader.uploadFloat(
                    "uLightIntensity[" + i + "]",
                    light.getIntensity()
            );

            Color lightColor =
                    light.getColor() != null
                            ? light.getColor()
                            : Color.White;

            shader.uploadVec4f(
                    "uLightColor[" + i + "]",
                    lightColor.r,
                    lightColor.g,
                    lightColor.b,
                    lightColor.a
            );

            shader.uploadFloat(
                    "uSpecularIntensity[" + i + "]",
                    light.getSpecularIntensity()
            );

            shader.uploadFloat(
                    "uShininess[" + i + "]",
                    light.getShininess()
            );

            shader.uploadFloat(
                    "uCutOff[" + i + "]",
                    light.getCutOff()
            );

            shader.uploadFloat(
                    "uOuterCutOff[" + i + "]",
                    light.getOuterCutOff()
            );
        }
    }

    private static void bindDirectTextures(
            GpuMesh mesh
    ) {
        for (
                int i = 0;
                i < mesh.textureCount;
                i++
        ) {
            Texture texture =
                    mesh.textures[i];

            int id =
                    texture != null
                            ? texture.getId()
                            : -1;

            if (!directTextureWasBound[i]
                    || currentlyBoundDirectTextures[i]
                        != id) {

                if (texture != null) {
                    texture.bind(i);
                } else if (whiteTexture != null) {
                    whiteTexture.bind(i);
                }

                currentlyBoundDirectTextures[i] =
                        id;

                directTextureWasBound[i] =
                        true;
            }
        }
    }

    private static void unbindDirectTextures() {
        /*
         * Do not issue 16 unbind calls here. RenderBatch.flush() establishes
         * its complete texture state before it draws, and skipping the driver
         * round-trip is measurably cheaper.
         */
        Arrays.fill(
                directTextureWasBound,
                false
        );
    }

    private static void setupVertexLayout() {
        glVertexAttribPointer(
                0,
                3,
                GL_FLOAT,
                false,
                Vertex.BYTES,
                0
        );

        glEnableVertexAttribArray(0);

        glVertexAttribPointer(
                1,
                3,
                GL_FLOAT,
                false,
                Vertex.BYTES,
                3L * Float.BYTES
        );

        glEnableVertexAttribArray(1);

        glVertexAttribPointer(
                2,
                4,
                GL_FLOAT,
                false,
                Vertex.BYTES,
                6L * Float.BYTES
        );

        glEnableVertexAttribArray(2);

        glVertexAttribPointer(
                3,
                2,
                GL_FLOAT,
                false,
                Vertex.BYTES,
                10L * Float.BYTES
        );

        glEnableVertexAttribArray(3);

        glVertexAttribPointer(
                4,
                1,
                GL_FLOAT,
                false,
                Vertex.BYTES,
                12L * Float.BYTES
        );

        glEnableVertexAttribArray(4);
    }

    private static void applyCullingState() {
        if (cullingEnabled) {
            glEnable(GL_CULL_FACE);
            glCullFace(cullFace);
            glFrontFace(frontFace);
        } else {
            glDisable(GL_CULL_FACE);
        }
    }

    private static void buildFrustum() {
        frustumMatrix
                .set(projectionMatrix)
                .mul(viewMatrix);

        float r0x = frustumMatrix.m00();
        float r0y = frustumMatrix.m10();
        float r0z = frustumMatrix.m20();
        float r0w = frustumMatrix.m30();

        float r1x = frustumMatrix.m01();
        float r1y = frustumMatrix.m11();
        float r1z = frustumMatrix.m21();
        float r1w = frustumMatrix.m31();

        float r2x = frustumMatrix.m02();
        float r2y = frustumMatrix.m12();
        float r2z = frustumMatrix.m22();
        float r2w = frustumMatrix.m32();

        float r3x = frustumMatrix.m03();
        float r3y = frustumMatrix.m13();
        float r3z = frustumMatrix.m23();
        float r3w = frustumMatrix.m33();

        setPlane(
                0,
                r3x + r0x,
                r3y + r0y,
                r3z + r0z,
                r3w + r0w
        );

        setPlane(
                1,
                r3x - r0x,
                r3y - r0y,
                r3z - r0z,
                r3w - r0w
        );

        setPlane(
                2,
                r3x + r1x,
                r3y + r1y,
                r3z + r1z,
                r3w + r1w
        );

        setPlane(
                3,
                r3x - r1x,
                r3y - r1y,
                r3z - r1z,
                r3w - r1w
        );

        setPlane(
                4,
                r3x + r2x,
                r3y + r2y,
                r3z + r2z,
                r3w + r2w
        );

        setPlane(
                5,
                r3x - r2x,
                r3y - r2y,
                r3z - r2z,
                r3w - r2w
        );
    }

    private static void setPlane(
            int plane,
            float a,
            float b,
            float c,
            float d
    ) {
        int o = plane * 4;

        frustumPlanes[o] = a;
        frustumPlanes[o + 1] = b;
        frustumPlanes[o + 2] = c;
        frustumPlanes[o + 3] = d;
    }

    private static boolean isMeshCulled(
            Mesh mesh,
            Matrix4f transform
    ) {
        Bounds bounds =
                getBounds(mesh);

        if (bounds == null) {
            return false;
        }

        Matrix4f model =
                transform != null
                        ? transform
                        : identityMatrix;

        float centerX =
                (bounds.minX + bounds.maxX)
                        * 0.5f;

        float centerY =
                (bounds.minY + bounds.maxY)
                        * 0.5f;

        float centerZ =
                (bounds.minZ + bounds.maxZ)
                        * 0.5f;

        float extentX =
                (bounds.maxX - bounds.minX)
                        * 0.5f;

        float extentY =
                (bounds.maxY - bounds.minY)
                        * 0.5f;

        float extentZ =
                (bounds.maxZ - bounds.minZ)
                        * 0.5f;

        /*
         * Transform the AABB's center.
         */
        float worldX =
                model.m00() * centerX
                + model.m10() * centerY
                + model.m20() * centerZ
                + model.m30();

        float worldY =
                model.m01() * centerX
                + model.m11() * centerY
                + model.m21() * centerZ
                + model.m31();

        float worldZ =
                model.m02() * centerX
                + model.m12() * centerY
                + model.m22() * centerZ
                + model.m32();

        /*
         * Use an oriented-box plane test rather than a loose bounding sphere.
         * This rejects substantially more off-screen geometry without touching
         * individual triangles.
         */
        for (int p = 0; p < 6; p++) {
            int o = p * 4;

            float a = frustumPlanes[o];
            float b = frustumPlanes[o + 1];
            float c = frustumPlanes[o + 2];
            float d = frustumPlanes[o + 3];

            float distance =
                    a * worldX
                    + b * worldY
                    + c * worldZ
                    + d;

            float radius =
                    extentX * Math.abs(
                            a * model.m00()
                            + b * model.m01()
                            + c * model.m02()
                    )
                    + extentY * Math.abs(
                            a * model.m10()
                            + b * model.m11()
                            + c * model.m12()
                    )
                    + extentZ * Math.abs(
                            a * model.m20()
                            + b * model.m21()
                            + c * model.m22()
                    );

            if (distance < -radius) {
                return true;
            }
        }

        return false;
    }

    private static Bounds getBounds(
            Mesh mesh
    ) {
        List<Vertex> vertices =
                mesh.getVertices();

        if (vertices == null
                || vertices.isEmpty()) {
            return null;
        }

        List<Integer> indices =
                mesh.getIndices();

        long fingerprint =
                calculateGeometryFingerprint(
                        vertices,
                        indices
                );

        Bounds cached =
                boundsCache.get(mesh);

        if (cached != null
                && cached.sourceVertices == vertices
                && cached.vertexCount == vertices.size()
                && cached.fingerprint == fingerprint) {

            return cached;
        }

        float minX =
                Float.POSITIVE_INFINITY;
        float minY =
                Float.POSITIVE_INFINITY;
        float minZ =
                Float.POSITIVE_INFINITY;

        float maxX =
                Float.NEGATIVE_INFINITY;
        float maxY =
                Float.NEGATIVE_INFINITY;
        float maxZ =
                Float.NEGATIVE_INFINITY;

        for (Vertex v : vertices) {
            minX = Math.min(minX, v.x);
            minY = Math.min(minY, v.y);
            minZ = Math.min(minZ, v.z);

            maxX = Math.max(maxX, v.x);
            maxY = Math.max(maxY, v.y);
            maxZ = Math.max(maxZ, v.z);
        }

        Bounds result =
                new Bounds(
                        vertices,
                        vertices.size(),
                        minX,
                        minY,
                        minZ,
                        maxX,
                        maxY,
                        maxZ,
                        fingerprint
                );

        boundsCache.put(
                mesh,
                result
        );

        return result;
    }

    private static long calculateGeometryFingerprint(
            List<Vertex> vertices,
            List<Integer> indices
    ) {
        long hash =
                0xcbf29ce484222325L;

        hash ^= vertices.size();
        hash *= 0x100000001b3L;

        if (!vertices.isEmpty()) {
            /*
             * Sampling keeps the check extremely cheap even for multi-million
             * vertex meshes while catching transforms/updates in practice.
             */
            final int samples =
                    Math.min(
                            32,
                            vertices.size()
                    );

            for (
                    int s = 0;
                    s < samples;
                    s++
            ) {
                int index =
                        samples == 1
                                ? 0
                                : (int) (
                                    (long) s
                                    * (vertices.size() - 1)
                                    / (samples - 1)
                                );

                Vertex v =
                        vertices.get(index);

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.x)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.y)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.z)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.nx)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.ny)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.nz)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.u)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.v)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.r)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.g)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.b)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.a)
                );

                hash = mix(
                        hash,
                        Float.floatToIntBits(v.texID)
                );
            }
        }

        if (indices != null
                && !indices.isEmpty()) {

            hash = mix(
                    hash,
                    indices.size()
            );

            int samples =
                    Math.min(
                            32,
                            indices.size()
                    );

            for (
                    int s = 0;
                    s < samples;
                    s++
            ) {
                int index =
                        samples == 1
                                ? 0
                                : (int) (
                                    (long) s
                                    * (indices.size() - 1)
                                    / (samples - 1)
                                );

                hash = mix(
                        hash,
                        indices.get(index)
                );
            }
        }

        return hash;
    }

    private static long mix(
            long hash,
            int value
    ) {
        hash ^= value;
        hash *= 0x100000001b3L;
        return hash;
    }

    /*
     * Allows applications that intentionally mutate Mesh geometry after
     * loading to invalidate the persistent GPU copy explicitly.
     */
    public static void destroy() {
        destroyGpuMeshes();
        destroyShadowResources();
        if (shadowShader != null) {
            shadowShader.detach();
        }
        if (shader != null) {
            shader.detach();
        }
        batches.clear();
        directDraws.clear();
        lastBatch = null;
    }

    public static void invalidateMesh(
            Mesh mesh
    ) {
        if (mesh == null) {
            return;
        }

        GpuMesh gpu =
                gpuMeshes.remove(mesh);

        if (gpu != null) {
            gpu.destroy();
        }

        boundsCache.remove(mesh);
    }

    public static void clearMeshCache() {
        destroyGpuMeshes();
    }

    private static void destroyGpuMeshes() {
        for (GpuMesh mesh : gpuMeshes.values()) {
            mesh.destroy();
        }

        gpuMeshes.clear();
        boundsCache.clear();
    }
}
