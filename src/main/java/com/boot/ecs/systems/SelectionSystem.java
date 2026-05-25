package com.boot.ecs.systems;

import com.boot.ai.NavGrid;
import com.boot.ai.PathFinder;
import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.BuildingType;
import com.boot.ecs.components.Harvester;
import com.boot.ecs.components.MovementState;
import com.boot.ecs.components.PathFollower;
import com.boot.ecs.components.Selectable;
import com.boot.ecs.components.SupplyCash;
import com.boot.ecs.components.TeamOwner;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import dev.dominion.ecs.api.Entity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class SelectionSystem {

    private final List<Vector3f> pathScratch = new ArrayList<>();

    public List<Entity> selectedUnits(EcsWorld ecs) {
        List<Entity> out = new ArrayList<>();
        ecs.dominion().findEntitiesWith(Selectable.class, UnitKind.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    if (r.comp1().selected) out.add(r.entity());
                });
        return out;
    }

    public void deselectAll(EcsWorld ecs) {
        ecs.dominion().findEntitiesWith(Selectable.class, UnitKind.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    r.comp1().selected = false;
                });
    }

    public void selectSingle(EcsWorld ecs, Entity unit) {
        deselectAll(ecs);
        if (unit != null) unit.get(Selectable.class).selected = true;
    }

    public void addToSelection(Entity unit) {
        if (unit == null) return;
        unit.get(Selectable.class).selected = true;
    }

    public boolean anyWorkerSelected(EcsWorld ecs) {
        boolean[] found = { false };
        ecs.dominion().findEntitiesWith(Selectable.class, UnitKind.class)
                .stream().forEach(r -> {
                    if (found[0]) return;
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    if (r.comp1().selected && r.comp2().type().isWorker()) found[0] = true;
                });
        return found[0];
    }

    public boolean anySelected(EcsWorld ecs) {
        boolean[] found = { false };
        ecs.dominion().findEntitiesWith(Selectable.class, UnitKind.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    if (r.comp1().selected) found[0] = true;
                });
        return found[0];
    }

    public void issueMove(EcsWorld ecs, Vector3f target, boolean queue,
                          NavGrid grid, PathFinder pathFinder) {
        int gx = grid.toCellI(target.x);
        int gz = grid.toCellJ(target.z);
        ecs.dominion().findEntitiesWith(Selectable.class, Transform.class, PathFollower.class, Harvester.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    if (!r.comp1().selected) return;
                    Transform t = r.comp2();
                    PathFollower pf = r.comp3();
                    Harvester h = r.comp4();
                    int sx, sz;
                    if (queue && !pf.path.isEmpty()) {
                        Vector3f last = pf.path.peekLast();
                        sx = grid.toCellI(last.x);
                        sz = grid.toCellJ(last.z);
                    } else {
                        sx = grid.toCellI(t.pos.x);
                        sz = grid.toCellJ(t.pos.z);
                    }
                    pathScratch.clear();
                    if (!pathFinder.findPathToNearest(sx, sz, gx, gz, 24, pathScratch)) return;
                    if (!queue) {
                        pf.path.clear();
                        h.targetPile = null;
                        h.dropoff = null;
                    }
                    for (int i = 1; i < pathScratch.size(); i++) {
                        pf.path.add(new Vector3f(pathScratch.get(i)));
                    }
                    if (!pf.path.isEmpty()) pf.state = MovementState.MOVING;
                });
    }

    public void issueHarvest(EcsWorld ecs, Entity pile, NavGrid grid, PathFinder pathFinder) {
        if (pile == null || !pile.isEnabled()) return;
        SupplyCash sc = pile.get(SupplyCash.class);
        if (sc == null || sc.cash <= 0) return;
        Transform pileT = pile.get(Transform.class);
        ecs.dominion().findEntitiesWith(Selectable.class, Transform.class, PathFollower.class, Harvester.class, UnitKind.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    if (!r.comp1().selected) return;
                    if (!r.comp5().type().isWorker()) return;
                    Transform t = r.comp2();
                    PathFollower pf = r.comp3();
                    Harvester h = r.comp4();
                    h.targetPile = pile;
                    h.dropoff = null;
                    if (UnitAi.pathfindTo(t, pf, grid, pathFinder, pileT.pos.x, pileT.pos.z, pathScratch)) {
                        pf.state = MovementState.MOVING_TO_PILE;
                    } else {
                        pf.state = MovementState.IDLE;
                        h.targetPile = null;
                    }
                });
    }

    public Entity pickUnit(EcsWorld ecs, Vector3f rayOrigin, Vector3f rayDir, float maxDist) {
        Entity[] best = { null };
        float[] bestT = { maxDist };
        ecs.dominion().findEntitiesWith(Transform.class, UnitKind.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    Transform tt = r.comp1();
                    UnitKind k = r.comp2();
                    float rad = k.type().radius;
                    float t = intersectAABB(
                            rayOrigin.x, rayOrigin.y, rayOrigin.z,
                            rayDir.x, rayDir.y, rayDir.z,
                            tt.pos.x - rad, tt.pos.y, tt.pos.z - rad,
                            tt.pos.x + rad, tt.pos.y + k.type().height, tt.pos.z + rad);
                    if (t >= 0 && t < bestT[0]) {
                        bestT[0] = t;
                        best[0] = r.entity();
                    }
                });
        return best[0];
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
