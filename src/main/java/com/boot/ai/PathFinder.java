package com.boot.ai;

import org.joml.Vector3f;

import java.util.List;

public final class PathFinder {

    private static final int STRAIGHT = 10;
    private static final int DIAGONAL = 14;
    private static final int[] DX = { 1, -1,  0,  0,  1,  1, -1, -1 };
    private static final int[] DZ = { 0,  0,  1, -1,  1, -1,  1, -1 };

    private final NavGrid grid;
    private final int size;

    private final int[] gCost;
    private final int[] parent;
    private final int[] gen;
    private final boolean[] closed;
    private final int[] heapCell;
    private final int[] heapF;
    private int heapSize;
    private int generation;

    private final Vector3f scratch = new Vector3f();

    public PathFinder(NavGrid grid) {
        this.grid = grid;
        this.size = grid.size();
        int n = size * size;
        this.gCost = new int[n];
        this.parent = new int[n];
        this.gen = new int[n];
        this.closed = new boolean[n];
        this.heapCell = new int[n * 8];
        this.heapF = new int[n * 8];
    }

    public boolean findPath(int sx, int sz, int gx, int gz, List<Vector3f> out) {
        out.clear();
        if (!grid.isWalkable(sx, sz) || !grid.isWalkable(gx, gz)) return false;
        int startIdx = sz * size + sx;
        int goalIdx = gz * size + gx;
        if (startIdx == goalIdx) {
            out.add(grid.cellCenter(gx, gz, new Vector3f()));
            return true;
        }

        generation++;
        heapSize = 0;

        gen[startIdx] = generation;
        gCost[startIdx] = 0;
        parent[startIdx] = -1;
        closed[startIdx] = false;
        push(startIdx, heuristic(sx, sz, gx, gz));

        while (heapSize > 0) {
            int cur = pop();
            if (closed[cur] && gen[cur] == generation) continue;
            closed[cur] = true;

            if (cur == goalIdx) {
                reconstruct(cur, out);
                return true;
            }

            int ci = cur % size;
            int cj = cur / size;
            int curG = gCost[cur];

            for (int d = 0; d < 8; d++) {
                int ni = ci + DX[d];
                int nj = cj + DZ[d];
                if (ni < 0 || nj < 0 || ni >= size || nj >= size) continue;
                if (!grid.isWalkable(ni, nj)) continue;
                if (DX[d] != 0 && DZ[d] != 0) {
                    if (!grid.isWalkable(ci + DX[d], cj) || !grid.isWalkable(ci, cj + DZ[d])) continue;
                }
                int nIdx = nj * size + ni;
                if (gen[nIdx] == generation && closed[nIdx]) continue;

                int stepCost = (DX[d] != 0 && DZ[d] != 0) ? DIAGONAL : STRAIGHT;
                int tentativeG = curG + stepCost;

                if (gen[nIdx] != generation || tentativeG < gCost[nIdx]) {
                    gen[nIdx] = generation;
                    gCost[nIdx] = tentativeG;
                    parent[nIdx] = cur;
                    closed[nIdx] = false;
                    push(nIdx, tentativeG + heuristic(ni, nj, gx, gz));
                }
            }
        }
        return false;
    }

    public boolean findPathToNearest(int sx, int sz, int gx, int gz, int maxRadius, List<Vector3f> out) {
        int start = nearestWalkable(sx, sz, maxRadius);
        if (start < 0) { out.clear(); return false; }
        int goal = nearestWalkable(gx, gz, maxRadius);
        if (goal < 0) { out.clear(); return false; }
        return findPath(start % size, start / size, goal % size, goal / size, out);
    }

    public int nearestWalkable(int ci, int cj, int maxRadius) {
        if (ci >= 0 && cj >= 0 && ci < size && cj < size && grid.isWalkable(ci, cj)) {
            return cj * size + ci;
        }
        for (int r = 1; r <= maxRadius; r++) {
            for (int dj = -r; dj <= r; dj++) {
                for (int di = -r; di <= r; di++) {
                    if (Math.abs(di) != r && Math.abs(dj) != r) continue;
                    int ni = ci + di;
                    int nj = cj + dj;
                    if (ni < 0 || nj < 0 || ni >= size || nj >= size) continue;
                    if (grid.isWalkable(ni, nj)) return nj * size + ni;
                }
            }
        }
        return -1;
    }

    private void reconstruct(int goalIdx, List<Vector3f> out) {
        int len = 0;
        int cur = goalIdx;
        while (cur != -1) { len++; cur = parent[cur]; }

        Vector3f[] tmp = new Vector3f[len];
        cur = goalIdx;
        int k = len - 1;
        while (cur != -1) {
            int ci = cur % size;
            int cj = cur / size;
            tmp[k--] = grid.cellCenter(ci, cj, new Vector3f());
            cur = parent[cur];
        }
        for (Vector3f v : tmp) out.add(v);
    }

    private int heuristic(int ax, int az, int bx, int bz) {
        int dx = Math.abs(ax - bx);
        int dz = Math.abs(az - bz);
        int min = Math.min(dx, dz);
        int max = Math.max(dx, dz);
        return DIAGONAL * min + STRAIGHT * (max - min);
    }

    private void push(int cellIdx, int f) {
        if (heapSize >= heapCell.length) return;
        int i = heapSize++;
        heapCell[i] = cellIdx;
        heapF[i] = f;
        siftUp(i);
    }

    private int pop() {
        int top = heapCell[0];
        heapSize--;
        if (heapSize > 0) {
            heapCell[0] = heapCell[heapSize];
            heapF[0] = heapF[heapSize];
            siftDown(0);
        }
        return top;
    }

    private void siftUp(int i) {
        int cell = heapCell[i];
        int f = heapF[i];
        while (i > 0) {
            int parentI = (i - 1) >>> 1;
            if (heapF[parentI] <= f) break;
            heapCell[i] = heapCell[parentI];
            heapF[i] = heapF[parentI];
            i = parentI;
        }
        heapCell[i] = cell;
        heapF[i] = f;
    }

    private void siftDown(int i) {
        int cell = heapCell[i];
        int f = heapF[i];
        int half = heapSize >>> 1;
        while (i < half) {
            int left = (i << 1) + 1;
            int right = left + 1;
            int best = left;
            if (right < heapSize && heapF[right] < heapF[left]) best = right;
            if (heapF[best] >= f) break;
            heapCell[i] = heapCell[best];
            heapF[i] = heapF[best];
            i = best;
        }
        heapCell[i] = cell;
        heapF[i] = f;
    }
}
