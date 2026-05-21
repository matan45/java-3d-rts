package com.boot.render;

import com.boot.ai.NavGrid;
import com.boot.world.Heightmap;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public final class NavDebugMesh {

    private int vao;
    private int vbo;
    private int ebo;
    private int indexCount;

    public NavDebugMesh(NavGrid grid) {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
        rebuild(grid);
    }

    public void rebuild(NavGrid grid) {
        Heightmap hm = grid.heightmap();
        int size = grid.size();
        float quad = grid.quadSize();
        float half = 0.4f * quad;
        float yOff = 0.18f;

        int count = 0;
        for (int j = 0; j < size; j++)
            for (int i = 0; i < size; i++)
                if (grid.isWalkable(i, j)) count++;

        if (count == 0) {
            indexCount = 0;
            return;
        }

        FloatBuffer verts = MemoryUtil.memAllocFloat(count * 4 * 3);
        IntBuffer indices = MemoryUtil.memAllocInt(count * 6);

        try {
            int vBase = 0;
            for (int j = 0; j < size; j++) {
                for (int i = 0; i < size; i++) {
                    if (!grid.isWalkable(i, j)) continue;
                    float x = i * quad;
                    float z = j * quad;
                    float y = hm.heightAtGrid(i, j) + yOff;
                    verts.put(x - half).put(y).put(z - half);
                    verts.put(x + half).put(y).put(z - half);
                    verts.put(x + half).put(y).put(z + half);
                    verts.put(x - half).put(y).put(z + half);
                    indices.put(vBase    ).put(vBase + 1).put(vBase + 2);
                    indices.put(vBase    ).put(vBase + 2).put(vBase + 3);
                    vBase += 4;
                }
            }
            verts.flip();
            indices.flip();
            indexCount = indices.remaining();

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, verts, GL_DYNAMIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_DYNAMIC_DRAW);
            glBindVertexArray(0);
        } finally {
            MemoryUtil.memFree(verts);
            MemoryUtil.memFree(indices);
        }
    }

    public void render() {
        if (indexCount == 0) return;
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void dispose() {
        if (ebo != 0) glDeleteBuffers(ebo);
        if (vbo != 0) glDeleteBuffers(vbo);
        if (vao != 0) glDeleteVertexArrays(vao);
        vao = vbo = ebo = 0;
        indexCount = 0;
    }
}
