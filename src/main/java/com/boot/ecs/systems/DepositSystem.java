package com.boot.ecs.systems;

import com.boot.ai.NavGrid;
import com.boot.ai.PathFinder;
import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.Harvester;
import com.boot.ecs.components.MovementState;
import com.boot.ecs.components.PathFollower;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import com.boot.ui.HudState;
import dev.dominion.ecs.api.Entity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class DepositSystem {

    private final List<Vector3f> scratch = new ArrayList<>();

    public void step(EcsWorld ecs, HudState state, NavGrid grid, PathFinder pathFinder) {
        ecs.dominion().findEntitiesWith(Transform.class, PathFollower.class, Harvester.class, UnitKind.class)
                .stream().forEach(r -> {
                    PathFollower pf = r.comp2();
                    if (pf.state != MovementState.DEPOSITING) return;
                    runDepositing(ecs, r.comp1(), pf, r.comp3(), state, grid, pathFinder);
                });
    }

    private void runDepositing(EcsWorld ecs, Transform t, PathFollower pf, Harvester h,
                               HudState state, NavGrid grid, PathFinder pathFinder) {
        state.cash += h.carriedCash;
        h.carriedCash = 0;
        h.dropoff = null;

        if (UnitAi.pileAlive(h.targetPile)) {
            Transform pt = h.targetPile.get(Transform.class);
            if (UnitAi.pathfindTo(t, pf, grid, pathFinder, pt.pos.x, pt.pos.z, scratch)) {
                pf.state = MovementState.MOVING_TO_PILE;
                return;
            }
        }
        Entity next = UnitAi.findNearestPile(ecs, t);
        if (next != null) {
            Transform nt = next.get(Transform.class);
            if (UnitAi.pathfindTo(t, pf, grid, pathFinder, nt.pos.x, nt.pos.z, scratch)) {
                h.targetPile = next;
                pf.state = MovementState.MOVING_TO_PILE;
                return;
            }
        }
        h.targetPile = null;
        pf.state = MovementState.IDLE;
    }
}
