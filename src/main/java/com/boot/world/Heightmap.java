package com.boot.world;

import org.joml.SimplexNoise;

import java.util.List;

public final class Heightmap {

    public record Plateau(float worldX, float worldZ,
                          float innerRadius, float outerRadius,
                          float targetHeight) {}

    private final int size;
    private final float quadSize;
    private final float verticalScale;
    private final float[] heights;
    private final List<Plateau> plateaus;
    private float minHeight = Float.POSITIVE_INFINITY;
    private float maxHeight = Float.NEGATIVE_INFINITY;

    public Heightmap(int size, float quadSize, float verticalScale, long seed) {
        this(size, quadSize, verticalScale, seed, List.of());
    }

    public Heightmap(int size, float quadSize, float verticalScale, long seed, List<Plateau> plateaus) {
        this.size = size;
        this.quadSize = quadSize;
        this.verticalScale = verticalScale;
        this.heights = new float[size * size];
        this.plateaus = List.copyOf(plateaus);

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
                float fbmHeight = n01 * verticalScale;

                float wx = i * quadSize;
                float wz = j * quadSize;
                float flatness = 0f;
                float weightSum = 0f;
                float baseSum = 0f;
                for (Plateau p : this.plateaus) {
                    float dx = wx - p.worldX;
                    float dz = wz - p.worldZ;
                    float d = (float) Math.sqrt(dx * dx + dz * dz);
                    float influence = 1f - smoothstep(p.innerRadius, p.outerRadius, d);
                    if (influence <= 0f) continue;
                    if (influence > flatness) flatness = influence;
                    weightSum += influence;
                    baseSum += influence * p.targetHeight;
                }
                float h;
                if (weightSum > 0f) {
                    float baseH = baseSum / weightSum;
                    h = fbmHeight + (baseH - fbmHeight) * flatness;
                } else {
                    h = fbmHeight;
                }

                heights[j * size + i] = h;
                if (h < minHeight) minHeight = h;
                if (h > maxHeight) maxHeight = h;
            }
        }
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        if (edge1 <= edge0) return x < edge0 ? 0f : 1f;
        float t = (x - edge0) / (edge1 - edge0);
        if (t < 0f) t = 0f; else if (t > 1f) t = 1f;
        return t * t * (3f - 2f * t);
    }

    public int size() { return size; }
    public float quadSize() { return quadSize; }
    public float worldSize() { return (size - 1) * quadSize; }
    public float verticalScale() { return verticalScale; }
    public float minHeight() { return minHeight; }
    public float maxHeight() { return maxHeight; }
    public List<Plateau> plateaus() { return plateaus; }

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
