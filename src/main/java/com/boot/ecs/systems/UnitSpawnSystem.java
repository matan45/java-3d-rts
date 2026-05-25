package com.boot.ecs.systems;

import com.boot.ai.NavGrid;
import com.boot.ai.PathFinder;
import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.Team;
import com.boot.units.UnitType;
import dev.dominion.ecs.api.Entity;
import org.joml.Vector3f;

public final class UnitSpawnSystem {

    private static final Vector3f scratch = new Vector3f();

    private UnitSpawnSystem() {}

    public static Entity spawnNear(EcsWorld ecs, UnitType type, float x, float z,
                                   NavGrid grid, PathFinder pathFinder, Team team) {
        int ci = grid.toCellI(x);
        int cj = grid.toCellJ(z);
        int spawnIdx = pathFinder.nearestWalkable(ci, cj, 16);
        if (spawnIdx < 0) return null;
        int sx = spawnIdx % grid.size();
        int sz = spawnIdx / grid.size();
        grid.cellCenter(sx, sz, scratch);
        return ecs.spawnUnit(type, scratch.x, scratch.y, scratch.z, team);
    }
}
