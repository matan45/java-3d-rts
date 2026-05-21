package com.boot.units;

import com.boot.world.PlacedBuilding;
import com.boot.world.SupplyPile;
import org.joml.Vector3f;

import java.util.ArrayDeque;

public final class Unit {

    public enum State { IDLE, MOVING, MOVING_TO_PILE, HARVESTING, MOVING_TO_BASE, DEPOSITING }

    public final UnitType type;
    public final Vector3f pos = new Vector3f();
    public float heading;
    public State state = State.IDLE;
    public final ArrayDeque<Vector3f> path = new ArrayDeque<>();
    public boolean selected;

    public int carriedCash;
    public SupplyPile targetPile;
    public PlacedBuilding dropoff;
    public float harvestTimer;

    public Unit(UnitType type, float x, float y, float z) {
        this.type = type;
        this.pos.set(x, y, z);
    }
}
