package com.boot.ecs.systems;

import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.BuildingType;
import com.boot.ecs.components.MovementState;
import com.boot.ecs.components.PathFollower;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import dev.dominion.ecs.api.Entity;

import java.util.ArrayList;
import java.util.List;

public final class CollisionSystem {

    private CollisionSystem() {}

    private record UnitRow(Transform t, UnitKind k, PathFollower pf) {}
    private record BuildingRow(Transform t, BuildingType bt) {}

    public static void step(EcsWorld ecs) {
        List<UnitRow> units = new ArrayList<>();
        ecs.dominion().findEntitiesWith(Transform.class, UnitKind.class, PathFollower.class)
                .stream().forEach(r -> units.add(new UnitRow(r.comp1(), r.comp2(), r.comp3())));

        List<BuildingRow> buildings = new ArrayList<>();
        ecs.dominion().findEntitiesWith(Transform.class, BuildingType.class)
                .stream().forEach(r -> buildings.add(new BuildingRow(r.comp1(), r.comp2())));

        resolveUnitUnit(units);
        resolveUnitBuilding(units, buildings);
    }

    private static void resolveUnitUnit(List<UnitRow> units) {
        int n = units.size();
        for (int i = 0; i < n; i++) {
            UnitRow a = units.get(i);
            for (int j = i + 1; j < n; j++) {
                UnitRow b = units.get(j);
                float dx = b.t.pos.x - a.t.pos.x;
                float dz = b.t.pos.z - a.t.pos.z;
                float minDist = a.k.type().radius + b.k.type().radius;
                float d2 = dx * dx + dz * dz;
                if (d2 >= minDist * minDist) continue;
                float d = (float) Math.sqrt(d2);
                float overlap;
                float nx, nz;
                if (d > 1e-4f) {
                    overlap = minDist - d;
                    nx = dx / d;
                    nz = dz / d;
                } else {
                    overlap = minDist;
                    nx = 1f;
                    nz = 0f;
                }
                boolean aStatic = isStatic(a.pf);
                boolean bStatic = isStatic(b.pf);
                float aFrac, bFrac;
                if (aStatic && !bStatic)      { aFrac = 0f;   bFrac = 1f;   }
                else if (!aStatic && bStatic) { aFrac = 1f;   bFrac = 0f;   }
                else                          { aFrac = 0.5f; bFrac = 0.5f; }
                a.t.pos.x -= nx * overlap * aFrac;
                a.t.pos.z -= nz * overlap * aFrac;
                b.t.pos.x += nx * overlap * bFrac;
                b.t.pos.z += nz * overlap * bFrac;
            }
        }
    }

    private static void resolveUnitBuilding(List<UnitRow> units, List<BuildingRow> buildings) {
        for (UnitRow u : units) {
            for (BuildingRow b : buildings) {
                float h = b.bt.halfSize() + u.k.type().radius;
                float dx = u.t.pos.x - b.t.pos.x;
                float dz = u.t.pos.z - b.t.pos.z;
                float absDx = Math.abs(dx);
                float absDz = Math.abs(dz);
                if (absDx >= h || absDz >= h) continue;
                float penX = h - absDx;
                float penZ = h - absDz;
                if (penX < penZ) {
                    u.t.pos.x += (dx >= 0 ? 1f : -1f) * penX;
                } else {
                    u.t.pos.z += (dz >= 0 ? 1f : -1f) * penZ;
                }
            }
        }
    }

    private static boolean isStatic(PathFollower pf) {
        return pf.state == MovementState.HARVESTING || pf.state == MovementState.DEPOSITING;
    }
}
