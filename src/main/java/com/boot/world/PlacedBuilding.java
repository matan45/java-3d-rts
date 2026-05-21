package com.boot.world;

public record PlacedBuilding(String name, float cx, float cy, float cz, float halfSize) {

    public boolean overlapsXZ(float ocx, float ocz, float oHalf) {
        return Math.abs(cx - ocx) < (halfSize + oHalf)
            && Math.abs(cz - ocz) < (halfSize + oHalf);
    }
}
