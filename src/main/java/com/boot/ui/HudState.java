package com.boot.ui;

import com.boot.economy.BuildingEconomy;
import com.boot.units.Unit;
import com.boot.world.PlacedBuilding;
import com.boot.world.SupplyPile;

import java.util.ArrayList;
import java.util.List;

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
    public PlacedBuilding selectedBuilding;
    public final List<Unit> selectedUnits = new ArrayList<>();

    public Tab activeTab = Tab.STRUCTURES;

    public String pendingPlacementType;
    public String pendingUnitProduction;

    public int mapCashAvailable = 0;
    public List<SupplyPile> supplyPilesView = List.of();

    public boolean hasSelection() { return !selectionName.isEmpty(); }
    public boolean hasUnitsSelected() { return !selectedUnits.isEmpty(); }
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
            "Black Market",
    };

    private static final String[] NONE = new String[0];

    public String[] currentTabItems() {
        return switch (activeTab) {
            case STRUCTURES -> STRUCTURES;
            case UNITS -> selectionName.isEmpty() ? NONE : BuildingEconomy.unitsFor(selectionName);
            case UPGRADES -> selectionName.isEmpty() ? NONE : BuildingEconomy.upgradesFor(selectionName);
        };
    }
}
