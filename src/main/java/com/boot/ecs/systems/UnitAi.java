package com.boot.ecs.systems;

import com.boot.ai.NavGrid;
import com.boot.ai.PathFinder;
import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.BuildingType;
import com.boot.ecs.components.Harvester;
import com.boot.ecs.components.MovementState;
import com.boot.ecs.components.PathFollower;
import com.boot.ecs.components.SupplyCash;
import com.boot.ecs.components.Transform;
import dev.dominion.ecs.api.Entity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

final class UnitAi {

    private UnitAi() {}

    static boolean pathfindTo(Transform t, PathFollower pf, NavGrid grid, PathFinder pathFinder,
                              float gx, float gz, List<Vector3f> scratch) {
        int sx = grid.toCellI(t.pos.x);
        int sz = grid.toCellJ(t.pos.z);
        int gxC = grid.toCellI(gx);
        int gzC = grid.toCellJ(gz);
        scratch.clear();
        if (!pathFinder.findPathToNearest(sx, sz, gxC, gzC, 24, scratch)) return false;
        pf.path.clear();
        for (int i = 1; i < scratch.size(); i++) {
            pf.path.add(new Vector3f(scratch.get(i)));
        }
        return !pf.path.isEmpty();
    }

    static Entity findDropoff(EcsWorld ecs, Transform t) {
        Entity[] best = { null };
        float[] bestD2 = { Float.MAX_VALUE };
        ecs.dominion().findEntitiesWith(Transform.class, BuildingType.class)
                .stream().forEach(r -> {
                    String name = r.comp2().name();
                    if (!"Supply Center".equals(name) && !"Command Center".equals(name)) return;
                    float dx = r.comp1().pos.x - t.pos.x;
                    float dz = r.comp1().pos.z - t.pos.z;
                    float d2 = dx * dx + dz * dz;
                    if (d2 < bestD2[0]) {
                        bestD2[0] = d2;
                        best[0] = r.entity();
                    }
                });
        return best[0];
    }

    static Entity findNearestPile(EcsWorld ecs, Transform t) {
        Entity[] best = { null };
        float[] bestD2 = { Float.MAX_VALUE };
        ecs.dominion().findEntitiesWith(Transform.class, SupplyCash.class)
                .stream().forEach(r -> {
                    if (r.comp2().cash <= 0) return;
                    float dx = r.comp1().pos.x - t.pos.x;
                    float dz = r.comp1().pos.z - t.pos.z;
                    float d2 = dx * dx + dz * dz;
                    if (d2 < bestD2[0]) {
                        bestD2[0] = d2;
                        best[0] = r.entity();
                    }
                });
        return best[0];
    }

    static boolean pileAlive(Entity e) {
        if (e == null) return false;
        if (!e.isEnabled()) return false;
        SupplyCash sc = e.get(SupplyCash.class);
        return sc != null && sc.cash > 0;
    }

    static boolean entityAlive(Entity e) {
        return e != null && e.isEnabled();
    }

    static void retarget(EcsWorld ecs, Transform t, PathFollower pf, Harvester h,
                         NavGrid grid, PathFinder pathFinder, List<Vector3f> scratch) {
        if (h.carriedCash > 0) {
            h.dropoff = findDropoff(ecs, t);
            if (entityAlive(h.dropoff)) {
                Transform dt = h.dropoff.get(Transform.class);
                if (pathfindTo(t, pf, grid, pathFinder, dt.pos.x, dt.pos.z, scratch)) {
                    pf.state = MovementState.MOVING_TO_BASE;
                    return;
                }
            }
        }
        Entity next = findNearestPile(ecs, t);
        if (next != null) {
            Transform nt = next.get(Transform.class);
            if (pathfindTo(t, pf, grid, pathFinder, nt.pos.x, nt.pos.z, scratch)) {
                h.targetPile = next;
                pf.state = MovementState.MOVING_TO_PILE;
                return;
            }
        }
        h.targetPile = null;
        pf.state = MovementState.IDLE;
    }
}
