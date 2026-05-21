package com.boot.render;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public final class BuildingMesh {

    private static final float[] CUBE = {
        // +Z face
        -0.5f, 0.0f,  0.5f,   0.5f, 0.0f,  0.5f,   0.5f, 1.0f,  0.5f,
        -0.5f, 0.0f,  0.5f,   0.5f, 1.0f,  0.5f,  -0.5f, 1.0f,  0.5f,
        // -Z face
         0.5f, 0.0f, -0.5f,  -0.5f, 0.0f, -0.5f,  -0.5f, 1.0f, -0.5f,
         0.5f, 0.0f, -0.5f,  -0.5f, 1.0f, -0.5f,   0.5f, 1.0f, -0.5f,
        // +X face
         0.5f, 0.0f,  0.5f,   0.5f, 0.0f, -0.5f,   0.5f, 1.0f, -0.5f,
         0.5f, 0.0f,  0.5f,   0.5f, 1.0f, -0.5f,   0.5f, 1.0f,  0.5f,
        // -X face
        -0.5f, 0.0f, -0.5f,  -0.5f, 0.0f,  0.5f,  -0.5f, 1.0f,  0.5f,
        -0.5f, 0.0f, -0.5f,  -0.5f, 1.0f,  0.5f,  -0.5f, 1.0f, -0.5f,
        // +Y face
        -0.5f, 1.0f,  0.5f,   0.5f, 1.0f,  0.5f,   0.5f, 1.0f, -0.5f,
        -0.5f, 1.0f,  0.5f,   0.5f, 1.0f, -0.5f,  -0.5f, 1.0f, -0.5f,
        // -Y face
        -0.5f, 0.0f, -0.5f,   0.5f, 0.0f, -0.5f,   0.5f, 0.0f,  0.5f,
        -0.5f, 0.0f, -0.5f,   0.5f, 0.0f,  0.5f,  -0.5f, 0.0f,  0.5f,
    };

    private final int vao;
    private final int vbo;

    public BuildingMesh() {
        FloatBuffer buf = MemoryUtil.memAllocFloat(CUBE.length);
        try {
            buf.put(CUBE).flip();

            vao = glGenVertexArrays();
            glBindVertexArray(vao);

            vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW);

            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);

            glBindVertexArray(0);
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glBindVertexArray(0);
    }

    public void dispose() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}
