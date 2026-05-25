package com.boot.world;

public final class VisionGrid {

    public static final byte UNEXPLORED = 0;
    public static final byte EXPLORED = 1;
    public static final byte VISIBLE = 2;

    private final int gridW;
    private final int gridH;
    private final float cellSize;
    private final float worldExtent;
    private final byte[] cells;

    public VisionGrid(float worldExtent, float cellSize) {
        this.worldExtent = worldExtent;
        this.cellSize = cellSize;
        this.gridW = Math.max(1, (int) Math.ceil(worldExtent / cellSize));
        this.gridH = this.gridW;
        this.cells = new byte[gridW * gridH];
    }

    public int width() { return gridW; }
    public int height() { return gridH; }
    public float cellSize() { return cellSize; }
    public float worldExtent() { return worldExtent; }
    public byte[] data() { return cells; }

    public void demoteVisibleToExplored() {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == VISIBLE) cells[i] = EXPLORED;
        }
    }

    public void stampVisibleCircle(float worldX, float worldZ, float radius) {
        stamp(worldX, worldZ, radius, VISIBLE);
    }

    public void revealPermanent(float worldX, float worldZ, float radius) {
        float r2 = radius * radius;
        int ci = clampX((int) Math.floor(worldX / cellSize));
        int cj = clampZ((int) Math.floor(worldZ / cellSize));
        int rCells = (int) Math.ceil(radius / cellSize) + 1;
        int i0 = clampX(ci - rCells), i1 = clampX(ci + rCells);
        int j0 = clampZ(cj - rCells), j1 = clampZ(cj + rCells);
        for (int j = j0; j <= j1; j++) {
            float cz = (j + 0.5f) * cellSize;
            float dz = cz - worldZ;
            for (int i = i0; i <= i1; i++) {
                float cx = (i + 0.5f) * cellSize;
                float dx = cx - worldX;
                if (dx * dx + dz * dz <= r2) {
                    int idx = j * gridW + i;
                    if (cells[idx] == UNEXPLORED) cells[idx] = EXPLORED;
                }
            }
        }
    }

    public byte stateAtWorld(float worldX, float worldZ) {
        int i = clampX((int) Math.floor(worldX / cellSize));
        int j = clampZ((int) Math.floor(worldZ / cellSize));
        return cells[j * gridW + i];
    }

    private void stamp(float worldX, float worldZ, float radius, byte state) {
        float r2 = radius * radius;
        int ci = clampX((int) Math.floor(worldX / cellSize));
        int cj = clampZ((int) Math.floor(worldZ / cellSize));
        int rCells = (int) Math.ceil(radius / cellSize) + 1;
        int i0 = clampX(ci - rCells), i1 = clampX(ci + rCells);
        int j0 = clampZ(cj - rCells), j1 = clampZ(cj + rCells);
        for (int j = j0; j <= j1; j++) {
            float cz = (j + 0.5f) * cellSize;
            float dz = cz - worldZ;
            for (int i = i0; i <= i1; i++) {
                float cx = (i + 0.5f) * cellSize;
                float dx = cx - worldX;
                if (dx * dx + dz * dz <= r2) {
                    int idx = j * gridW + i;
                    if (cells[idx] < state) cells[idx] = state;
                }
            }
        }
    }

    private int clampX(int i) { return i < 0 ? 0 : (i >= gridW ? gridW - 1 : i); }
    private int clampZ(int j) { return j < 0 ? 0 : (j >= gridH ? gridH - 1 : j); }
}
