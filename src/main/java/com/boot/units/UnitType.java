package com.boot.units;

public enum UnitType {
    RANGER("Ranger", 6.0f, 0.85f, 0.20f, 0.20f, 1.0f, 1.8f, 0,    0f),
    DOZER ("Dozer",  4.5f, 0.95f, 0.78f, 0.20f, 1.4f, 1.5f, 300,  1.5f);

    public final String label;
    public final float speed;
    public final float r, g, b;
    public final float radius;
    public final float height;
    public final int carryCap;
    public final float harvestPeriod;

    UnitType(String label, float speed,
             float r, float g, float b,
             float radius, float height,
             int carryCap, float harvestPeriod) {
        this.label = label;
        this.speed = speed;
        this.r = r; this.g = g; this.b = b;
        this.radius = radius;
        this.height = height;
        this.carryCap = carryCap;
        this.harvestPeriod = harvestPeriod;
    }

    public boolean isWorker() { return carryCap > 0; }

    public static UnitType byName(String name) {
        for (UnitType t : values()) if (t.label.equals(name)) return t;
        return null;
    }
}
