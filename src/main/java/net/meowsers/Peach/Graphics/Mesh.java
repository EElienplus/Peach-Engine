package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Vertex;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Mesh {
    private String name = "Mesh";
    private List<Vertex> vertices = new ArrayList<>();
    private List<Integer> indices = new ArrayList<>();
    private Texture texture;
    private List<Texture> textures = new ArrayList<>();
    private Color color = Color.White;

    public Mesh() {
    }

    public Mesh(String name) {
        this.name = name;
    }
    public Mesh(List<Vertex> vertices, List<Integer> indices, Texture texture) {
        if (vertices != null) {
            for (Vertex v : vertices) {
                this.vertices.add(new Vertex(v));
            }
        }
        if (indices != null) {
            this.indices.addAll(indices);
        }
        setTexture(texture);
    }
    public Mesh(List<Vertex> vertices, List<Integer> indices, List<Texture> textures) {
        if (vertices != null) {
            for (Vertex v : vertices) {
                this.vertices.add(new Vertex(v));
            }
        }
        if (indices != null) {
            this.indices.addAll(indices);
        }
        setTextures(textures);
    }
    public Mesh(List<Vertex> vertices, List<Integer> indices, Texture... textures) {
        this(vertices, indices, textures != null ? Arrays.asList(textures) : null);
    }
    public Mesh(List<Vertex> vertices, List<Integer> indices) {
        this(vertices, indices, (Texture) null);
    }
    public Mesh(List<Vertex> vertices, Texture texture) {
        this(vertices, null, texture);
    }
    public Mesh(List<Vertex> vertices, Texture... textures) {
        this(vertices, null, textures != null ? Arrays.asList(textures) : null);
    }
    public Mesh(List<Vertex> vertices) {
        this(vertices, null, (Texture) null);
    }
    public Mesh(Vertex[] vertices, int[] indices, Texture texture) {
        if (vertices != null) {
            for (Vertex v : vertices) {
                this.vertices.add(new Vertex(v));
            }
        }
        if (indices != null) {
            for (int idx : indices) {
                this.indices.add(idx);
            }
        }
        setTexture(texture);
    }
    public Mesh(Vertex[] vertices, int[] indices, List<Texture> textures) {
        if (vertices != null) {
            for (Vertex v : vertices) {
                this.vertices.add(new Vertex(v));
            }
        }
        if (indices != null) {
            for (int idx : indices) {
                this.indices.add(idx);
            }
        }
        setTextures(textures);
    }
    public Mesh(Vertex[] vertices, int[] indices, Texture... textures) {
        this(vertices, indices, textures != null ? Arrays.asList(textures) : null);
    }
    public Mesh(Vertex[] vertices, int[] indices) {
        this(vertices, indices, (Texture) null);
    }
    public Mesh(Vertex[] vertices, Texture texture) {
        this(vertices, (int[]) null, texture);
    }
    public Mesh(Vertex[] vertices, List<Texture> textures) {
        this(vertices, (int[]) null, textures);
    }
    public Mesh(Vertex[] vertices, Texture... textures) {
        this(vertices, (int[]) null, textures != null ? Arrays.asList(textures) : null);
    }
    public Mesh(Vertex[] vertices) {
        this(vertices, (int[]) null, (Texture) null);
    }
    public Mesh(Mesh other) {
        if (other != null) {
            this.name = other.name;
            for (Vertex v : other.vertices) {
                this.vertices.add(new Vertex(v));
            }
            this.indices.addAll(other.indices);
            this.texture = other.texture;
            this.textures.addAll(other.textures);
            this.color = other.color;
        }
    }

    public static Mesh load(String filepath) {
        Model model = Model.load(filepath);
        return model.toMesh();
    }

    public Mesh add(Mesh other) {
        return add(other, null);
    }
    public Mesh add(Mesh other, Matrix4f transform) {
        if (other == null) return this;

        int baseOffset = this.vertices.size();

        int[] texMapping = null;
        if (!other.textures.isEmpty()) {
            texMapping = new int[other.textures.size()];
            for (int i = 0; i < other.textures.size(); i++) {
                Texture t = other.textures.get(i);
                int idx = this.textures.indexOf(t);
                if (idx == -1 && t != null) {
                    idx = this.textures.size();
                    this.textures.add(t);
                }
                texMapping[i] = idx >= 0 ? idx : 0;
            }
        } else if (other.texture != null) {
            int idx = this.textures.indexOf(other.texture);
            if (idx == -1) {
                idx = this.textures.size();
                this.textures.add(other.texture);
            }
        }

        for (Vertex v : other.vertices) {
            Vertex newV = new Vertex(v);
            if (transform != null) {
                Vector4f pos = new Vector4f(newV.x, newV.y, newV.z, 1.0f);
                transform.transform(pos);
                newV.x = pos.x;
                newV.y = pos.y;
                newV.z = pos.z;

                Vector3f norm = new Vector3f(newV.nx, newV.ny, newV.nz);
                transform.transformDirection(norm).normalize();
                newV.nx = norm.x;
                newV.ny = norm.y;
                newV.nz = norm.z;
            }
            if (texMapping != null) {
                int tid = (int) newV.texID;
                if (tid >= 0 && tid < texMapping.length) {
                    newV.texID = texMapping[tid];
                }
            } else if (other.texture != null) {
                int idx = this.textures.indexOf(other.texture);
                if (idx >= 0) {
                    newV.texID = idx;
                }
            }
            this.vertices.add(newV);
        }

        if (!other.indices.isEmpty()) {
            for (int idx : other.indices) {
                this.indices.add(baseOffset + idx);
            }
        } else {
            for (int i = 0; i < other.vertices.size(); i++) {
                this.indices.add(baseOffset + i);
            }
        }

        if (this.texture == null) {
            if (other.texture != null) {
                this.texture = other.texture;
            } else if (!this.textures.isEmpty()) {
                this.texture = this.textures.get(0);
            }
        }

        return this;
    }
    public Mesh add(Model model) {
        return add(model, null);
    }
    public Mesh add(Model model, Matrix4f transform) {
        if (model == null) return this;
        for (Mesh m : model.getMeshes()) {
            Matrix4f combined = new Matrix4f();
            if (transform != null) {
                combined.set(transform).mul(model.getModelMatrix());
            } else {
                combined.set(model.getModelMatrix());
            }
            add(m, combined);
        }
        return this;
    }
    public Mesh add(List<Vertex> verts, List<Integer> inds) {
        if (verts == null) return this;
        int baseOffset = this.vertices.size();
        for (Vertex v : verts) {
            this.vertices.add(new Vertex(v));
        }
        if (inds != null && !inds.isEmpty()) {
            for (int idx : inds) {
                this.indices.add(baseOffset + idx);
            }
        } else {
            for (int i = 0; i < verts.size(); i++) {
                this.indices.add(baseOffset + i);
            }
        }
        return this;
    }
    public Mesh add(List<Vertex> verts) {
        return add(verts, null);
    }
    public Mesh add(Vertex... verts) {
        return add(Arrays.asList(verts), null);
    }

    public Mesh addVertex(Vertex vertex) {
        if (vertex != null) {
            this.vertices.add(new Vertex(vertex));
        }
        return this;
    }
    public Mesh addIndex(int index) {
        this.indices.add(index);
        return this;
    }
    public Mesh addIndices(int... indices) {
        if (indices != null) {
            for (int idx : indices) {
                this.indices.add(idx);
            }
        }
        return this;
    }
    public Mesh addTriangle(Vertex v0, Vertex v1, Vertex v2) {
        int baseOffset = this.vertices.size();
        this.vertices.add(new Vertex(v0));
        this.vertices.add(new Vertex(v1));
        this.vertices.add(new Vertex(v2));
        this.indices.add(baseOffset);
        this.indices.add(baseOffset + 1);
        this.indices.add(baseOffset + 2);
        return this;
    }
    public Mesh addQuad(Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
        int baseOffset = this.vertices.size();
        this.vertices.add(new Vertex(v0));
        this.vertices.add(new Vertex(v1));
        this.vertices.add(new Vertex(v2));
        this.vertices.add(new Vertex(v3));
        this.indices.add(baseOffset);
        this.indices.add(baseOffset + 1);
        this.indices.add(baseOffset + 2);
        this.indices.add(baseOffset + 2);
        this.indices.add(baseOffset + 3);
        this.indices.add(baseOffset);
        return this;
    }

    public static Mesh combine(Mesh... meshes) {
        return combine(Arrays.asList(meshes));
    }

    public static Mesh combine(Collection<Mesh> meshes) {
        Mesh combined = new Mesh("CombinedMesh");
        if (meshes != null) {
            for (Mesh m : meshes) {
                combined.add(m);
            }
        }
        return combined;
    }
    public Mesh transform(Matrix4f matrix) {
        if (matrix == null) return this;
        for (Vertex v : vertices) {
            Vector4f pos = new Vector4f(v.x, v.y, v.z, 1.0f);
            matrix.transform(pos);
            v.x = pos.x;
            v.y = pos.y;
            v.z = pos.z;

            Vector3f norm = new Vector3f(v.nx, v.ny, v.nz);
            matrix.transformDirection(norm);
            if (norm.lengthSquared() > 0.00001f) {
                norm.normalize();
                v.nx = norm.x;
                v.ny = norm.y;
                v.nz = norm.z;
            }
        }
        return this;
    }
    public Mesh calculateNormals() {
        if (vertices.isEmpty()) return this;

        for (Vertex v : vertices) {
            v.nx = 0; v.ny = 0; v.nz = 0;
        }

        if (!indices.isEmpty()) {
            int numTris = indices.size() / 3;
            for (int t = 0; t < numTris; t++) {
                int i0 = indices.get(t * 3 + 0);
                int i1 = indices.get(t * 3 + 1);
                int i2 = indices.get(t * 3 + 2);

                if (i0 < vertices.size() && i1 < vertices.size() && i2 < vertices.size()) {
                    Vertex v0 = vertices.get(i0);
                    Vertex v1 = vertices.get(i1);
                    Vertex v2 = vertices.get(i2);

                    Vector3f p0 = new Vector3f(v0.x, v0.y, v0.z);
                    Vector3f p1 = new Vector3f(v1.x, v1.y, v1.z);
                    Vector3f p2 = new Vector3f(v2.x, v2.y, v2.z);

                    Vector3f e1 = new Vector3f(p1).sub(p0);
                    Vector3f e2 = new Vector3f(p2).sub(p0);
                    Vector3f normal = new Vector3f(e1).cross(e2);
                    if (normal.lengthSquared() > 0.00001f) {
                        normal.normalize();
                        v0.nx += normal.x; v0.ny += normal.y; v0.nz += normal.z;
                        v1.nx += normal.x; v1.ny += normal.y; v1.nz += normal.z;
                        v2.nx += normal.x; v2.ny += normal.y; v2.nz += normal.z;
                    }
                }
            }
        } else {
            int numVerts = vertices.size();
            if (numVerts % 3 == 0) {
                for (int i = 0; i < numVerts; i += 3) {
                    Vertex v0 = vertices.get(i);
                    Vertex v1 = vertices.get(i + 1);
                    Vertex v2 = vertices.get(i + 2);
                    Vector3f e1 = new Vector3f(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z);
                    Vector3f e2 = new Vector3f(v2.x - v0.x, v2.y - v0.y, v2.z - v0.z);
                    Vector3f normal = new Vector3f(e1).cross(e2);
                    if (normal.lengthSquared() > 0.00001f) {
                        normal.normalize();
                        v0.nx += normal.x; v0.ny += normal.y; v0.nz += normal.z;
                        v1.nx += normal.x; v1.ny += normal.y; v1.nz += normal.z;
                        v2.nx += normal.x; v2.ny += normal.y; v2.nz += normal.z;
                    }
                }
            }
        }

        for (Vertex v : vertices) {
            float lenSq = v.nx * v.nx + v.ny * v.ny + v.nz * v.nz;
            if (lenSq > 0.00001f) {
                float invLen = (float) (1.0 / Math.sqrt(lenSq));
                v.nx *= invLen; v.ny *= invLen; v.nz *= invLen;
            } else {
                v.nx = 0.0f; v.ny = 0.0f; v.nz = 1.0f;
            }
        }

        return this;
    }
    public Mesh translate(Vector3f offset) {
        if (offset == null) return this;
        return transform(new Matrix4f().translate(offset));
    }

    public Mesh rotate(float angleRadians, float axisX, float axisY, float axisZ) {
        return transform(new Matrix4f().rotate(angleRadians, axisX, axisY, axisZ));
    }
    public Mesh rotateX(float angleRadians) {
        return transform(new Matrix4f().rotateX(angleRadians));
    }
    public Mesh rotateY(float angleRadians) {
        return transform(new Matrix4f().rotateY(angleRadians));
    }
    public Mesh rotateZ(float angleRadians) {
        return transform(new Matrix4f().rotateZ(angleRadians));
    }

    public Mesh scale(float sx, float sy, float sz) {
        return transform(new Matrix4f().scale(sx, sy, sz));
    }
    public Mesh scale(float s) {
        return scale(s, s, s);
    }

    public Vector3f getMinBounds() {
        if (vertices.isEmpty()) return new Vector3f(0, 0, 0);
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        for (Vertex v : vertices) {
            if (v.x < minX) minX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.z < minZ) minZ = v.z;
        }
        return new Vector3f(minX, minY, minZ);
    }
    public Vector3f getMaxBounds() {
        if (vertices.isEmpty()) return new Vector3f(0, 0, 0);
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (Vertex v : vertices) {
            if (v.x > maxX) maxX = v.x;
            if (v.y > maxY) maxY = v.y;
            if (v.z > maxZ) maxZ = v.z;
        }
        return new Vector3f(maxX, maxY, maxZ);
    }

    public Mesh center() {
        if (vertices.isEmpty()) return this;
        Vector3f c = getCenter();
        return translate(new Vector3f(-c.x, -c.y, -c.z));
    }
    public Vector3f getCenter() {
        if (vertices.isEmpty()) return new Vector3f(0, 0, 0);
        Vector3f min = getMinBounds();
        Vector3f max = getMaxBounds();
        return new Vector3f((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f, (min.z + max.z) * 0.5f);
    }
    public Vector3f getSize() {
        if (vertices.isEmpty()) return new Vector3f(0, 0, 0);
        Vector3f min = getMinBounds();
        Vector3f max = getMaxBounds();
        return new Vector3f(max.x - min.x, max.y - min.y, max.z - min.z);
    }

    public Mesh normalize() {
        return fitToSize(1.0f);
    }
    public Mesh fitToSize(float targetSize) {
        if (vertices.isEmpty()) return this;
        center();
        Vector3f size = getSize();
        float maxDim = Math.max(size.x, Math.max(size.y, size.z));
        if (maxDim > 0.00001f) {
            float s = targetSize / maxDim;
            scale(s);
        }
        return this;
    }

    public Mesh copy() {
        return new Mesh(this);
    }

    public static Mesh createQuad(float x, float y, float width, float height, Color color, Texture texture) {
        Color c = color != null ? color : Color.White;
        Vertex v0 = new Vertex(x, y, 0.0f, 0.0f, 0.0f, 1.0f, c.r, c.g, c.b, c.a, 0.0f, 0.0f);
        Vertex v1 = new Vertex(x + width, y, 0.0f, 0.0f, 0.0f, 1.0f, c.r, c.g, c.b, c.a, 1.0f, 0.0f);
        Vertex v2 = new Vertex(x + width, y + height, 0.0f, 0.0f, 0.0f, 1.0f, c.r, c.g, c.b, c.a, 1.0f, 1.0f);
        Vertex v3 = new Vertex(x, y + height, 0.0f, 0.0f, 0.0f, 1.0f, c.r, c.g, c.b, c.a, 0.0f, 1.0f);

        Mesh mesh = new Mesh("Quad");
        mesh.addQuad(v0, v1, v2, v3);
        mesh.setTexture(texture);
        mesh.setColor(c);
        return mesh;
    }
    public static Mesh createQuad(float x, float y, float width, float height, Color color) {
        return createQuad(x, y, width, height, color, null);
    }
    public static Mesh createQuad(float x, float y, float width, float height, Texture texture) {
        return createQuad(x, y, width, height, Color.White, texture);
    }
    public static Mesh createQuad(float width, float height, Color color, Texture texture) {
        return createQuad(0, 0, width, height, color, texture);
    }
    public static Mesh createQuad(float width, float height, Color color) {
        return createQuad(0, 0, width, height, color, null);
    }
    public static Mesh createQuad(float width, float height, Texture texture) {
        return createQuad(0, 0, width, height, Color.White, texture);
    }
    public static Mesh createPlane(float width, float depth, Color color, Texture texture, boolean upsideDown) {
        Color c = color != null ? color : Color.White;
        float halfW = width * 0.5f;
        float halfD = depth * 0.5f;

        float normalY = upsideDown ? -1.0f : 1.0f;

        Vertex v0 = new Vertex(-halfW, 0.0f, -halfD, 0.0f, normalY, 0.0f, c.r, c.g, c.b, c.a, 0.0f, 0.0f);
        Vertex v1 = new Vertex(halfW, 0.0f, -halfD, 0.0f, normalY, 0.0f, c.r, c.g, c.b, c.a, 1.0f, 0.0f);
        Vertex v2 = new Vertex(halfW, 0.0f, halfD, 0.0f, normalY, 0.0f, c.r, c.g, c.b, c.a, 1.0f, 1.0f);
        Vertex v3 = new Vertex(-halfW, 0.0f, halfD, 0.0f, normalY, 0.0f, c.r, c.g, c.b, c.a, 0.0f, 1.0f);

        Mesh mesh = new Mesh("Plane");

        if (!upsideDown) {
            mesh.addQuad(v3, v2, v1, v0);
        } else {
            mesh.addQuad(v0, v1, v2, v3);
        }

        mesh.setTexture(texture);
        mesh.setColor(c);
        return mesh;
    }
    public static Mesh createCube(float size, Color color, Texture texture) {
        Color c = color != null ? color : Color.White;
        float h = size * 0.5f;

        Mesh mesh = new Mesh("Cube");

        // Front face (Z+)
        mesh.addQuad(
                new Vertex(-h, -h,  h, 0, 0, 1, c.r, c.g, c.b, c.a, 0.0f, 0.0f),
                new Vertex( h, -h,  h, 0, 0, 1, c.r, c.g, c.b, c.a, 1.0f, 0.0f),
                new Vertex( h,  h,  h, 0, 0, 1, c.r, c.g, c.b, c.a, 1.0f, 1.0f),
                new Vertex(-h,  h,  h, 0, 0, 1, c.r, c.g, c.b, c.a, 0.0f, 1.0f)
        );

        // Back face (Z-)
        mesh.addQuad(
                new Vertex( h, -h, -h, 0, 0, -1, c.r, c.g, c.b, c.a, 0.0f, 0.0f),
                new Vertex(-h, -h, -h, 0, 0, -1, c.r, c.g, c.b, c.a, 1.0f, 0.0f),
                new Vertex(-h,  h, -h, 0, 0, -1, c.r, c.g, c.b, c.a, 1.0f, 1.0f),
                new Vertex( h,  h, -h, 0, 0, -1, c.r, c.g, c.b, c.a, 0.0f, 1.0f)
        );

        // Top face (Y+)
        mesh.addQuad(
                new Vertex(-h,  h,  h, 0, 1, 0, c.r, c.g, c.b, c.a, 0.0f, 0.0f),
                new Vertex( h,  h,  h, 0, 1, 0, c.r, c.g, c.b, c.a, 1.0f, 0.0f),
                new Vertex( h,  h, -h, 0, 1, 0, c.r, c.g, c.b, c.a, 1.0f, 1.0f),
                new Vertex(-h,  h, -h, 0, 1, 0, c.r, c.g, c.b, c.a, 0.0f, 1.0f)
        );

        // Bottom face (Y-)
        mesh.addQuad(
                new Vertex(-h, -h, -h, 0, -1, 0, c.r, c.g, c.b, c.a, 0.0f, 0.0f),
                new Vertex( h, -h, -h, 0, -1, 0, c.r, c.g, c.b, c.a, 1.0f, 0.0f),
                new Vertex( h, -h,  h, 0, -1, 0, c.r, c.g, c.b, c.a, 1.0f, 1.0f),
                new Vertex(-h, -h,  h, 0, -1, 0, c.r, c.g, c.b, c.a, 0.0f, 1.0f)
        );

        // Right face (X+)
        mesh.addQuad(
                new Vertex( h, -h,  h, 1, 0, 0, c.r, c.g, c.b, c.a, 0.0f, 0.0f),
                new Vertex( h, -h, -h, 1, 0, 0, c.r, c.g, c.b, c.a, 1.0f, 0.0f),
                new Vertex( h,  h, -h, 1, 0, 0, c.r, c.g, c.b, c.a, 1.0f, 1.0f),
                new Vertex( h,  h,  h, 1, 0, 0, c.r, c.g, c.b, c.a, 0.0f, 1.0f)
        );

        // Left face (X-)
        mesh.addQuad(
                new Vertex(-h, -h, -h, -1, 0, 0, c.r, c.g, c.b, c.a, 0.0f, 0.0f),
                new Vertex(-h, -h,  h, -1, 0, 0, c.r, c.g, c.b, c.a, 1.0f, 0.0f),
                new Vertex(-h,  h,  h, -1, 0, 0, c.r, c.g, c.b, c.a, 1.0f, 1.0f),
                new Vertex(-h,  h, -h, -1, 0, 0, c.r, c.g, c.b, c.a, 0.0f, 1.0f)
        );

        mesh.setTexture(texture);
        mesh.setColor(c);
        return mesh;
    }
    public static Mesh createCube(float size, Texture texture) {
        return createCube(size, Color.White, texture);
    }
    public static Mesh createCube(float size, Color color) {
        return createCube(size, color, null);
    }
    public static Mesh createCube(float size) {
        return createCube(size, Color.White, null);
    }

    public static Mesh createSphere(float radius, int slices, int stacks, Color color, Texture texture) {
        Color c = color != null ? color : Color.White;
        Mesh mesh = new Mesh("Sphere");

        for (int i = 0; i <= stacks; i++) {
            float v = (float) i / (float) stacks;
            float phi = v * (float) Math.PI;

            for (int j = 0; j <= slices; j++) {
                float u = (float) j / (float) slices;
                float theta = u * (float) (Math.PI * 2.0);

                float cx = (float) (Math.cos(theta) * Math.sin(phi));
                float cy = (float) Math.cos(phi);
                float cz = (float) (Math.sin(theta) * Math.sin(phi));

                float x = cx * radius;
                float y = cy * radius;
                float z = cz * radius;

                mesh.vertices.add(new Vertex(x, y, z, cx, cy, cz, c.r, c.g, c.b, c.a, u, v));
            }
        }

        int stride = slices + 1;
        for (int i = 0; i < stacks; i++) {
            for (int j = 0; j < slices; j++) {
                int first = i * stride + j;
                int second = first + stride;

                mesh.indices.add(first);
                mesh.indices.add(second);
                mesh.indices.add(first + 1);

                mesh.indices.add(second);
                mesh.indices.add(second + 1);
                mesh.indices.add(first + 1);
            }
        }

        mesh.setTexture(texture);
        mesh.setColor(c);
        return mesh;
    }
    public static Mesh createSphere(float radius, int slices, int stacks, Color color) {
        return createSphere(radius, slices, stacks, color, null);
    }
    public static Mesh createSphere(float radius, int slices, int stacks) {
        return createSphere(radius, slices, stacks, Color.White, null);
    }
    public static Mesh createSphere(float radius, Color color) {
        return createSphere(radius, 16, 16, color, null);
    }
    public static Mesh createSphere(float radius) {
        return createSphere(radius, 16, 16, Color.White, null);
    }
    public static Mesh createSphere() {
        return createSphere(0.5f, 16, 16, Color.White, null);
    }

    public List<Vertex> getVertices() {
        return vertices;
    }
    public void setVertices(List<Vertex> newVertices) {
        this.vertices.clear();
        if (newVertices != null) {
            for (Vertex v : newVertices) {
                this.vertices.add(new Vertex(v));
            }
        }
    }
    public List<Integer> getIndices() {
        return indices;
    }
    public int[] getIndicesArray() {
        int[] arr = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            arr[i] = indices.get(i);
        }
        return arr;
    }
    public void setIndices(List<Integer> newIndices) {
        this.indices.clear();
        if (newIndices != null) {
            this.indices.addAll(newIndices);
        }
    }

    public Texture getTexture() {
        if (texture != null) return texture;
        if (!textures.isEmpty()) return textures.get(0);
        return null;
    }
    public void setTexture(Texture texture) {
        this.texture = texture;
        if (texture != null && !textures.contains(texture)) {
            this.textures.add(texture);
        }
    }
    public List<Texture> getTextures() {
        return textures;
    }
    public void setTextures(List<Texture> textures) {
        this.textures.clear();
        if (textures != null) {
            this.textures.addAll(textures);
            if (!this.textures.isEmpty()) {
                this.texture = this.textures.get(0);
            }
        }
    }
    public void setTextures(Texture... textures) {
        setTextures(textures != null ? Arrays.asList(textures) : null);
    }
    public void addTexture(Texture texture) {
        if (texture != null) {
            if (!this.textures.contains(texture)) {
                this.textures.add(texture);
            }
            if (this.texture == null) {
                this.texture = texture;
            }
        }
    }
    public void addTextures(Texture... textures) {
        if (textures != null) {
            for (Texture t : textures) {
                addTexture(t);
            }
        }
    }
    public void addTextures(Collection<Texture> textures) {
        if (textures != null) {
            for (Texture t : textures) {
                addTexture(t);
            }
        }
    }
    public Texture getTexture(int index) {
        if (index >= 0 && index < textures.size()) {
            return textures.get(index);
        }
        return getTexture();
    }
    public boolean hasTextures() {
        return texture != null || !textures.isEmpty();
    }
    public Color getColor() {
        return color;
    }
    public void setColor(Color color) {
        this.color = color != null ? new Color(color) : new Color(1.0f, 1.0f, 1.0f, 1.0f);
        for (Vertex v : vertices) {
            v.r = this.color.r;
            v.g = this.color.g;
            v.b = this.color.b;
            v.a = this.color.a;
        }
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getVertexCount() {
        return vertices.size();
    }
    public int getIndexCount() {
        return indices.size();
    }
    public int getTriangleCount() {
        return !indices.isEmpty() ? indices.size() / 3 : vertices.size() / 3;
    }
    public List<Vertex> getUnindexedVertices() {
        if (indices.isEmpty()) {
            return new ArrayList<>(vertices);
        }
        List<Vertex> unindexed = new ArrayList<>(indices.size());
        for (int idx : indices) {
            if (idx >= 0 && idx < vertices.size()) {
                unindexed.add(new Vertex(vertices.get(idx)));
            }
        }
        return unindexed;
    }

    /**
     * Calculates the face normal of a specific triangle.
     */
    public Vector3f getFaceNormal(int triangleIndex) {
        int i0, i1, i2;
        if (!indices.isEmpty()) {
            if (triangleIndex * 3 + 2 >= indices.size()) return new Vector3f(0, 0, 1);
            i0 = indices.get(triangleIndex * 3);
            i1 = indices.get(triangleIndex * 3 + 1);
            i2 = indices.get(triangleIndex * 3 + 2);
        } else {
            if (triangleIndex * 3 + 2 >= vertices.size()) return new Vector3f(0, 0, 1);
            i0 = triangleIndex * 3;
            i1 = triangleIndex * 3 + 1;
            i2 = triangleIndex * 3 + 2;
        }
        if (i0 >= vertices.size() || i1 >= vertices.size() || i2 >= vertices.size()) {
            return new Vector3f(0, 0, 1);
        }
        Vertex v0 = vertices.get(i0);
        Vertex v1 = vertices.get(i1);
        Vertex v2 = vertices.get(i2);
        Vector3f e1 = new Vector3f(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z);
        Vector3f e2 = new Vector3f(v2.x - v0.x, v2.y - v0.y, v2.z - v0.z);
        Vector3f normal = new Vector3f(e1).cross(e2);
        if (normal.lengthSquared() > 0.00001f) {
            normal.normalize();
        } else {
            normal.set(0, 0, 1);
        }
        return normal;
    }

    /**
     * Checks if a triangle is front-facing with respect to a camera position and optional world transform.
     */
    public boolean isFaceVisible(int triangleIndex, Vector3f cameraPos, Matrix4f transform) {
        if (cameraPos == null) return true;
        int i0, i1, i2;
        if (!indices.isEmpty()) {
            if (triangleIndex * 3 + 2 >= indices.size()) return true;
            i0 = indices.get(triangleIndex * 3);
            i1 = indices.get(triangleIndex * 3 + 1);
            i2 = indices.get(triangleIndex * 3 + 2);
        } else {
            if (triangleIndex * 3 + 2 >= vertices.size()) return true;
            i0 = triangleIndex * 3;
            i1 = triangleIndex * 3 + 1;
            i2 = triangleIndex * 3 + 2;
        }
        if (i0 >= vertices.size() || i1 >= vertices.size() || i2 >= vertices.size()) return true;

        Vertex v0 = vertices.get(i0);
        Vertex v1 = vertices.get(i1);
        Vertex v2 = vertices.get(i2);

        Vector4f p0 = new Vector4f(v0.x, v0.y, v0.z, 1.0f);
        Vector4f p1 = new Vector4f(v1.x, v1.y, v1.z, 1.0f);
        Vector4f p2 = new Vector4f(v2.x, v2.y, v2.z, 1.0f);
        if (transform != null) {
            transform.transform(p0);
            transform.transform(p1);
            transform.transform(p2);
        }

        Vector3f e1 = new Vector3f(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z);
        Vector3f e2 = new Vector3f(p2.x - p0.x, p2.y - p0.y, p2.z - p0.z);
        Vector3f normal = new Vector3f(e1).cross(e2);

        Vector3f viewDir = new Vector3f(cameraPos.x - p0.x, cameraPos.y - p0.y, cameraPos.z - p0.z);
        return normal.dot(viewDir) > 0.0f;
    }

    /**
     * Performs geometric backface culling, returning a new Mesh with only front-facing triangles.
     */
    public Mesh cullBackfaces(Vector3f cameraPos, Matrix4f transform) {
        if (cameraPos == null) return new Mesh(this);
        Mesh culled = new Mesh(this.name + "_Culled");
        culled.setColor(this.color);
        culled.setTexture(this.texture);
        culled.setTextures(this.textures);

        int numTriangles = getTriangleCount();
        for (int t = 0; t < numTriangles; t++) {
            if (isFaceVisible(t, cameraPos, transform)) {
                int i0 = !indices.isEmpty() ? indices.get(t * 3) : t * 3;
                int i1 = !indices.isEmpty() ? indices.get(t * 3 + 1) : t * 3 + 1;
                int i2 = !indices.isEmpty() ? indices.get(t * 3 + 2) : t * 3 + 2;
                if (i0 < vertices.size() && i1 < vertices.size() && i2 < vertices.size()) {
                    culled.addTriangle(vertices.get(i0), vertices.get(i1), vertices.get(i2));
                }
            }
        }
        return culled;
    }

    public Mesh cullBackfaces(Vector3f cameraPos) {
        return cullBackfaces(cameraPos, null);
    }

    /**
     * Performs geometric normal culling against a directional view vector, returning a new Mesh with front-facing geometry.
     */
    public Mesh cullNormals(Vector3f viewDir, Matrix4f transform) {
        if (viewDir == null) return new Mesh(this);
        Mesh culled = new Mesh(this.name + "_NormalCulled");
        culled.setColor(this.color);
        culled.setTexture(this.texture);
        culled.setTextures(this.textures);

        int numTriangles = getTriangleCount();
        for (int t = 0; t < numTriangles; t++) {
            Vector3f fn = getFaceNormal(t);
            if (transform != null) {
                Vector3f tf = new Vector3f(fn);
                transform.transformDirection(tf);
                if (tf.lengthSquared() > 0.00001f) fn = tf.normalize();
            }
            if (fn.dot(viewDir) < 0.0f) { // Normal pointing towards viewer (opposite to view ray)
                int i0 = !indices.isEmpty() ? indices.get(t * 3) : t * 3;
                int i1 = !indices.isEmpty() ? indices.get(t * 3 + 1) : t * 3 + 1;
                int i2 = !indices.isEmpty() ? indices.get(t * 3 + 2) : t * 3 + 2;
                if (i0 < vertices.size() && i1 < vertices.size() && i2 < vertices.size()) {
                    culled.addTriangle(vertices.get(i0), vertices.get(i1), vertices.get(i2));
                }
            }
        }
        return culled;
    }

    public Mesh cullNormals(Vector3f viewDir) {
        return cullNormals(viewDir, null);
    }
}
