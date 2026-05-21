package com.boot.world;

public final class SupplyPile {

    private final float cx;
    private final float cy;
    private final float cz;
    private int cash;

    public SupplyPile(float cx, float cy, float cz, int cash) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.cash = cash;
    }

    public float cx() { return cx; }
    public float cy() { return cy; }
    public float cz() { return cz; }
    public int cash() { return cash; }

    public int drain(int amount) {
        int taken = Math.min(amount, cash);
        cash -= taken;
        return taken;
    }

    public boolean depleted() { return cash <= 0; }
}
