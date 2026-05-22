package com.boot.ecs.components;

import org.joml.Vector3f;

public final class Transform {

    public final Vector3f pos = new Vector3f();
    public float heading;

    public Transform(float x, float y, float z) {
        this.pos.set(x, y, z);
    }

    public Transform(float x, float y, float z, float heading) {
        this.pos.set(x, y, z);
        this.heading = heading;
    }
}
