package com.boot.ecs.components;

public final class HealthState {

    public int hp;
    public int maxHp;

    public HealthState(int hp, int maxHp) {
        this.hp = hp;
        this.maxHp = maxHp;
    }
}
