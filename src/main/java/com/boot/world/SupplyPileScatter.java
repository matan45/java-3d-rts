package com.boot.world;

import com.boot.ecs.EcsWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class SupplyPileScatter {

    private static final int TARGET_COUNT = 14;
    private static final int MAX_ATTEMPTS = 400;
    private static final float EDGE_MARGIN = 16f;
    private static final float MIN_SPACING = 14f;
    private static final float PILE_HALF = 2f;
    private static final float MAX_SLOPE_DELTA = 1.0f;
    private static final long SALT = 0x5A1EDEA1CAFEBABEL;

    private SupplyPileScatter() {}

    public static void scatter(Heightmap hm, long seed, EcsWorld ecs) {
        Random rng = new Random(seed ^ SALT);
        List<float[]> accepted = new ArrayList<>();

        float world = hm.worldSize();
        float lo = EDGE_MARGIN;
        float hi = world - EDGE_MARGIN;
        if (hi <= lo) return;

        int attempts = 0;
        while (accepted.size() < TARGET_COUNT && attempts < MAX_ATTEMPTS) {
            attempts++;
            float cx = lo + rng.nextFloat() * (hi - lo);
            float cz = lo + rng.nextFloat() * (hi - lo);

            float a = hm.heightAt(cx - PILE_HALF, cz - PILE_HALF);
            float b = hm.heightAt(cx + PILE_HALF, cz - PILE_HALF);
            float c = hm.heightAt(cx - PILE_HALF, cz + PILE_HALF);
            float d = hm.heightAt(cx + PILE_HALF, cz + PILE_HALF);
            float min = Math.min(Math.min(a, b), Math.min(c, d));
            float max = Math.max(Math.max(a, b), Math.max(c, d));
            if (max - min > MAX_SLOPE_DELTA) continue;

            boolean tooClose = false;
            for (float[] xz : accepted) {
                float dx = xz[0] - cx;
                float dz = xz[1] - cz;
                if (dx * dx + dz * dz < MIN_SPACING * MIN_SPACING) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) continue;

            int cash = 1500 + rng.nextInt(21) * 100;
            float cy = hm.heightAt(cx, cz);
            ecs.spawnPile(cx, cy, cz, cash);
            accepted.add(new float[] { cx, cz });
        }
    }
}
