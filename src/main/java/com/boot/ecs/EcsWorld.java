package com.boot.ecs;

import com.boot.ecs.components.BuildingTag;
import com.boot.ecs.components.BuildingType;
import com.boot.ecs.components.Harvester;
import com.boot.ecs.components.HealthState;
import com.boot.ecs.components.IncomeSource;
import com.boot.ecs.components.PathFollower;
import com.boot.ecs.components.PhysicsBody;
import com.boot.ecs.components.Renderable;
import com.boot.ecs.components.Selectable;
import com.boot.ecs.components.SupplyCash;
import com.boot.ecs.components.SupplyPileTag;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import com.boot.ecs.components.UnitTag;
import com.boot.economy.BuildingEconomy;
import com.boot.physics.PhysicsWorld;
import com.boot.units.UnitType;
import dev.dominion.ecs.api.Dominion;
import dev.dominion.ecs.api.Entity;
import physx.physics.PxRigidDynamic;

import java.util.ArrayList;
import java.util.List;

public final class EcsWorld {

    private static final float SUPPLY_TINT_R = 0.20f;
    private static final float SUPPLY_TINT_G = 0.85f;
    private static final float SUPPLY_TINT_B = 0.30f;
    private static final float SUPPLY_HALF = 2f;

    private static final float BUILDING_TINT_R = 0.78f;
    private static final float BUILDING_TINT_G = 0.74f;
    private static final float BUILDING_TINT_B = 0.65f;
    private static final int BUILDING_DEFAULT_HP = 100;

    private static final int UNIT_DEFAULT_HP = 100;

    private final Dominion dominion = Dominion.create("rts");
    private final List<Entity> pendingDestroy = new ArrayList<>();

    private PhysicsWorld physics;

    public Dominion dominion() {
        return dominion;
    }

    public void attachPhysics(PhysicsWorld physics) {
        this.physics = physics;
    }

    public Entity spawnPile(float cx, float cy, float cz, int cash) {
        return dominion.createEntity(
                new Transform(cx, cy, cz),
                new SupplyCash(cash),
                new Renderable(Renderable.Kind.SUPPLY_CUBE,
                        SUPPLY_TINT_R, SUPPLY_TINT_G, SUPPLY_TINT_B, 1f,
                        SUPPLY_HALF, SUPPLY_HALF, SUPPLY_HALF, null),
                SupplyPileTag.INSTANCE
        );
    }

    public Entity spawnBuilding(String name, float cx, float cy, float cz, float halfSize) {
        return dominion.createEntity(
                new Transform(cx, cy, cz),
                new BuildingType(name, halfSize),
                new Renderable(Renderable.Kind.BUILDING_MESH,
                        BUILDING_TINT_R, BUILDING_TINT_G, BUILDING_TINT_B, 1f,
                        halfSize, halfSize, halfSize, name),
                new HealthState(BUILDING_DEFAULT_HP, BUILDING_DEFAULT_HP),
                new IncomeSource(BuildingEconomy.income(name)),
                BuildingTag.INSTANCE
        );
    }

    public Entity spawnUnit(UnitType type, float x, float y, float z) {
        PxRigidDynamic actor = physics != null
                ? physics.createKinematicSphere(x, y + type.radius, z, type.radius)
                : null;
        return dominion.createEntity(
                new Transform(x, y, z),
                new UnitKind(type),
                new PathFollower(),
                new Harvester(),
                new Selectable(),
                new HealthState(UNIT_DEFAULT_HP, UNIT_DEFAULT_HP),
                new PhysicsBody(actor),
                new Renderable(Renderable.Kind.UNIT_CUBE,
                        type.r, type.g, type.b, 1f,
                        type.radius, type.height * 0.5f, type.radius, null),
                UnitTag.INSTANCE
        );
    }

    public void requestDestroy(Entity e) {
        if (e != null) pendingDestroy.add(e);
    }

    public void flushDestroys() {
        if (pendingDestroy.isEmpty()) return;
        for (Entity e : pendingDestroy) {
            releasePhysicsIfPresent(e);
            dominion.deleteEntity(e);
        }
        pendingDestroy.clear();
    }

    public void shutdown() {
        flushDestroys();
        if (physics != null) {
            dominion.findEntitiesWith(PhysicsBody.class).stream().forEach(r -> releaseBody(r.comp()));
        }
    }

    private void releasePhysicsIfPresent(Entity e) {
        if (physics == null) return;
        if (!e.has(PhysicsBody.class)) return;
        releaseBody(e.get(PhysicsBody.class));
    }

    private void releaseBody(PhysicsBody body) {
        if (body == null || body.released || body.actor == null) return;
        physics.releaseActor(body.actor);
        body.actor = null;
        body.released = true;
    }
}
