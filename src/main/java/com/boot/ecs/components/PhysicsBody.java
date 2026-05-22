package com.boot.ecs.components;

import physx.physics.PxRigidActor;

public final class PhysicsBody {

    public PxRigidActor actor;
    public boolean released;

    public PhysicsBody(PxRigidActor actor) {
        this.actor = actor;
    }
}
