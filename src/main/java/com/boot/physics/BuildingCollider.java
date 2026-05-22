package com.boot.physics;

import dev.dominion.ecs.api.Entity;
import physx.physics.PxRigidStatic;

import java.util.HashMap;
import java.util.Map;

public final class BuildingCollider {

    private static final float BUILDING_HALF_HEIGHT = 6f;

    private final PhysicsWorld world;
    private final Map<Entity, PxRigidStatic> actors = new HashMap<>();

    public BuildingCollider(PhysicsWorld world) {
        this.world = world;
    }

    public void addBuilding(Entity entity, float cx, float cy, float cz, float halfSize) {
        if (actors.containsKey(entity)) return;
        PxRigidStatic actor = world.createStaticBox(
                cx, cy + BUILDING_HALF_HEIGHT, cz,
                halfSize, BUILDING_HALF_HEIGHT, halfSize,
                PhysicsWorld.FILTER_BUILDING);
        actors.put(entity, actor);
    }

    public void removeBuilding(Entity entity) {
        PxRigidStatic actor = actors.remove(entity);
        if (actor != null) world.releaseActor(actor);
    }

    public void dispose() {
        for (PxRigidStatic actor : actors.values()) {
            world.releaseActor(actor);
        }
        actors.clear();
    }
}
