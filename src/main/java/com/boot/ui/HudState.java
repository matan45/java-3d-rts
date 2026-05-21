package com.boot.ui;

public final class HudState {

    public int gold = 500;
    public int wood = 300;
    public int food = 200;
    public int stone = 100;

    public int population = 4;
    public int populationCap = 10;

    public double gameTimeSeconds = 0;

    public String selectionName = "";
    public int selectionHp;
    public int selectionMaxHp;

    public boolean hasSelection() { return !selectionName.isEmpty(); }

    public void tick(double dt) {
        gameTimeSeconds += dt;
    }

    public String formattedGameTime() {
        int t = (int) gameTimeSeconds;
        return String.format("%02d:%02d", t / 60, t % 60);
    }
}
