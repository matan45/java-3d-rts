package com.boot.ui;

public final class HudState {

    public enum Tab { STRUCTURES, UNITS, UPGRADES }

    public int cash = 2000;
    public int cashPerSecond = 0;

    public int powerProduced = 5;
    public int powerConsumed = 0;

    public double gameTimeSeconds = 0;

    public String selectionName = "";
    public String selectionType = "";
    public int selectionHp;
    public int selectionMaxHp;
    public int selectionVeterancy = 0;

    public Tab activeTab = Tab.STRUCTURES;

    public String pendingPlacementType;

    public boolean hasSelection() { return !selectionName.isEmpty(); }
    public int powerSurplus() { return powerProduced - powerConsumed; }
    public boolean lowPower() { return powerSurplus() < 0; }

    public void tick(double dt) {
        gameTimeSeconds += dt;
    }

    public String formattedGameTime() {
        int t = (int) gameTimeSeconds;
        return String.format("%02d:%02d", t / 60, t % 60);
    }

    public static final String[] STRUCTURES = {
            "Command Center", "Power Plant", "Supply Center",
            "Barracks", "War Factory", "Airfield",
            "Strategy Center", "Detention Camp", "Patriot Battery",
    };
    public static final String[] UNITS = {
            "Dozer", "Ranger", "Missile Defender",
            "Crusader", "Paladin", "Tomahawk",
            "Humvee", "Comanche", "Raptor",
    };
    public static final String[] UPGRADES = {
            "Composite Armor", "Capture Building", "Flashbangs",
            "Drone Armor", "TOW Missile", "Bunker Busters",
            "Stealth Comanche", "Laser Missiles", "Countermeasures",
    };

    public String[] currentTabItems() {
        return switch (activeTab) {
            case STRUCTURES -> STRUCTURES;
            case UNITS -> UNITS;
            case UPGRADES -> UPGRADES;
        };
    }
}
