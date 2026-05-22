package com.boot.ecs.components;

public final class Renderable {

    public enum Kind { UNIT_CUBE, BUILDING_MESH, SUPPLY_CUBE }

    public Kind kind;
    public float r, g, b, a;
    public float hx, hy, hz;
    public String meshKey;

    public Renderable(Kind kind, float r, float g, float b, float a,
                      float hx, float hy, float hz, String meshKey) {
        this.kind = kind;
        this.r = r; this.g = g; this.b = b; this.a = a;
        this.hx = hx; this.hy = hy; this.hz = hz;
        this.meshKey = meshKey;
    }
}
