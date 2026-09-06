package net.meowsers.Peach.Graphics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.meowsers.Peach.Structures.Vertex;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class RenderBatch {
    public static final int MAX_BATCH_SIZE = 50000;
    public static final int MAX_INDICES = 250000;
    private final Vertex[] vertices;
    private int vertexCount = 0;

    private final int[] indices;
    private int indexCount = 0;

    private int vaoID, vboID, eboID;
    private final Texture[] textures;
    private int textureCount = 1;
    private boolean hasRoom = true;
    private final Texture whiteTexture;

    public RenderBatch(Texture whiteTexture) {
        this.vertices = new Vertex[MAX_BATCH_SIZE];
        this.indices = new int[MAX_INDICES];
        this.textures = new Texture[16];
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
        return textureCount < 16;
    }

    public boolean hasTextures(Collection<Texture> texs) {
        if (texs == null || texs.isEmpty()) return true;
        int needed = 0;
        for (Texture t : texs) {
            if (t != null && !hasTexture(t)) {
                needed++;
            }
        }
        return textureCount + needed <= 16;
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
        if (textureCount < 16) {
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

        int[] slotMap = null;
        if (texturesList != null && !texturesList.isEmpty()) {
            slotMap = new int[texturesList.size()];
            for (int i = 0; i < texturesList.size(); i++) {
                slotMap[i] = (int) addTexture(texturesList.get(i));
            }
        }

        int baseOffset = vertexCount;
        for (Vertex v : verts) {
            if (vertexCount >= MAX_BATCH_SIZE) break;
            Vertex copy = new Vertex(v);
            if (slotMap != null) {
                int tid = (int) copy.texID;
                if (tid >= 0 && tid < slotMap.length) {
                    copy.texID = (float) slotMap[tid];
                } else {
                    copy.texID = (float) slotMap[0];
                }
            } else {
                copy.texID = 0.0f;
            }
            vertices[vertexCount++] = copy;
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

        int[] slotMap = null;
        float fallbackSlot = 0.0f;

        if (activeTextures != null && !activeTextures.isEmpty()) {
            slotMap = new int[activeTextures.size()];
            for (int i = 0; i < activeTextures.size(); i++) {
                slotMap[i] = (int) addTexture(activeTextures.get(i));
            }
            fallbackSlot = (float) slotMap[0];
        } else if (mesh.getTexture() != null) {
            fallbackSlot = addTexture(mesh.getTexture());
        }

        int baseOffset = vertexCount;
        for (Vertex v : meshVerts) {
            if (vertexCount >= MAX_BATCH_SIZE) break;
            Vertex vert = new Vertex(v);
            if (transform != null) {
                Vector4f pos = new Vector4f(vert.x, vert.y, vert.z, 1.0f);
                transform.transform(pos);
                vert.x = pos.x;
                vert.y = pos.y;
                vert.z = pos.z;

                Vector3f norm = new Vector3f(vert.nx, vert.ny, vert.nz);
                transform.transformDirection(norm).normalize();
                vert.nx = norm.x;
                vert.ny = norm.y;
                vert.nz = norm.z;
            }
            if (mesh.getColor() != null && (vert.r == 1.0f && vert.g == 1.0f && vert.b == 1.0f && vert.a == 1.0f)) {
                vert.r = mesh.getColor().r;
                vert.g = mesh.getColor().g;
                vert.b = mesh.getColor().b;
                vert.a = mesh.getColor().a;
            }
            if (slotMap != null) {
                int tid = (int) vert.texID;
                if (tid >= 0 && tid < slotMap.length) {
                    vert.texID = (float) slotMap[tid];
                } else {
                    vert.texID = fallbackSlot;
                }
            } else {
                vert.texID = fallbackSlot;
            }
            vertices[vertexCount++] = vert;
        }

        List<Integer> meshIndices = mesh.getIndices();
        if (!meshIndices.isEmpty()) {
            for (int idx : meshIndices) {
                if (indexCount >= MAX_INDICES) break;
                indices[indexCount++] = baseOffset + idx;
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

        float[] floatBuffer = new float[vertexCount * Vertex.ELEMENT_SIZE];
        for (int i = 0; i < vertexCount; i++) {
            vertices[i].write(floatBuffer, i * Vertex.ELEMENT_SIZE);
        }

        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, floatBuffer);

        int[] indexSubBuffer = new int[indexCount];
        System.arraycopy(indices, 0, indexSubBuffer, 0, indexCount);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, indexSubBuffer);

        for (int i = 0; i < 16; i++) {
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

        for (int i = 0; i < 16; i++) {
            if (textures[i] != null) {
                textures[i].unbind();
            } else if (whiteTexture != null) {
                whiteTexture.unbind();
            }
        }
        return true;
    }
}