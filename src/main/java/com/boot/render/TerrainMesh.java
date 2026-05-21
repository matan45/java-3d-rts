package com.boot.render;

import com.boot.world.Heightmap;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public final class TerrainMesh {

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;

    public TerrainMesh(Heightmap hm) {
        int n = hm.size();
        float quad = hm.quadSize();
        int vertCount = n * n;
        int triCount = (n - 1) * (n - 1) * 2;
        this.indexCount = triCount * 3;

        FloatBuffer verts = MemoryUtil.memAllocFloat(vertCount * 6);
        IntBuffer indices = MemoryUtil.memAllocInt(indexCount);

        float maxH = Math.max(1e-3f, hm.maxHeight());
        Random rng = new Random(0xBADCAFE);

        try {
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    float x = i * quad;
                    float z = j * quad;
                    float y = hm.heightAtGrid(i, j);

                    float h01 = y / maxH;
                    float[] c = bandColor(h01);
                    float jitter = (rng.nextFloat() - 0.5f) * 0.06f;
                    float r = clamp01(c[0] + jitter);
                    float g = clamp01(c[1] + jitter);
                    float b = clamp01(c[2] + jitter);

                    verts.put(x).put(y).put(z).put(r).put(g).put(b);
                }
            }
            verts.flip();

            for (int j = 0; j < n - 1; j++) {
                for (int i = 0; i < n - 1; i++) {
                    int a = j * n + i;
                    int b = j * n + (i + 1);
                    int c = (j + 1) * n + i;
                    int d = (j + 1) * n + (i + 1);
                    indices.put(a).put(c).put(b);
                    indices.put(b).put(c).put(d);
                }
            }
            indices.flip();

            vao = glGenVertexArrays();
            glBindVertexArray(vao);

            vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, verts, GL_STATIC_DRAW);

            int stride = 6 * Float.BYTES;
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);

            ebo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

            glBindVertexArray(0);
        } finally {
            MemoryUtil.memFree(verts);
            MemoryUtil.memFree(indices);
        }
    }

    private static float[] bandColor(float h) {
        if (h < 0.20f) return new float[]{0.10f, 0.25f, 0.45f};
        if (h < 0.28f) return new float[]{0.85f, 0.78f, 0.55f};
        if (h < 0.55f) return new float[]{0.30f, 0.55f, 0.25f};
        if (h < 0.80f) return new float[]{0.50f, 0.45f, 0.40f};
        return new float[]{0.95f, 0.95f, 0.98f};
    }

    private static float clamp01(float v) {
        return v < 0 ? 0 : v > 1 ? 1 : v;
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteBuffers(ebo);
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
