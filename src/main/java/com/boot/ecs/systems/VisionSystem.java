package com.boot.ecs.systems;

import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.BuildingType;
import com.boot.ecs.components.TeamOwner;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import com.boot.world.VisionGrid;

public final class VisionSystem {

    private static final float UNIT_SIGHT = 18.0f;
    private static final float BUILDING_SIGHT = 22.0f;

    private VisionSystem() {}

    public static void step(EcsWorld ecs, VisionGrid grid) {
        grid.demoteVisibleToExplored();

        ecs.dominion().findEntitiesWith(Transform.class, UnitKind.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    Transform t = r.comp1();
                    grid.stampVisibleCircle(t.pos.x, t.pos.z, UNIT_SIGHT);
                });

        ecs.dominion().findEntitiesWith(Transform.class, BuildingType.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    Transform t = r.comp1();
                    grid.stampVisibleCircle(t.pos.x, t.pos.z, BUILDING_SIGHT);
                });
    }
}
