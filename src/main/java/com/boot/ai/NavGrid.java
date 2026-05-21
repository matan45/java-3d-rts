package com.boot.ai;

import com.boot.world.Heightmap;
import com.boot.world.PlacedBuilding;
import com.boot.world.SupplyPile;
import org.joml.Vector3f;

import java.util.List;

public final class NavGrid {

    private static final float PILE_HALF = 2f;

    private final Heightmap hm;
    private final int size;
    private final float quadSize;
    private final boolean[] terrainWalkable;
    private final boolean[] walkable;

    public NavGrid(Heightmap hm, float maxSlope, float waterLevel) {
        this.hm = hm;
        this.size = hm.size();
        this.quadSize = hm.quadSize();
        int n = size * size;
        this.terrainWalkable = new boolean[n];
        this.walkable = new boolean[n];

        for (int j = 0; j < size; j++) {
            for (int i = 0; i < size; i++) {
                float h = hm.heightAtGrid(i, j);
                if (h < waterLevel) {
                    terrainWalkable[j * size + i] = false;
                    continue;
                }
                float h1 = hm.heightAtGrid(i + 1, j);
                float h2 = hm.heightAtGrid(i - 1, j);
                float h3 = hm.heightAtGrid(i, j + 1);
                float h4 = hm.heightAtGrid(i, j - 1);
                float md = Math.max(
                        Math.max(Math.abs(h - h1), Math.abs(h - h2)),
                        Math.max(Math.abs(h - h3), Math.abs(h - h4)));
                terrainWalkable[j * size + i] = md < maxSlope;
            }
        }
        System.arraycopy(terrainWalkable, 0, walkable, 0, n);
    }

    public void rebuildObstacles(List<PlacedBuilding> buildings, List<SupplyPile> piles) {
        System.arraycopy(terrainWalkable, 0, walkable, 0, size * size);
        if (buildings != null) for (PlacedBuilding p : buildings) blockBuilding(p);
        if (piles != null) for (SupplyPile p : piles) blockPile(p);
    }

    public void blockBuilding(PlacedBuilding p) {
        blockAABB(p.cx(), p.cz(), p.halfSize());
    }

    public void blockPile(SupplyPile p) {
        blockAABB(p.cx(), p.cz(), PILE_HALF);
    }

    private void blockAABB(float cx, float cz, float half) {
        int iMin = Math.max(0, (int) Math.floor((cx - half) / quadSize));
        int iMax = Math.min(size - 1, (int) Math.ceil((cx + half) / quadSize));
        int jMin = Math.max(0, (int) Math.floor((cz - half) / quadSize));
        int jMax = Math.min(size - 1, (int) Math.ceil((cz + half) / quadSize));
        for (int j = jMin; j <= jMax; j++) {
            for (int i = iMin; i <= iMax; i++) {
                walkable[j * size + i] = false;
            }
        }
    }

    public boolean isWalkable(int i, int j) {
        if (i < 0 || j < 0 || i >= size || j >= size) return false;
        return walkable[j * size + i];
    }

    public int size() { return size; }
    public float quadSize() { return quadSize; }
    public Heightmap heightmap() { return hm; }

    public int toCellI(float worldX) {
        int i = Math.round(worldX / quadSize);
        if (i < 0) return 0;
        if (i >= size) return size - 1;
        return i;
    }

    public int toCellJ(float worldZ) {
        int j = Math.round(worldZ / quadSize);
        if (j < 0) return 0;
        if (j >= size) return size - 1;
        return j;
    }

    public Vector3f cellCenter(int i, int j, Vector3f out) {
        out.set(i * quadSize, hm.heightAtGrid(i, j), j * quadSize);
        return out;
    }
}
