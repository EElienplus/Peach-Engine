package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Vertex;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RenderBatchTest {

    @Test
    public void testRenderBatchAddMeshZeroAllocation() {
        RenderBatch batch = new RenderBatch(null);

        Mesh mesh = new Mesh("TestMesh");
        List<Vertex> verts = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            verts.add(new Vertex(i, i, i, Color.White, 0, 0));
            indices.add(i);
        }
        mesh.add(verts, indices);

        Matrix4f transform = new Matrix4f().identity().translate(1, 2, 3);

        // Perform warm up
        for (int frame = 0; frame < 50; frame++) {
            batch.addMesh(mesh, (List<Texture>) null, transform);
            batch.reset();
        }

        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Run 1000 simulated frames
        for (int frame = 0; frame < 1000; frame++) {
            batch.addMesh(mesh, (List<Texture>) null, transform);
            batch.reset();
        }

        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long diff = memAfter - memBefore;

        // Even with 1000 frames x 1000 vertices, heap growth should be negligible (well under 5MB, not hundreds of MB)
        assertTrue(diff < 5 * 1024 * 1024, "Expected minimal memory allocation during batch rendering, but observed diff: " + diff);
    }
}
