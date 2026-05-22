package com.boot.ecs.components;

import org.joml.Vector3f;

import java.util.ArrayDeque;

public final class PathFollower {

    public final ArrayDeque<Vector3f> path = new ArrayDeque<>();
    public MovementState state = MovementState.IDLE;
}
