package com.boot.economy;

import java.util.HashMap;
import java.util.Map;

public final class BuildingEconomy {

    private static final String[] NONE = new String[0];

    private static final Map<String, Integer> COST = new HashMap<>();
    private static final Map<String, Integer> INCOME_PER_SEC = new HashMap<>();
    private static final Map<String, Integer> UNIT_COST = new HashMap<>();
    private static final Map<String, Integer> UPGRADE_COST = new HashMap<>();
    private static final Map<String, String[]> UNITS_BY_BUILDING = new HashMap<>();
    private static final Map<String, String[]> UPGRADES_BY_BUILDING = new HashMap<>();

    static {
        COST.put("Command Center",   2000);
        COST.put("Power Plant",       800);
        COST.put("Supply Center",    1500);
        COST.put("Barracks",          500);
        COST.put("War Factory",      1500);
        COST.put("Airfield",         1000);
        COST.put("Strategy Center",  1500);
        COST.put("Detention Camp",   1000);
        COST.put("Patriot Battery",  1000);
        COST.put("Black Market",     1500);

        INCOME_PER_SEC.put("Supply Center", 5);
        INCOME_PER_SEC.put("Black Market",  8);

        UNITS_BY_BUILDING.put("Command Center", new String[] { "Dozer" });
        UNITS_BY_BUILDING.put("Barracks",       new String[] { "Ranger", "Missile Defender" });
        UNITS_BY_BUILDING.put("War Factory",    new String[] { "Crusader", "Paladin", "Tomahawk", "Humvee" });
        UNITS_BY_BUILDING.put("Airfield",       new String[] { "Comanche", "Raptor" });

        UPGRADES_BY_BUILDING.put("Barracks",        new String[] { "Capture Building", "Flashbangs" });
        UPGRADES_BY_BUILDING.put("War Factory",     new String[] { "Composite Armor", "TOW Missile", "Bunker Busters" });
        UPGRADES_BY_BUILDING.put("Airfield",        new String[] { "Stealth Comanche", "Laser Missiles", "Countermeasures" });
        UPGRADES_BY_BUILDING.put("Strategy Center", new String[] { "Drone Armor" });

        UNIT_COST.put("Dozer",            1000);
        UNIT_COST.put("Ranger",            200);
        UNIT_COST.put("Missile Defender",  300);
        UNIT_COST.put("Crusader",          800);
        UNIT_COST.put("Paladin",          1500);
        UNIT_COST.put("Tomahawk",         1200);
        UNIT_COST.put("Humvee",            700);
        UNIT_COST.put("Comanche",         1500);
        UNIT_COST.put("Raptor",           1200);

        UPGRADE_COST.put("Capture Building", 1000);
        UPGRADE_COST.put("Flashbangs",        600);
        UPGRADE_COST.put("Composite Armor",  1000);
        UPGRADE_COST.put("TOW Missile",      1500);
        UPGRADE_COST.put("Bunker Busters",    800);
        UPGRADE_COST.put("Stealth Comanche", 1500);
        UPGRADE_COST.put("Laser Missiles",   2500);
        UPGRADE_COST.put("Countermeasures",   800);
        UPGRADE_COST.put("Drone Armor",      1000);
    }

    private BuildingEconomy() {}

    public static int cost(String name) {
        if (name == null) return 0;
        Integer v = COST.get(name);
        return v == null ? 0 : v;
    }

    public static int income(String name) {
        if (name == null) return 0;
        Integer v = INCOME_PER_SEC.get(name);
        return v == null ? 0 : v;
    }

    public static int unitCost(String name) {
        if (name == null) return 0;
        Integer v = UNIT_COST.get(name);
        return v == null ? 0 : v;
    }

    public static int upgradeCost(String name) {
        if (name == null) return 0;
        Integer v = UPGRADE_COST.get(name);
        return v == null ? 0 : v;
    }

    public static String[] unitsFor(String building) {
        if (building == null) return NONE;
        String[] v = UNITS_BY_BUILDING.get(building);
        return v == null ? NONE : v;
    }

    public static String[] upgradesFor(String building) {
        if (building == null) return NONE;
        String[] v = UPGRADES_BY_BUILDING.get(building);
        return v == null ? NONE : v;
    }
}
