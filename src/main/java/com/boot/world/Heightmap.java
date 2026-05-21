package com.boot.world;

import org.joml.SimplexNoise;

public final class Heightmap {

    private final int size;
    private final float quadSize;
    private final float verticalScale;
    private final float[] heights;
    private float minHeight = Float.POSITIVE_INFINITY;
    private float maxHeight = Float.NEGATIVE_INFINITY;

    public Heightmap(int size, float quadSize, float verticalScale, long seed) {
        this.size = size;
        this.quadSize = quadSize;
        this.verticalScale = verticalScale;
        this.heights = new float[size * size];

        float ox = ((seed * 1664525L + 1013904223L) & 0xFFFFFF) * 0.001f;
        float oz = ((seed * 22695477L + 1L) & 0xFFFFFF) * 0.001f;
        float baseFreq = 0.012f;
        int octaves = 5;
        float lacunarity = 2.0f;
        float gain = 0.5f;

        float maxAmp = 0f;
        float a = 1f;
        for (int o = 0; o < octaves; o++) { maxAmp += a; a *= gain; }

        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                float amp = 1f;
                float freq = baseFreq;
                float sum = 0f;
                for (int o = 0; o < octaves; o++) {
                    sum += SimplexNoise.noise((i + ox) * freq, (j + oz) * freq) * amp;
                    amp *= gain;
                    freq *= lacunarity;
                }
                float n01 = (sum / maxAmp) * 0.5f + 0.5f;
                n01 = (float) Math.pow(n01, 1.35);
                float h = n01 * verticalScale;
                heights[j * size + i] = h;
                if (h < minHeight) minHeight = h;
                if (h > maxHeight) maxHeight = h;
            }
        }
    }

    public int size() { return size; }
    public float quadSize() { return quadSize; }
    public float worldSize() { return (size - 1) * quadSize; }
    public float verticalScale() { return verticalScale; }
    public float minHeight() { return minHeight; }
    public float maxHeight() { return maxHeight; }

    public float heightAtGrid(int i, int j) {
        if (i < 0) i = 0; else if (i >= size) i = size - 1;
        if (j < 0) j = 0; else if (j >= size) j = size - 1;
        return heights[j * size + i];
    }

    public float heightAt(float worldX, float worldZ) {
        float fx = worldX / quadSize;
        float fz = worldZ / quadSize;
        int i0 = (int) Math.floor(fx);
        int j0 = (int) Math.floor(fz);
        float tx = fx - i0;
        float tz = fz - j0;
        float h00 = heightAtGrid(i0, j0);
        float h10 = heightAtGrid(i0 + 1, j0);
        float h01 = heightAtGrid(i0, j0 + 1);
        float h11 = heightAtGrid(i0 + 1, j0 + 1);
        float h0 = h00 + (h10 - h00) * tx;
        float h1 = h01 + (h11 - h01) * tx;
        return h0 + (h1 - h0) * tz;
    }
}
