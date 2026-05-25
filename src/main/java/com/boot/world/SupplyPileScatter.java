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

    private static final int PILES_PER_PLATEAU = 2;
    private static final float PLATEAU_ANNULUS_THICKNESS = 12f;
    private static final int PER_PLATEAU_ATTEMPTS = 60;

    private SupplyPileScatter() {}

    public static void scatter(Heightmap hm, long seed, EcsWorld ecs) {
        Random rng = new Random(seed ^ SALT);
        List<float[]> accepted = new ArrayList<>();

        float world = hm.worldSize();
        float lo = EDGE_MARGIN;
        float hi = world - EDGE_MARGIN;
        if (hi <= lo) return;

        for (Heightmap.Plateau p : hm.plateaus()) {
            int placed = 0;
            int attempts = 0;
            while (placed < PILES_PER_PLATEAU && attempts < PER_PLATEAU_ATTEMPTS) {
                attempts++;
                float angle = rng.nextFloat() * (float) (Math.PI * 2.0);
                float r = p.outerRadius() + rng.nextFloat() * PLATEAU_ANNULUS_THICKNESS;
                float cx = p.worldX() + (float) Math.cos(angle) * r;
                float cz = p.worldZ() + (float) Math.sin(angle) * r;
                if (cx < lo || cx > hi || cz < lo || cz > hi) continue;
                if (tryPlace(hm, ecs, accepted, rng, cx, cz)) placed++;
            }
        }

        int attempts = 0;
        while (accepted.size() < TARGET_COUNT && attempts < MAX_ATTEMPTS) {
            attempts++;
            float cx = lo + rng.nextFloat() * (hi - lo);
            float cz = lo + rng.nextFloat() * (hi - lo);
            tryPlace(hm, ecs, accepted, rng, cx, cz);
        }
    }

    private static boolean tryPlace(Heightmap hm, EcsWorld ecs, List<float[]> accepted,
                                    Random rng, float cx, float cz) {
        float a = hm.heightAt(cx - PILE_HALF, cz - PILE_HALF);
        float b = hm.heightAt(cx + PILE_HALF, cz - PILE_HALF);
        float c = hm.heightAt(cx - PILE_HALF, cz + PILE_HALF);
        float d = hm.heightAt(cx + PILE_HALF, cz + PILE_HALF);
        float min = Math.min(Math.min(a, b), Math.min(c, d));
        float max = Math.max(Math.max(a, b), Math.max(c, d));
        if (max - min > MAX_SLOPE_DELTA) return false;

        for (float[] xz : accepted) {
            float dx = xz[0] - cx;
            float dz = xz[1] - cz;
            if (dx * dx + dz * dz < MIN_SPACING * MIN_SPACING) return false;
        }

        int cash = 1500 + rng.nextInt(21) * 100;
        float cy = hm.heightAt(cx, cz);
        ecs.spawnPile(cx, cy, cz, cash);
        accepted.add(new float[] { cx, cz });
        return true;
    }
}
