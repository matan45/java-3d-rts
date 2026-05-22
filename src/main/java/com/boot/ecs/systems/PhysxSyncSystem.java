package com.boot.ecs.systems;

import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.PhysicsBody;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import com.boot.physics.PhysicsWorld;
import physx.physics.PxRigidDynamic;

public final class PhysxSyncSystem {

    private PhysxSyncSystem() {}

    public static void step(EcsWorld ecs, PhysicsWorld physics) {
        if (physics == null) return;
        ecs.dominion().findEntitiesWith(Transform.class, PhysicsBody.class, UnitKind.class)
                .stream().forEach(r -> {
                    Transform t = r.comp1();
                    PhysicsBody body = r.comp2();
                    if (body.actor == null || body.released) return;
                    if (body.actor instanceof PxRigidDynamic dyn) {
                        physics.setKinematicPose(dyn, t.pos.x, t.pos.y + r.comp3().type().radius, t.pos.z);
                    }
                });
    }
}
