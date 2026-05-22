package com.boot.ecs.systems;

import com.boot.ai.NavGrid;
import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.BuildingType;
import com.boot.ecs.components.SupplyCash;
import com.boot.ecs.components.Transform;

public final class NavObstacleSync {

    private NavObstacleSync() {}

    public static void rebuild(EcsWorld ecs, NavGrid grid) {
        grid.resetObstacles();
        ecs.dominion().findEntitiesWith(Transform.class, BuildingType.class)
                .stream().forEach(r -> grid.blockBuilding(r.comp1().pos.x, r.comp1().pos.z, r.comp2().halfSize()));
        ecs.dominion().findEntitiesWith(Transform.class, SupplyCash.class)
                .stream().forEach(r -> grid.blockPile(r.comp1().pos.x, r.comp1().pos.z));
    }
}
