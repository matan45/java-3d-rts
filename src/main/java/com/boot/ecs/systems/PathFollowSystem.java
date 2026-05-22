package com.boot.ecs.systems;

import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.MovementState;
import com.boot.ecs.components.PathFollower;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import org.joml.Vector3f;

public final class PathFollowSystem {

    private static final float ARRIVE_DIST_SQ = 0.25f;

    private PathFollowSystem() {}

    public static void step(EcsWorld ecs, float dt) {
        ecs.dominion().findEntitiesWith(Transform.class, PathFollower.class, UnitKind.class)
                .stream().forEach(r -> advance(r.comp1(), r.comp2(), r.comp3(), dt));
    }

    private static void advance(Transform t, PathFollower pf, UnitKind kind, float dt) {
        if (pf.path.isEmpty()) return;
        if (pf.state == MovementState.HARVESTING || pf.state == MovementState.DEPOSITING) return;
        Vector3f wp = pf.path.peek();
        float dx = wp.x - t.pos.x;
        float dz = wp.z - t.pos.z;
        float d2 = dx * dx + dz * dz;
        if (d2 < ARRIVE_DIST_SQ) {
            pf.path.poll();
            return;
        }
        float dist = (float) Math.sqrt(d2);
        float nx = dx / dist;
        float nz = dz / dist;
        float step = Math.min(kind.type().speed * dt, dist);
        t.pos.x += nx * step;
        t.pos.z += nz * step;
        t.heading = (float) Math.atan2(nx, nz);
    }
}
