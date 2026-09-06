package net.meowsers.Peach.Structures;

public class Vertex {
    public float x, y, z;
    public float nx, ny, nz;
    public float r, g, b, a;
    public float u, v;
    public float texID;

    public static final int ELEMENT_SIZE = 13; // 3 pos + 3 normal + 4 color + 2 uv + 1 texID
    public static final int BYTES = ELEMENT_SIZE * Float.BYTES;

    public Vertex(float x, float y, float z, float nx, float ny, float nz, float r, float g, float b, float a, float u, float v) {
        this.x = x; this.y = y; this.z = z;
        this.nx = nx; this.ny = ny; this.nz = nz;
        this.r = r; this.g = g; this.b = b; this.a = a;
        this.u = u; this.v = v;
        this.texID = 0.0f;
    }

    public Vertex(float x, float y, float z, float r, float g, float b, float a, float u, float v) {
        this(x, y, z, 0.0f, 0.0f, 1.0f, r, g, b, a, u, v);
    }

    public Vertex(Vertex other) {
        this.x = other.x; this.y = other.y; this.z = other.z;
        this.nx = other.nx; this.ny = other.ny; this.nz = other.nz;
        this.r = other.r; this.g = other.g; this.b = other.b; this.a = other.a;
        this.u = other.u; this.v = other.v;
        this.texID = other.texID;
    }

    public Vertex(float x, float y, float z, float u, float v) {
        this(x, y, z, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, u, v);
    }

    public Vertex(float x, float y, float z) {
        this(x, y, z, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f);
    }

    public Vertex(float x, float y, float z, Color color, float u, float v) {
        this(x, y, z, 0.0f, 0.0f, 1.0f, color.r, color.g, color.b, color.a, u, v);
    }

    public Vertex(float x, float y, float z, Color color) {
        this(x, y, z, 0.0f, 0.0f, 1.0f, color.r, color.g, color.b, color.a, 0.0f, 0.0f);
    }

    public Vertex(float x, float y, Color color) {
        this(x, y, 0.0f, 0.0f, 0.0f, 1.0f, color.r, color.g, color.b, color.a, 0.0f, 0.0f);
    }

    public Vertex(float x, float y, Color color, float u, float v) {
        this(x, y, 0.0f, 0.0f, 0.0f, 1.0f, color.r, color.g, color.b, color.a, u, v);
    }

    public void write(float[] buffer, int offset) {
        buffer[offset + 0] = x;
        buffer[offset + 1] = y;
        buffer[offset + 2] = z;
        buffer[offset + 3] = nx;
        buffer[offset + 4] = ny;
        buffer[offset + 5] = nz;
        buffer[offset + 6] = r;
        buffer[offset + 7] = g;
        buffer[offset + 8] = b;
        buffer[offset + 9] = a;
        buffer[offset + 10] = u;
        buffer[offset + 11] = v;
        buffer[offset + 12] = texID;
    }
}