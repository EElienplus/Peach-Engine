package net.meowsers.Peach.Graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Vertex;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderBatch {
    public static final int MAX_BATCH_SIZE = 50000;
    public static final int MAX_INDICES = 250000;

    private final float[] vertexData = new float[MAX_BATCH_SIZE * Vertex.ELEMENT_SIZE];
    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(MAX_BATCH_SIZE * Vertex.ELEMENT_SIZE);
    private int vertexCount = 0;

    private final int[] indices = new int[MAX_INDICES];
    private final IntBuffer indexBuffer = BufferUtils.createIntBuffer(MAX_INDICES);
    private int indexCount = 0;

    private int vaoID, vboID, eboID;
    private final Texture[] textures;
    private int textureCount = 1;
    private boolean hasRoom = true;
    private final Texture whiteTexture;

    private final int[] slotMap = new int[15];
    private final Vector4f tempPos = new Vector4f();
    private final Vector3f tempNorm = new Vector3f();

    public RenderBatch(Texture whiteTexture) {
        this.textures = new Texture[15];
        this.whiteTexture = whiteTexture;
        this.textures[0] = whiteTexture;
        this.textureCount = 1;
    }

    public void init() {
        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) MAX_BATCH_SIZE * Vertex.BYTES, GL_DYNAMIC_DRAW);

        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (long) MAX_INDICES * Integer.BYTES, GL_DYNAMIC_DRAW);

        // Attributes: Position (3), Normal (3), Color (4), TexCoords (2), TexID (1)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, Vertex.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 3, GL_FLOAT, false, Vertex.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 4, GL_FLOAT, false, Vertex.BYTES, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, 2, GL_FLOAT, false, Vertex.BYTES, 10 * Float.BYTES);
        glEnableVertexAttribArray(3);

        glVertexAttribPointer(4, 1, GL_FLOAT, false, Vertex.BYTES, 12 * Float.BYTES);
        glEnableVertexAttribArray(4);

        glBindVertexArray(0);
    }

    public boolean hasRoom() {
        return hasRoom;
    }

    public boolean hasRoom(int numVerts, int numIndices) {
        return (vertexCount + numVerts <= MAX_BATCH_SIZE) && (indexCount + numIndices <= MAX_INDICES);
    }

    public boolean hasTextureRoom() {
        return textureCount < 15;
    }

    public boolean hasTextures(Collection<Texture> texs) {
        if (texs == null || texs.isEmpty()) return true;
        int needed = 0;
        for (Texture t : texs) {
            if (t != null && !hasTexture(t)) {
                needed++;
            }
        }
        return textureCount + needed <= 15;
    }

    public boolean hasTextures(Texture... texs) {
        if (texs == null || texs.length == 0) return true;
        return hasTextures(Arrays.asList(texs));
    }

    public boolean hasTexture(Texture tex) {
        if (tex == null || tex == whiteTexture || (whiteTexture != null && tex.getId() == whiteTexture.getId())) {
            return true;
        }
        for (int i = 1; i < textureCount; i++) {
            if (textures[i] == tex || (textures[i] != null && textures[i].getId() == tex.getId())) {
                return true;
            }
        }
        return false;
    }

    public float addTexture(Texture tex) {
        if (tex == null || tex == whiteTexture || (whiteTexture != null && tex.getId() == whiteTexture.getId())) {
            return 0.0f;
        }
        for (int i = 1; i < textureCount; i++) {
            if (textures[i] == tex || (textures[i] != null && textures[i].getId() == tex.getId())) {
                return (float) i;
            }
        }
        if (textureCount < 15) {
            textures[textureCount] = tex;
            float slot = (float) textureCount;
            textureCount++;
            return slot;
        }
        return 0.0f;
    }

    public void reset() {
        vertexCount = 0;
        indexCount = 0;
        for (int i = 1; i < textureCount; i++) {
            textures[i] = null;
        }
        textures[0] = whiteTexture;
        textureCount = 1;
        hasRoom = true;
    }

    public void addVertices(List<Vertex> verts, List<Texture> texturesList) {
        if (verts == null || verts.isEmpty()) return;

        boolean hasSlotMap = false;
        if (texturesList != null && !texturesList.isEmpty()) {
            hasSlotMap = true;
            for (int i = 0; i < texturesList.size() && i < 15; i++) {
                slotMap[i] = (int) addTexture(texturesList.get(i));
            }
        }

        int baseOffset = vertexCount;
        for (int vi = 0; vi < verts.size(); vi++) {
            if (vertexCount >= MAX_BATCH_SIZE) break;
            Vertex v = verts.get(vi);

            float tid = 0.0f;
            if (hasSlotMap) {
                int rawTid = (int) v.texID;
                if (rawTid >= 0 && rawTid < texturesList.size()) {
                    tid = (float) slotMap[rawTid];
                } else {
                    tid = (float) slotMap[0];
                }
            }

            int offset = vertexCount * Vertex.ELEMENT_SIZE;
            vertexData[offset + 0] = v.x;
            vertexData[offset + 1] = v.y;
            vertexData[offset + 2] = v.z;
            vertexData[offset + 3] = v.nx;
            vertexData[offset + 4] = v.ny;
            vertexData[offset + 5] = v.nz;
            vertexData[offset + 6] = v.r;
            vertexData[offset + 7] = v.g;
            vertexData[offset + 8] = v.b;
            vertexData[offset + 9] = v.a;
            vertexData[offset + 10] = v.u;
            vertexData[offset + 11] = v.v;
            vertexData[offset + 12] = tid;
            vertexCount++;
        }

        int count = verts.size();
        if (count % 4 == 0) {
            for (int i = 0; i < count; i += 4) {
                if (indexCount + 6 > MAX_INDICES) break;
                indices[indexCount++] = baseOffset + i + 0;
                indices[indexCount++] = baseOffset + i + 1;
                indices[indexCount++] = baseOffset + i + 2;
                indices[indexCount++] = baseOffset + i + 2;
                indices[indexCount++] = baseOffset + i + 3;
                indices[indexCount++] = baseOffset + i + 0;
            }
        } else {
            for (int i = 0; i < count; i++) {
                if (indexCount >= MAX_INDICES) break;
                indices[indexCount++] = baseOffset + i;
            }
        }
        if (vertexCount >= MAX_BATCH_SIZE - 4 || indexCount >= MAX_INDICES - 6) {
            hasRoom = false;
        }
    }

    public void addVertices(List<Vertex> verts, Texture texture) {
        if (texture != null) {
            addVertices(verts, Collections.singletonList(texture));
        } else {
            addVertices(verts, (List<Texture>) null);
        }
    }

    public void addMesh(Mesh mesh, List<Texture> texturesOverride, Matrix4f transform) {
        if (mesh == null) return;
        List<Vertex> meshVerts = mesh.getVertices();
        if (meshVerts.isEmpty()) return;

        List<Texture> activeTextures = texturesOverride != null && !texturesOverride.isEmpty()
                ? texturesOverride
                : mesh.getTextures();

        boolean hasSlotMap = false;
        float fallbackSlot = 0.0f;

        if (activeTextures != null && !activeTextures.isEmpty()) {
            hasSlotMap = true;
            for (int i = 0; i < activeTextures.size() && i < 15; i++) {
                slotMap[i] = (int) addTexture(activeTextures.get(i));
            }
            fallbackSlot = (float) slotMap[0];
        } else if (mesh.getTexture() != null) {
            fallbackSlot = addTexture(mesh.getTexture());
        }

        Color meshColor = mesh.getColor();
        boolean hasMeshColor = meshColor != null;

        int baseOffset = vertexCount;
        for (int vi = 0; vi < meshVerts.size(); vi++) {
            if (vertexCount >= MAX_BATCH_SIZE) break;
            Vertex v = meshVerts.get(vi);

            float vx = v.x;
            float vy = v.y;
            float vz = v.z;
            float vnx = v.nx;
            float vny = v.ny;
            float vnz = v.nz;

            if (transform != null) {
                tempPos.set(vx, vy, vz, 1.0f);
                transform.transform(tempPos);
                vx = tempPos.x;
                vy = tempPos.y;
                vz = tempPos.z;

                tempNorm.set(vnx, vny, vnz);
                transform.transformDirection(tempNorm).normalize();
                vnx = tempNorm.x;
                vny = tempNorm.y;
                vnz = tempNorm.z;
            }

            float vr = v.r;
            float vg = v.g;
            float vb = v.b;
            float va = v.a;

            if (hasMeshColor && (vr == 1.0f && vg == 1.0f && vb == 1.0f && va == 1.0f)) {
                vr = meshColor.r;
                vg = meshColor.g;
                vb = meshColor.b;
                va = meshColor.a;
            }

            float tid = fallbackSlot;
            if (hasSlotMap) {
                int rawTid = (int) v.texID;
                if (rawTid >= 0 && rawTid < activeTextures.size() && rawTid < 15) {
                    tid = (float) slotMap[rawTid];
                }
            }

            int offset = vertexCount * Vertex.ELEMENT_SIZE;
            vertexData[offset + 0] = vx;
            vertexData[offset + 1] = vy;
            vertexData[offset + 2] = vz;
            vertexData[offset + 3] = vnx;
            vertexData[offset + 4] = vny;
            vertexData[offset + 5] = vnz;
            vertexData[offset + 6] = vr;
            vertexData[offset + 7] = vg;
            vertexData[offset + 8] = vb;
            vertexData[offset + 9] = va;
            vertexData[offset + 10] = v.u;
            vertexData[offset + 11] = v.v;
            vertexData[offset + 12] = tid;
            vertexCount++;
        }

        List<Integer> meshIndices = mesh.getIndices();
        if (!meshIndices.isEmpty()) {
            for (int i = 0; i < meshIndices.size(); i++) {
                if (indexCount >= MAX_INDICES) break;
                indices[indexCount++] = baseOffset + meshIndices.get(i);
            }
        } else {
            int count = meshVerts.size();
            if (count % 4 == 0) {
                for (int i = 0; i < count; i += 4) {
                    if (indexCount + 6 > MAX_INDICES) break;
                    indices[indexCount++] = baseOffset + i + 0;
                    indices[indexCount++] = baseOffset + i + 1;
                    indices[indexCount++] = baseOffset + i + 2;
                    indices[indexCount++] = baseOffset + i + 2;
                    indices[indexCount++] = baseOffset + i + 3;
                    indices[indexCount++] = baseOffset + i + 0;
                }
            } else {
                for (int i = 0; i < count; i++) {
                    if (indexCount >= MAX_INDICES) break;
                    indices[indexCount++] = baseOffset + i;
                }
            }
        }

        if (vertexCount >= MAX_BATCH_SIZE - 4 || indexCount >= MAX_INDICES - 6) {
            hasRoom = false;
        }
    }

    public void addMesh(Mesh mesh, Texture textureOverride, Matrix4f transform) {
        if (textureOverride != null) {
            addMesh(mesh, Collections.singletonList(textureOverride), transform);
        } else {
            addMesh(mesh, (List<Texture>) null, transform);
        }
    }

    public boolean flush() {
        if (vertexCount == 0 || indexCount == 0) return false;

        glBindVertexArray(vaoID);

        vertexBuffer.clear();
        vertexBuffer.put(vertexData, 0, vertexCount * Vertex.ELEMENT_SIZE);
        vertexBuffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        indexBuffer.clear();
        indexBuffer.put(indices, 0, indexCount);
        indexBuffer.flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indexBuffer);

        for (int i = 0; i < 15; i++) {
            if (textures[i] != null) {
                textures[i].bind(i);
            } else if (whiteTexture != null) {
                whiteTexture.bind(i);
            }
        }

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);
        glEnableVertexAttribArray(3);
        glEnableVertexAttribArray(4);

        if (Renderer.isCullingEnabled()) {
            glEnable(GL_CULL_FACE);
            glCullFace(Renderer.getCullFace());
            glFrontFace(Renderer.getFrontFace());
        } else {
            glDisable(GL_CULL_FACE);
        }

        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glDisableVertexAttribArray(4);
        glBindVertexArray(0);

        for (int i = 0; i < 15; i++) {
            if (textures[i] != null) {
                textures[i].unbind();
            } else if (whiteTexture != null) {
                whiteTexture.unbind();
            }
        }
        return true;
    }
}
