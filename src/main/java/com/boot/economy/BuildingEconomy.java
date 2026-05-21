package com.boot.economy;

import java.util.HashMap;
import java.util.Map;

public final class BuildingEconomy {

    private static final Map<String, Integer> COST = new HashMap<>();
    private static final Map<String, Integer> INCOME_PER_SEC = new HashMap<>();

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
}
