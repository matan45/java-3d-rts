package com.boot.render;

import com.boot.world.VisionGrid;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_R8;

public final class FogTexture {

    private final int textureId;
    private final int width;
    private final int height;
    private final ByteBuffer scratch;

    public FogTexture(int width, int height) {
        this.width = width;
        this.height = height;
        this.scratch = MemoryUtil.memAlloc(width * height);

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, width, height, 0,
                GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void update(VisionGrid grid) {
        if (grid.width() != width || grid.height() != height) {
            throw new IllegalArgumentException("Grid size mismatch");
        }
        byte[] src = grid.data();
        scratch.clear();
        for (int i = 0; i < src.length; i++) {
            int v;
            switch (src[i]) {
                case VisionGrid.VISIBLE   -> v = 255;
                case VisionGrid.EXPLORED  -> v = 128;
                default                   -> v = 0;
            }
            scratch.put((byte) v);
        }
        scratch.flip();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height,
                GL_RED, GL_UNSIGNED_BYTE, scratch);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public int id() { return textureId; }

    public void dispose() {
        glDeleteTextures(textureId);
        MemoryUtil.memFree(scratch);
    }
}
