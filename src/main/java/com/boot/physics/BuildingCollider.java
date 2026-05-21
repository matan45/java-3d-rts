package com.boot.physics;

import com.boot.world.PlacedBuilding;
import physx.physics.PxRigidStatic;

import java.util.HashMap;
import java.util.Map;

public final class BuildingCollider {

    private static final float BUILDING_HALF_HEIGHT = 6f;

    private final PhysicsWorld world;
    private final Map<PlacedBuilding, PxRigidStatic> actors = new HashMap<>();

    public BuildingCollider(PhysicsWorld world) {
        this.world = world;
    }

    public void addBuilding(PlacedBuilding b) {
        if (actors.containsKey(b)) return;
        float h = b.halfSize();
        PxRigidStatic actor = world.createStaticBox(
                b.cx(), b.cy() + BUILDING_HALF_HEIGHT, b.cz(),
                h, BUILDING_HALF_HEIGHT, h,
                PhysicsWorld.FILTER_BUILDING);
        actors.put(b, actor);
    }

    public void removeBuilding(PlacedBuilding b) {
        PxRigidStatic actor = actors.remove(b);
        if (actor != null) world.releaseActor(actor);
    }

    public void dispose() {
        for (PxRigidStatic actor : actors.values()) {
            world.releaseActor(actor);
        }
        actors.clear();
    }
}
