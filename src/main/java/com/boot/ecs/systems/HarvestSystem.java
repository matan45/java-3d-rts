package com.boot.ecs.systems;

import com.boot.ai.NavGrid;
import com.boot.ai.PathFinder;
import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.Harvester;
import com.boot.ecs.components.MovementState;
import com.boot.ecs.components.PathFollower;
import com.boot.ecs.components.SupplyCash;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import com.boot.ui.HudState;
import dev.dominion.ecs.api.Entity;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class HarvestSystem {

    private final List<Vector3f> scratch = new ArrayList<>();
    private boolean navDirty;

    public boolean consumeNavDirty() {
        boolean was = navDirty;
        navDirty = false;
        return was;
    }

    public void step(EcsWorld ecs, float dt, HudState state, NavGrid grid, PathFinder pathFinder) {
        ecs.dominion().findEntitiesWith(Transform.class, PathFollower.class, Harvester.class, UnitKind.class)
                .stream().forEach(r -> {
                    Transform t = r.comp1();
                    PathFollower pf = r.comp2();
                    Harvester h = r.comp3();
                    UnitKind k = r.comp4();
                    if (pf.path.isEmpty()) handlePathEnd(ecs, t, pf, h, grid, pathFinder);
                    if (pf.state == MovementState.HARVESTING) runHarvesting(ecs, t, pf, h, k, dt, state, grid, pathFinder);
                });
    }

    private void handlePathEnd(EcsWorld ecs, Transform t, PathFollower pf, Harvester h,
                               NavGrid grid, PathFinder pathFinder) {
        switch (pf.state) {
            case MOVING -> pf.state = MovementState.IDLE;
            case MOVING_TO_PILE -> {
                if (UnitAi.pileAlive(h.targetPile)) {
                    pf.state = MovementState.HARVESTING;
                    h.harvestTimer = 0f;
                } else {
                    UnitAi.retarget(ecs, t, pf, h, grid, pathFinder, scratch);
                }
            }
            case MOVING_TO_BASE -> pf.state = MovementState.DEPOSITING;
            default -> {}
        }
    }

    private void runHarvesting(EcsWorld ecs, Transform t, PathFollower pf, Harvester h, UnitKind k,
                               float dt, HudState state, NavGrid grid, PathFinder pathFinder) {
        if (!k.type().isWorker()) return;
        h.harvestTimer += dt;
        if (h.harvestTimer < k.type().harvestPeriod) return;
        h.harvestTimer = 0f;

        if (UnitAi.pileAlive(h.targetPile)) {
            SupplyCash sc = h.targetPile.get(SupplyCash.class);
            int want = k.type().carryCap - h.carriedCash;
            int got = Math.min(want, sc.cash);
            sc.cash -= got;
            h.carriedCash += got;
            state.mapCashAvailable -= got;
            if (sc.cash <= 0) {
                ecs.requestDestroy(h.targetPile);
                navDirty = true;
            }
        }

        boolean full = h.carriedCash >= k.type().carryCap;
        boolean pileGone = !UnitAi.pileAlive(h.targetPile);
        if (full || pileGone) {
            h.dropoff = UnitAi.findDropoff(ecs, t);
            if (UnitAi.entityAlive(h.dropoff)) {
                Entity drop = h.dropoff;
                Transform dt2 = drop.get(Transform.class);
                if (UnitAi.pathfindTo(t, pf, grid, pathFinder, dt2.pos.x, dt2.pos.z, scratch)) {
                    pf.state = MovementState.MOVING_TO_BASE;
                    return;
                }
            }
            pf.state = MovementState.IDLE;
        }
    }
}
