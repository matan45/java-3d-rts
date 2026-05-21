package com.boot.core;

import com.boot.ai.NavGrid;
import com.boot.ai.PathFinder;
import com.boot.economy.BuildingEconomy;
import com.boot.physics.PhysicsWorld;
import com.boot.physics.TerrainCollider;
import com.boot.render.NavDebugMesh;
import com.boot.render.Renderer;
import com.boot.render.TerrainMesh;
import com.boot.ui.Hud;
import com.boot.ui.HudState;
import com.boot.ui.Minimap;
import com.boot.units.Unit;
import com.boot.units.UnitManager;
import com.boot.units.UnitType;
import com.boot.world.Heightmap;
import com.boot.world.PlacedBuilding;
import com.boot.world.RtsCamera;
import com.boot.world.SupplyPile;
import com.boot.world.SupplyPileScatter;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public final class Engine {

    private static final float FOOTPRINT_HALF = 3f;
    private static final float MAX_SLOPE_DELTA = 1.5f;
    private static final float SELECTION_HEIGHT = 12f;
    private static final int DEFAULT_HP = 100;

    private Window window;
    private Input input;
    private Heightmap heightmap;
    private TerrainMesh terrainMesh;
    private RtsCamera camera;
    private Renderer renderer;
    private Hud hud;
    private Minimap minimap;
    private PhysicsWorld physics;
    private TerrainCollider terrainCollider;

    private NavGrid navGrid;
    private PathFinder pathFinder;
    private NavDebugMesh navDebugMesh;
    private UnitManager unitManager;
    private boolean showNavDebug = false;
    private final List<Vector3f> debugPath = new ArrayList<>();
    private Vector3f debugPathStart;

    private final List<PlacedBuilding> placedBuildings = new ArrayList<>();
    private final List<SupplyPile> supplyPiles = new ArrayList<>();
    private float cashAccumulator = 0f;

    private static final long WORLD_SEED = 0xC0FFEEL;

    public void run() {
        try {
            init();
            loop();
        } finally {
            dispose();
        }
    }

    private void init() {
        window = new Window("Java 3D RTS", 1600, 900);
        input = new Input(window);

        heightmap = new Heightmap(256, 1.0f, 30f, WORLD_SEED);
        terrainMesh = new TerrainMesh(heightmap);

        supplyPiles.addAll(SupplyPileScatter.scatter(heightmap, WORLD_SEED));

        physics = new PhysicsWorld();
        terrainCollider = new TerrainCollider(physics, heightmap);
        physics.step(0.001f);

        navGrid = new NavGrid(heightmap, 2.0f, 0.18f * heightmap.maxHeight());
        navGrid.rebuildObstacles(placedBuildings, supplyPiles);
        pathFinder = new PathFinder(navGrid);
        navDebugMesh = new NavDebugMesh(navGrid);
        unitManager = new UnitManager(navGrid, pathFinder, heightmap);

        camera = new RtsCamera();
        camera.target().set(heightmap.worldSize() * 0.5f, 0f, heightmap.worldSize() * 0.5f);

        renderer = new Renderer();
        hud = new Hud();
        hud.init(window.handle());
        minimap = new Minimap(heightmap);
        hud.attachMinimap(minimap);

        HudState initialState = hud.state();
        initialState.supplyPilesView = supplyPiles;
        int totalMapCash = 0;
        for (SupplyPile p : supplyPiles) totalMapCash += p.cash();
        initialState.mapCashAvailable = totalMapCash;

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
    }

    private void loop() {
        double lastTime = glfwGetTime();

        while (!window.shouldClose()) {
            double now = glfwGetTime();
            float dt = (float) (now - lastTime);
            lastTime = now;

            input.beginFrame();

            if (input.isKeyPressed(GLFW_KEY_ESCAPE)) {
                window.requestClose();
            }

            camera.update(input, heightmap, window, dt);
            physics.step(dt);

            camera.updateMatrices(window.aspect());

            Vector3f hover = renderer.pickTerrain(
                    input.cursorX(), input.cursorY(),
                    window, camera, physics,
                    input.gameWantsMouse());

            if (input.isKeyJustPressed(GLFW_KEY_F3)) {
                showNavDebug = !showNavDebug;
            }
            if (input.isKeyJustPressed(GLFW_KEY_F4) && hover != null) {
                if (debugPathStart == null) {
                    debugPathStart = new Vector3f(hover);
                    debugPath.clear();
                } else {
                    int sx = navGrid.toCellI(debugPathStart.x);
                    int sz = navGrid.toCellJ(debugPathStart.z);
                    int gx = navGrid.toCellI(hover.x);
                    int gz = navGrid.toCellJ(hover.z);
                    pathFinder.findPathToNearest(sx, sz, gx, gz, 24, debugPath);
                    debugPathStart = null;
                }
            }

            HudState state = hud.state();

            int incomeRate = 0;
            for (PlacedBuilding p : placedBuildings) incomeRate += BuildingEconomy.income(p.name());
            state.cashPerSecond = incomeRate;
            cashAccumulator += incomeRate * dt;
            if (cashAccumulator >= 1f) {
                int whole = (int) cashAccumulator;
                state.cash += whole;
                cashAccumulator -= whole;
            }

            PlacedBuilding ghost = null;
            boolean ghostValid = false;
            if (state.pendingPlacementType != null) {
                ghostValid = isPlacementValid(hover, state);
                if (input.isMousePressed(GLFW_MOUSE_BUTTON_RIGHT)) {
                    state.pendingPlacementType = null;
                } else if (input.isMousePressed(GLFW_MOUSE_BUTTON_LEFT) && ghostValid) {
                    state.cash -= BuildingEconomy.cost(state.pendingPlacementType);
                    PlacedBuilding placed = new PlacedBuilding(
                            state.pendingPlacementType,
                            hover.x, hover.y, hover.z, FOOTPRINT_HALF);
                    placedBuildings.add(placed);
                    navGrid.blockBuilding(placed);
                    navDebugMesh.rebuild(navGrid);
                    state.pendingPlacementType = null;
                } else if (hover != null) {
                    ghost = new PlacedBuilding(
                            state.pendingPlacementType,
                            hover.x, hover.y, hover.z, FOOTPRINT_HALF);
                }
            } else {
                if (input.isMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
                    boolean shift = input.isKeyPressed(GLFW_KEY_LEFT_SHIFT)
                            || input.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
                    Unit hitUnit = renderer.pickUnit(
                            input.cursorX(), input.cursorY(),
                            window, camera, unitManager, input.gameWantsMouse());
                    if (hitUnit != null) {
                        clearBuildingSelection(state);
                        if (shift) {
                            unitManager.addToSelection(hitUnit);
                        } else {
                            unitManager.selectSingle(hitUnit);
                        }
                        syncUnitSelection(state);
                    } else {
                        int idx = renderer.pickBuilding(
                                input.cursorX(), input.cursorY(),
                                window, camera, placedBuildings, SELECTION_HEIGHT,
                                input.gameWantsMouse());
                        if (idx >= 0) {
                            PlacedBuilding hit = placedBuildings.get(idx);
                            unitManager.deselectAll();
                            syncUnitSelection(state);
                            state.selectedBuilding = hit;
                            state.selectionName = hit.name();
                            state.selectionType = "Structure";
                            state.selectionHp = DEFAULT_HP;
                            state.selectionMaxHp = DEFAULT_HP;
                            state.selectionVeterancy = 0;
                        } else if (hover != null) {
                            unitManager.deselectAll();
                            syncUnitSelection(state);
                            clearBuildingSelection(state);
                        }
                    }
                }
                if (input.isMousePressed(GLFW_MOUSE_BUTTON_RIGHT)
                        && !unitManager.selected().isEmpty()
                        && hover != null) {
                    boolean queue = input.isKeyPressed(GLFW_KEY_LEFT_SHIFT)
                            || input.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
                    SupplyPile pile = pileAt(hover);
                    if (pile != null && unitManager.anyWorkerSelected()) {
                        unitManager.issueHarvest(pile, placedBuildings);
                    } else {
                        unitManager.issueMove(hover, queue);
                    }
                }
            }

            if (state.pendingUnitProduction != null) {
                handleUnitProduction(state);
            }

            unitManager.tick(dt, supplyPiles, placedBuildings, state);
            if (unitManager.navObstaclesChanged()) {
                navDebugMesh.rebuild(navGrid);
                unitManager.clearNavObstaclesChanged();
            }

            renderer.render(window, camera, terrainMesh, placedBuildings, supplyPiles,
                    unitManager.units(),
                    ghost, ghostValid,
                    showNavDebug ? navDebugMesh : null,
                    debugPath);

            hud.frame(dt, window, camera, hover);

            input.endFrame();
            window.swapAndPoll();
        }
    }

    private static final float PILE_PICK_HALF = 2f;

    private SupplyPile pileAt(Vector3f world) {
        for (SupplyPile p : supplyPiles) {
            if (p.depleted()) continue;
            if (Math.abs(p.cx() - world.x) <= PILE_PICK_HALF
                    && Math.abs(p.cz() - world.z) <= PILE_PICK_HALF) {
                return p;
            }
        }
        return null;
    }

    private void clearBuildingSelection(HudState state) {
        state.selectedBuilding = null;
        state.selectionName = "";
        state.selectionType = "";
        state.selectionHp = 0;
        state.selectionMaxHp = 0;
        state.selectionVeterancy = 0;
    }

    private void syncUnitSelection(HudState state) {
        state.selectedUnits.clear();
        state.selectedUnits.addAll(unitManager.selected());
    }

    private void handleUnitProduction(HudState state) {
        String name = state.pendingUnitProduction;
        state.pendingUnitProduction = null;
        UnitType type = UnitType.byName(name);
        if (type == null) {
            state.cash += BuildingEconomy.unitCost(name);
            return;
        }
        PlacedBuilding src = state.selectedBuilding;
        if (src == null) {
            state.cash += BuildingEconomy.unitCost(name);
            return;
        }
        float offset = src.halfSize() + 2.0f;
        unitManager.spawn(type, src.cx() + offset, src.cz());
    }

    private boolean isPlacementValid(Vector3f hover, HudState state) {
        if (hover == null) return false;
        if (state.cash < BuildingEconomy.cost(state.pendingPlacementType)) return false;
        float cx = hover.x, cz = hover.z, h = FOOTPRINT_HALF;
        float a = heightmap.heightAt(cx - h, cz - h);
        float b = heightmap.heightAt(cx + h, cz - h);
        float c = heightmap.heightAt(cx - h, cz + h);
        float d = heightmap.heightAt(cx + h, cz + h);
        float min = Math.min(Math.min(a, b), Math.min(c, d));
        float max = Math.max(Math.max(a, b), Math.max(c, d));
        if (max - min > MAX_SLOPE_DELTA) return false;
        for (PlacedBuilding p : placedBuildings) {
            if (p.overlapsXZ(cx, cz, h)) return false;
        }
        return true;
    }

    private void dispose() {
        if (hud != null) hud.dispose();
        if (navDebugMesh != null) navDebugMesh.dispose();
        if (renderer != null) renderer.dispose();
        if (terrainMesh != null) terrainMesh.dispose();
        if (terrainCollider != null) terrainCollider.dispose();
        if (physics != null) physics.dispose();
        if (input != null) input.dispose();
        if (window != null) window.dispose();
    }
}
