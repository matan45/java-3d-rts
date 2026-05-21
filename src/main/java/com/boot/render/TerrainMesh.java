package com.boot.render;

import com.boot.world.Heightmap;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public final class TerrainMesh {

    private final int vao;
    private final int vbo;
    private final int ebo;
    private final int indexCount;
    private final float maxHeight;

    public TerrainMesh(Heightmap hm) {
        int n = hm.size();
        float quad = hm.quadSize();
        int vertCount = n * n;
        int triCount = (n - 1) * (n - 1) * 2;
        this.indexCount = triCount * 3;
        this.maxHeight = Math.max(1e-3f, hm.maxHeight());

        FloatBuffer verts = MemoryUtil.memAllocFloat(vertCount * 6);
        IntBuffer indices = MemoryUtil.memAllocInt(indexCount);

        try {
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    float x = i * quad;
                    float z = j * quad;
                    float y = hm.heightAtGrid(i, j);

                    float dhx = hm.heightAtGrid(i + 1, j) - hm.heightAtGrid(i - 1, j);
                    float dhz = hm.heightAtGrid(i, j + 1) - hm.heightAtGrid(i, j - 1);
                    float nx = -dhx;
                    float ny = 2f * quad;
                    float nz = -dhz;
                    float inv = 1f / (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                    nx *= inv; ny *= inv; nz *= inv;

                    verts.put(x).put(y).put(z).put(nx).put(ny).put(nz);
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

    public float maxHeight() { return maxHeight; }

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
