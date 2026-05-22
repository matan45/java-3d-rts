package com.boot.ecs.systems;

import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitTag;
import com.boot.world.Heightmap;

public final class TerrainStickSystem {

    private TerrainStickSystem() {}

    public static void step(EcsWorld ecs, Heightmap heightmap) {
        ecs.dominion().findEntitiesWith(Transform.class, UnitTag.class)
                .stream().forEach(r -> {
                    Transform t = r.comp1();
                    t.pos.y = heightmap.heightAt(t.pos.x, t.pos.z);
                });
    }
}
