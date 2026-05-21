package com.boot.units;

import com.boot.ai.NavGrid;
import com.boot.ai.PathFinder;
import com.boot.ui.HudState;
import com.boot.world.Heightmap;
import com.boot.world.PlacedBuilding;
import com.boot.world.SupplyPile;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class UnitManager {

    private static final float ARRIVE_DIST_SQ = 0.25f;

    private final NavGrid grid;
    private final PathFinder pathFinder;
    private final Heightmap heightmap;
    private final List<Unit> units = new ArrayList<>();
    private final List<Unit> selected = new ArrayList<>();
    private final List<Vector3f> pathScratch = new ArrayList<>();
    private boolean navObstaclesDirty;

    public UnitManager(NavGrid grid, PathFinder pathFinder, Heightmap heightmap) {
        this.grid = grid;
        this.pathFinder = pathFinder;
        this.heightmap = heightmap;
    }

    public List<Unit> units() { return units; }
    public List<Unit> selected() { return selected; }

    public boolean navObstaclesChanged() { return navObstaclesDirty; }
    public void clearNavObstaclesChanged() { navObstaclesDirty = false; }

    public Unit spawn(UnitType type, float x, float z) {
        int ci = grid.toCellI(x);
        int cj = grid.toCellJ(z);
        int spawnIdx = pathFinder.nearestWalkable(ci, cj, 16);
        if (spawnIdx < 0) return null;
        int sx = spawnIdx % grid.size();
        int sz = spawnIdx / grid.size();
        Vector3f c = new Vector3f();
        grid.cellCenter(sx, sz, c);
        Unit u = new Unit(type, c.x, c.y, c.z);
        units.add(u);
        return u;
    }

    public Unit pickUnit(Vector3f rayOrigin, Vector3f rayDir, float maxDist) {
        Unit best = null;
        float bestT = maxDist;
        for (Unit u : units) {
            float r = u.type.radius;
            float t = intersectAABB(
                    rayOrigin.x, rayOrigin.y, rayOrigin.z,
                    rayDir.x, rayDir.y, rayDir.z,
                    u.pos.x - r, u.pos.y, u.pos.z - r,
                    u.pos.x + r, u.pos.y + u.type.height, u.pos.z + r);
            if (t >= 0 && t < bestT) {
                bestT = t;
                best = u;
            }
        }
        return best;
    }

    public void selectSingle(Unit u) {
        for (Unit other : selected) other.selected = false;
        selected.clear();
        if (u != null) {
            u.selected = true;
            selected.add(u);
        }
    }

    public void addToSelection(Unit u) {
        if (u == null || u.selected) return;
        u.selected = true;
        selected.add(u);
    }

    public void deselectAll() {
        for (Unit u : selected) u.selected = false;
        selected.clear();
    }

    public boolean anyWorkerSelected() {
        for (Unit u : selected) if (u.type.isWorker()) return true;
        return false;
    }

    public void issueMove(Vector3f target, boolean queue) {
        if (selected.isEmpty()) return;
        int gx = grid.toCellI(target.x);
        int gz = grid.toCellJ(target.z);
        for (Unit u : selected) {
            int sx, sz;
            if (queue && !u.path.isEmpty()) {
                Vector3f last = u.path.peekLast();
                sx = grid.toCellI(last.x);
                sz = grid.toCellJ(last.z);
            } else {
                sx = grid.toCellI(u.pos.x);
                sz = grid.toCellJ(u.pos.z);
            }
            pathScratch.clear();
            if (!pathFinder.findPathToNearest(sx, sz, gx, gz, 24, pathScratch)) continue;
            if (!queue) {
                u.path.clear();
                u.targetPile = null;
                u.dropoff = null;
            }
            for (int i = 1; i < pathScratch.size(); i++) {
                u.path.add(new Vector3f(pathScratch.get(i)));
            }
            if (!u.path.isEmpty()) u.state = Unit.State.MOVING;
        }
    }

    public void issueHarvest(SupplyPile pile, List<PlacedBuilding> buildings) {
        if (pile == null || pile.depleted()) return;
        for (Unit u : selected) {
            if (!u.type.isWorker()) continue;
            u.targetPile = pile;
            u.dropoff = null;
            if (pathfindTo(u, pile.cx(), pile.cz())) {
                u.state = Unit.State.MOVING_TO_PILE;
            } else {
                u.state = Unit.State.IDLE;
                u.targetPile = null;
            }
        }
    }

    public void tick(float dt, List<SupplyPile> piles, List<PlacedBuilding> buildings, HudState state) {
        for (Unit u : units) {
            advanceMovement(u, dt);
            if (u.path.isEmpty()) handlePathEnd(u, piles, buildings);
            if (u.state == Unit.State.HARVESTING) {
                runHarvesting(u, dt, piles, buildings, state);
            }
            if (u.state == Unit.State.DEPOSITING) {
                runDepositing(u, piles, buildings, state);
            }
            u.pos.y = heightmap.heightAt(u.pos.x, u.pos.z);
        }
    }

    private void advanceMovement(Unit u, float dt) {
        if (u.path.isEmpty()) return;
        if (u.state == Unit.State.HARVESTING || u.state == Unit.State.DEPOSITING) return;
        Vector3f wp = u.path.peek();
        float dx = wp.x - u.pos.x;
        float dz = wp.z - u.pos.z;
        float d2 = dx * dx + dz * dz;
        if (d2 < ARRIVE_DIST_SQ) {
            u.path.poll();
            return;
        }
        float dist = (float) Math.sqrt(d2);
        float nx = dx / dist;
        float nz = dz / dist;
        float step = Math.min(u.type.speed * dt, dist);
        u.pos.x += nx * step;
        u.pos.z += nz * step;
        u.heading = (float) Math.atan2(nx, nz);
    }

    private void handlePathEnd(Unit u, List<SupplyPile> piles, List<PlacedBuilding> buildings) {
        switch (u.state) {
            case MOVING -> u.state = Unit.State.IDLE;
            case MOVING_TO_PILE -> {
                if (u.targetPile != null && !u.targetPile.depleted()) {
                    u.state = Unit.State.HARVESTING;
                    u.harvestTimer = 0f;
                } else {
                    retarget(u, piles, buildings);
                }
            }
            case MOVING_TO_BASE -> u.state = Unit.State.DEPOSITING;
            default -> {}
        }
    }

    private void runHarvesting(Unit u, float dt, List<SupplyPile> piles,
                               List<PlacedBuilding> buildings, HudState state) {
        u.harvestTimer += dt;
        if (u.harvestTimer < u.type.harvestPeriod) return;
        u.harvestTimer = 0f;

        if (u.targetPile != null && !u.targetPile.depleted()) {
            int want = u.type.carryCap - u.carriedCash;
            int got = u.targetPile.drain(want);
            u.carriedCash += got;
            state.mapCashAvailable -= got;
            if (u.targetPile.depleted()) {
                piles.remove(u.targetPile);
                grid.rebuildObstacles(buildings, piles);
                navObstaclesDirty = true;
            }
        }

        boolean full = u.carriedCash >= u.type.carryCap;
        boolean pileGone = u.targetPile == null || u.targetPile.depleted();
        if (full || pileGone) {
            u.dropoff = findDropoff(u, buildings);
            if (u.dropoff != null && pathfindTo(u, u.dropoff.cx(), u.dropoff.cz())) {
                u.state = Unit.State.MOVING_TO_BASE;
            } else {
                u.state = Unit.State.IDLE;
            }
        }
    }

    private void runDepositing(Unit u, List<SupplyPile> piles,
                               List<PlacedBuilding> buildings, HudState state) {
        state.cash += u.carriedCash;
        u.carriedCash = 0;
        u.dropoff = null;

        if (u.targetPile != null && !u.targetPile.depleted()) {
            if (pathfindTo(u, u.targetPile.cx(), u.targetPile.cz())) {
                u.state = Unit.State.MOVING_TO_PILE;
                return;
            }
        }
        SupplyPile next = findNearestPile(u, piles);
        if (next != null && pathfindTo(u, next.cx(), next.cz())) {
            u.targetPile = next;
            u.state = Unit.State.MOVING_TO_PILE;
        } else {
            u.targetPile = null;
            u.state = Unit.State.IDLE;
        }
    }

    private void retarget(Unit u, List<SupplyPile> piles, List<PlacedBuilding> buildings) {
        if (u.carriedCash > 0) {
            u.dropoff = findDropoff(u, buildings);
            if (u.dropoff != null && pathfindTo(u, u.dropoff.cx(), u.dropoff.cz())) {
                u.state = Unit.State.MOVING_TO_BASE;
                return;
            }
        }
        SupplyPile next = findNearestPile(u, piles);
        if (next != null && pathfindTo(u, next.cx(), next.cz())) {
            u.targetPile = next;
            u.state = Unit.State.MOVING_TO_PILE;
        } else {
            u.targetPile = null;
            u.state = Unit.State.IDLE;
        }
    }

    private boolean pathfindTo(Unit u, float gx, float gz) {
        int sx = grid.toCellI(u.pos.x);
        int sz = grid.toCellJ(u.pos.z);
        int gxC = grid.toCellI(gx);
        int gzC = grid.toCellJ(gz);
        pathScratch.clear();
        if (!pathFinder.findPathToNearest(sx, sz, gxC, gzC, 24, pathScratch)) return false;
        u.path.clear();
        for (int i = 1; i < pathScratch.size(); i++) {
            u.path.add(new Vector3f(pathScratch.get(i)));
        }
        return !u.path.isEmpty();
    }

    private PlacedBuilding findDropoff(Unit u, List<PlacedBuilding> buildings) {
        PlacedBuilding best = null;
        float bestD2 = Float.MAX_VALUE;
        for (PlacedBuilding b : buildings) {
            if (!"Supply Center".equals(b.name()) && !"Command Center".equals(b.name())) continue;
            float dx = b.cx() - u.pos.x;
            float dz = b.cz() - u.pos.z;
            float d2 = dx * dx + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = b; }
        }
        return best;
    }

    private SupplyPile findNearestPile(Unit u, List<SupplyPile> piles) {
        SupplyPile best = null;
        float bestD2 = Float.MAX_VALUE;
        for (SupplyPile p : piles) {
            if (p.depleted()) continue;
            float dx = p.cx() - u.pos.x;
            float dz = p.cz() - u.pos.z;
            float d2 = dx * dx + dz * dz;
            if (d2 < bestD2) { bestD2 = d2; best = p; }
        }
        return best;
    }

    private static float intersectAABB(float ox, float oy, float oz,
                                       float dx, float dy, float dz,
                                       float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ) {
        float invDx = 1f / dx, invDy = 1f / dy, invDz = 1f / dz;
        float tx1 = (minX - ox) * invDx, tx2 = (maxX - ox) * invDx;
        float ty1 = (minY - oy) * invDy, ty2 = (maxY - oy) * invDy;
        float tz1 = (minZ - oz) * invDz, tz2 = (maxZ - oz) * invDz;
        float tmin = Math.max(Math.max(Math.min(tx1, tx2), Math.min(ty1, ty2)), Math.min(tz1, tz2));
        float tmax = Math.min(Math.min(Math.max(tx1, tx2), Math.max(ty1, ty2)), Math.max(tz1, tz2));
        if (tmax < 0 || tmax < tmin) return -1f;
        return tmin > 0 ? tmin : tmax;
    }
}
