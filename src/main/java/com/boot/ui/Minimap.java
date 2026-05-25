package com.boot.ui;

import com.boot.world.Heightmap;
import com.boot.world.VisionGrid;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public final class Minimap {

    private final int textureId;
    private final int texSize;
    private final float worldSize;
    private final byte[] baseRGBA;
    private final ByteBuffer uploadBuf;

    public Minimap(Heightmap hm) {
        this.texSize = Math.min(hm.size(), 256);
        this.worldSize = hm.worldSize();
        this.baseRGBA = new byte[texSize * texSize * 4];
        this.uploadBuf = MemoryUtil.memAlloc(texSize * texSize * 4);

        float step = (hm.size() - 1) / (float) (texSize - 1);
        float maxH = Math.max(1e-3f, hm.maxHeight());
        int p = 0;
        for (int j = 0; j < texSize; j++) {
            for (int i = 0; i < texSize; i++) {
                int gi = Math.min(hm.size() - 1, (int) (i * step));
                int gj = Math.min(hm.size() - 1, (int) (j * step));
                float y = hm.heightAtGrid(gi, gj);
                float h01 = y / maxH;
                float[] c = bandColor(h01);
                float shade = 0.85f + 0.30f * h01;
                baseRGBA[p++] = (byte) clamp255((int) (c[0] * shade * 255));
                baseRGBA[p++] = (byte) clamp255((int) (c[1] * shade * 255));
                baseRGBA[p++] = (byte) clamp255((int) (c[2] * shade * 255));
                baseRGBA[p++] = (byte) 255;
            }
        }

        uploadBuf.clear();
        uploadBuf.put(baseRGBA);
        uploadBuf.flip();

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, texSize, texSize, 0,
                GL_RGBA, GL_UNSIGNED_BYTE, uploadBuf);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void update(VisionGrid grid) {
        uploadBuf.clear();
        int gw = grid.width();
        int gh = grid.height();
        for (int j = 0; j < texSize; j++) {
            int gj = Math.min(gh - 1, (int) ((j / (float) texSize) * gh));
            for (int i = 0; i < texSize; i++) {
                int gi = Math.min(gw - 1, (int) ((i / (float) texSize) * gw));
                byte state = grid.data()[gj * gw + gi];
                float mul;
                switch (state) {
                    case VisionGrid.VISIBLE  -> mul = 1.0f;
                    case VisionGrid.EXPLORED -> mul = 0.45f;
                    default                  -> mul = 0.0f;
                }
                int p = (j * texSize + i) * 4;
                int r = (baseRGBA[p    ] & 0xFF);
                int g = (baseRGBA[p + 1] & 0xFF);
                int b = (baseRGBA[p + 2] & 0xFF);
                uploadBuf.put((byte) clamp255((int) (r * mul)));
                uploadBuf.put((byte) clamp255((int) (g * mul)));
                uploadBuf.put((byte) clamp255((int) (b * mul)));
                uploadBuf.put((byte) 255);
            }
        }
        uploadBuf.flip();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, texSize, texSize,
                GL_RGBA, GL_UNSIGNED_BYTE, uploadBuf);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private static float[] bandColor(float h) {
        if (h < 0.20f) return new float[]{0.10f, 0.25f, 0.45f};
        if (h < 0.28f) return new float[]{0.85f, 0.78f, 0.55f};
        if (h < 0.55f) return new float[]{0.30f, 0.55f, 0.25f};
        if (h < 0.80f) return new float[]{0.50f, 0.45f, 0.40f};
        return new float[]{0.95f, 0.95f, 0.98f};
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }

    public int textureId() { return textureId; }
    public float worldSize() { return worldSize; }

    public void dispose() {
        glDeleteTextures(textureId);
        MemoryUtil.memFree(uploadBuf);
    }
}
