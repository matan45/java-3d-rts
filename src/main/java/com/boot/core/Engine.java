package com.boot.core;

import com.boot.ai.NavGrid;
import com.boot.ai.PathFinder;
import com.boot.ecs.EcsWorld;
import com.boot.ecs.components.BuildingType;
import com.boot.ecs.components.HealthState;
import com.boot.ecs.components.SupplyCash;
import com.boot.ecs.components.Team;
import com.boot.ecs.components.TeamOwner;
import com.boot.ecs.components.Transform;
import com.boot.ecs.components.UnitKind;
import com.boot.ecs.systems.CollisionSystem;
import com.boot.ecs.systems.DepositSystem;
import com.boot.ecs.systems.HarvestSystem;
import com.boot.ecs.systems.IncomeSystem;
import com.boot.ecs.systems.NavObstacleSync;
import com.boot.ecs.systems.PathFollowSystem;
import com.boot.ecs.systems.PhysxSyncSystem;
import com.boot.ecs.systems.SelectionSystem;
import com.boot.ecs.systems.TerrainStickSystem;
import com.boot.ecs.systems.UnitSpawnSystem;
import com.boot.ecs.systems.VisionSystem;
import com.boot.economy.BuildingEconomy;
import com.boot.physics.BuildingCollider;
import com.boot.physics.PhysicsWorld;
import com.boot.physics.TerrainCollider;
import com.boot.render.FogTexture;
import com.boot.render.NavDebugMesh;
import com.boot.render.Renderer;
import com.boot.render.TerrainMesh;
import com.boot.ui.Hud;
import com.boot.ui.HudState;
import com.boot.ui.Minimap;
import com.boot.units.UnitType;
import com.boot.world.BuildingGhost;
import com.boot.world.Heightmap;
import com.boot.world.RtsCamera;
import com.boot.world.SupplyPileScatter;
import com.boot.world.VisionGrid;
import dev.dominion.ecs.api.Entity;
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
    private static final float PILE_PICK_HALF = 2f;

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
    private BuildingCollider buildingCollider;
    private EcsWorld ecs;
    private VisionGrid visionGrid;
    private FogTexture fogTexture;

    private NavGrid navGrid;
    private PathFinder pathFinder;
    private NavDebugMesh navDebugMesh;
    private boolean showNavDebug = false;
    private final List<Vector3f> debugPath = new ArrayList<>();
    private Vector3f debugPathStart;

    private boolean lmbDragging;
    private float dragStartX, dragStartY;
    private static final float DRAG_THRESHOLD_SQ = 5f * 5f;

    private float cashAccumulator = 0f;

    private final SelectionSystem selectionSystem = new SelectionSystem();
    private final HarvestSystem harvestSystem = new HarvestSystem();
    private final DepositSystem depositSystem = new DepositSystem();

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

        int mapSize = 256;
        float quad = 1.0f;
        float vScale = 30f;
        float worldExtent = (mapSize - 1) * quad;
        float plateauMargin = 40f;
        float plateauInner = 22f;
        float plateauOuter = 42f;
        float plateauHeight = 8f;
        float near = plateauMargin;
        float far = worldExtent - plateauMargin;
        List<Heightmap.Plateau> plateaus = List.of(
                new Heightmap.Plateau(near, near, plateauInner, plateauOuter, plateauHeight),
                new Heightmap.Plateau(far,  near, plateauInner, plateauOuter, plateauHeight),
                new Heightmap.Plateau(near, far,  plateauInner, plateauOuter, plateauHeight),
                new Heightmap.Plateau(far,  far,  plateauInner, plateauOuter, plateauHeight)
        );
        heightmap = new Heightmap(mapSize, quad, vScale, WORLD_SEED, plateaus);
        terrainMesh = new TerrainMesh(heightmap);

        ecs = new EcsWorld();
        SupplyPileScatter.scatter(heightmap, WORLD_SEED, ecs);

        physics = new PhysicsWorld();
        terrainCollider = new TerrainCollider(physics, heightmap);
        buildingCollider = new BuildingCollider(physics);
        physics.step(0.001f);
        ecs.attachPhysics(physics);

        navGrid = new NavGrid(heightmap, 2.0f, 0.18f * heightmap.maxHeight());
        NavObstacleSync.rebuild(ecs, navGrid);
        pathFinder = new PathFinder(navGrid);
        navDebugMesh = new NavDebugMesh(navGrid);

        spawnEnemies();
        NavObstacleSync.rebuild(ecs, navGrid);
        navDebugMesh.rebuild(navGrid);

        camera = new RtsCamera();
        if (!heightmap.plateaus().isEmpty()) {
            Heightmap.Plateau home = heightmap.plateaus().get(0);
            camera.target().set(home.worldX(), home.targetHeight(), home.worldZ());
        } else {
            camera.target().set(heightmap.worldSize() * 0.5f, 0f, heightmap.worldSize() * 0.5f);
        }

        renderer = new Renderer();
        hud = new Hud();
        hud.init(window.handle());
        minimap = new Minimap(heightmap);
        hud.attachMinimap(minimap);
        hud.attachEcs(ecs);

        visionGrid = new VisionGrid(heightmap.worldSize(), 2.0f);
        hud.attachVisionGrid(visionGrid);
        if (!heightmap.plateaus().isEmpty()) {
            Heightmap.Plateau home = heightmap.plateaus().get(0);
            visionGrid.revealPermanent(home.worldX(), home.worldZ(), home.outerRadius() + 6f);
        }
        fogTexture = new FogTexture(visionGrid.width(), visionGrid.height());
        VisionSystem.step(ecs, visionGrid);
        fogTexture.update(visionGrid);
        minimap.update(visionGrid);

        HudState initialState = hud.state();
        int[] totalMapCash = { 0 };
        ecs.dominion().findEntitiesWith(SupplyCash.class)
                .stream().forEach(r -> totalMapCash[0] += r.comp().cash);
        initialState.mapCashAvailable = totalMapCash[0];

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

            int incomeRate = IncomeSystem.totalRate(ecs);
            state.cashPerSecond = incomeRate;
            cashAccumulator += incomeRate * dt;
            if (cashAccumulator >= 1f) {
                int whole = (int) cashAccumulator;
                state.cash += whole;
                cashAccumulator -= whole;
            }

            BuildingGhost ghost = null;
            boolean ghostValid = false;
            if (state.pendingPlacementType != null) {
                ghostValid = isPlacementValid(hover, state);
                if (input.isMousePressed(GLFW_MOUSE_BUTTON_RIGHT)) {
                    state.pendingPlacementType = null;
                } else if (input.isMousePressed(GLFW_MOUSE_BUTTON_LEFT) && ghostValid) {
                    state.cash -= BuildingEconomy.cost(state.pendingPlacementType);
                    Entity entity = ecs.spawnBuilding(
                            state.pendingPlacementType,
                            hover.x, hover.y, hover.z, FOOTPRINT_HALF, Team.PLAYER);
                    buildingCollider.addBuilding(entity, hover.x, hover.y, hover.z, FOOTPRINT_HALF);
                    navGrid.blockBuilding(hover.x, hover.z, FOOTPRINT_HALF);
                    navDebugMesh.rebuild(navGrid);
                    state.pendingPlacementType = null;
                } else if (hover != null) {
                    ghost = new BuildingGhost(
                            state.pendingPlacementType,
                            hover.x, hover.y, hover.z, FOOTPRINT_HALF);
                }
            } else {
                handleSelectionInput(state, hover);
            }

            if (state.pendingUnitProduction != null) {
                handleUnitProduction(state);
            }

            PathFollowSystem.step(ecs, dt);
            harvestSystem.step(ecs, dt, state, navGrid, pathFinder);
            depositSystem.step(ecs, state, navGrid, pathFinder);
            CollisionSystem.step(ecs);
            TerrainStickSystem.step(ecs, heightmap);
            PhysxSyncSystem.step(ecs, physics);
            VisionSystem.step(ecs, visionGrid);
            fogTexture.update(visionGrid);
            minimap.update(visionGrid);

            if (harvestSystem.consumeNavDirty()) {
                NavObstacleSync.rebuild(ecs, navGrid);
                navDebugMesh.rebuild(navGrid);
            }

            renderer.render(window, camera, terrainMesh, ecs,
                    ghost, ghostValid,
                    showNavDebug ? navDebugMesh : null,
                    debugPath,
                    visionGrid, fogTexture);

            hud.frame(dt, window, camera, hover);

            ecs.flushDestroys();

            input.endFrame();
            window.swapAndPoll();
        }
    }

    private void handleSelectionInput(HudState state, Vector3f hover) {
        boolean lmbHeld = input.isMouseHeldAny(GLFW_MOUSE_BUTTON_LEFT);
        boolean lmbPressed = input.isMousePressed(GLFW_MOUSE_BUTTON_LEFT);

        if (lmbPressed && hover != null) {
            lmbDragging = true;
            dragStartX = (float) input.cursorX();
            dragStartY = (float) input.cursorY();
            state.dragActive = false;
        }

        if (lmbDragging && lmbHeld) {
            float cx = (float) input.cursorX();
            float cy = (float) input.cursorY();
            float dx = cx - dragStartX;
            float dy = cy - dragStartY;
            if (dx * dx + dy * dy > DRAG_THRESHOLD_SQ) {
                state.dragActive = true;
                state.dragX0 = Math.min(dragStartX, cx);
                state.dragY0 = Math.min(dragStartY, cy);
                state.dragX1 = Math.max(dragStartX, cx);
                state.dragY1 = Math.max(dragStartY, cy);
            }
        }

        if (lmbDragging && !lmbHeld) {
            boolean shift = input.isKeyPressed(GLFW_KEY_LEFT_SHIFT)
                    || input.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
            if (state.dragActive) {
                selectUnitsInRect(state, shift);
            } else {
                handleSingleClickSelect(state, hover, shift);
            }
            lmbDragging = false;
            state.dragActive = false;
        }

        if (!lmbDragging
                && input.isMousePressed(GLFW_MOUSE_BUTTON_RIGHT)
                && selectionSystem.anySelected(ecs)
                && hover != null) {
            boolean queue = input.isKeyPressed(GLFW_KEY_LEFT_SHIFT)
                    || input.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);
            Entity pile = pileAt(hover);
            if (pile != null && selectionSystem.anyWorkerSelected(ecs)) {
                selectionSystem.issueHarvest(ecs, pile, navGrid, pathFinder);
            } else {
                selectionSystem.issueMove(ecs, hover, queue, navGrid, pathFinder);
            }
        }
    }

    private void handleSingleClickSelect(HudState state, Vector3f hover, boolean shift) {
        Entity hitUnit = renderer.pickUnit(
                input.cursorX(), input.cursorY(),
                window, camera, ecs, selectionSystem, input.gameWantsMouse());
        if (hitUnit != null) {
            clearBuildingSelection(state);
            if (shift) {
                selectionSystem.addToSelection(hitUnit);
            } else {
                selectionSystem.selectSingle(ecs, hitUnit);
            }
            syncUnitSelection(state);
            return;
        }
        Entity hitBuilding = renderer.pickBuilding(
                input.cursorX(), input.cursorY(),
                window, camera, ecs, SELECTION_HEIGHT,
                input.gameWantsMouse());
        if (hitBuilding != null) {
            selectionSystem.deselectAll(ecs);
            syncUnitSelection(state);
            BuildingType bt = hitBuilding.get(BuildingType.class);
            HealthState hp = hitBuilding.get(HealthState.class);
            state.selectedBuilding = hitBuilding;
            state.selectionName = bt != null ? bt.name() : "";
            state.selectionType = "Structure";
            state.selectionHp = hp != null ? hp.hp : DEFAULT_HP;
            state.selectionMaxHp = hp != null ? hp.maxHp : DEFAULT_HP;
            state.selectionVeterancy = 0;
            return;
        }
        if (hover != null) {
            selectionSystem.deselectAll(ecs);
            syncUnitSelection(state);
            clearBuildingSelection(state);
        }
    }

    private final float[] projScratch = new float[2];

    private void selectUnitsInRect(HudState state, boolean shift) {
        if (!shift) selectionSystem.deselectAll(ecs);
        ecs.dominion().findEntitiesWith(Transform.class, UnitKind.class)
                .stream().forEach(r -> {
                    if (!TeamOwner.isPlayer(r.entity())) return;
                    Transform t = r.comp1();
                    UnitKind k = r.comp2();
                    float cx = t.pos.x;
                    float cy = t.pos.y + k.type().height * 0.5f;
                    float cz = t.pos.z;
                    if (!renderer.projectToScreen(cx, cy, cz, camera, window, projScratch)) return;
                    float sx = projScratch[0];
                    float sy = projScratch[1];
                    if (sx >= state.dragX0 && sx <= state.dragX1
                            && sy >= state.dragY0 && sy <= state.dragY1) {
                        selectionSystem.addToSelection(r.entity());
                    }
                });
        clearBuildingSelection(state);
        syncUnitSelection(state);
    }

    private void spawnEnemies() {
        java.util.List<Heightmap.Plateau> all = heightmap.plateaus();
        if (all.size() < 2) return;
        for (int i = 1; i < all.size(); i++) {
            Heightmap.Plateau p = all.get(i);
            float bx = p.worldX();
            float bz = p.worldZ();
            float by = heightmap.heightAt(bx, bz);
            Entity b = ecs.spawnBuilding("Barracks", bx, by, bz, FOOTPRINT_HALF, Team.ENEMY);
            buildingCollider.addBuilding(b, bx, by, bz, FOOTPRINT_HALF);
            navGrid.blockBuilding(bx, bz, FOOTPRINT_HALF);

            UnitSpawnSystem.spawnNear(ecs, UnitType.RANGER, bx + 7f, bz + 2f, navGrid, pathFinder, Team.ENEMY);
            UnitSpawnSystem.spawnNear(ecs, UnitType.RANGER, bx - 2f, bz + 7f, navGrid, pathFinder, Team.ENEMY);
        }
    }

    private Entity pileAt(Vector3f world) {
        Entity[] best = { null };
        ecs.dominion().findEntitiesWith(Transform.class, SupplyCash.class)
                .stream().forEach(r -> {
                    if (r.comp2().cash <= 0) return;
                    Transform t = r.comp1();
                    if (Math.abs(t.pos.x - world.x) <= PILE_PICK_HALF
                            && Math.abs(t.pos.z - world.z) <= PILE_PICK_HALF) {
                        best[0] = r.entity();
                    }
                });
        return best[0];
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
        state.selectedUnits.addAll(selectionSystem.selectedUnits(ecs));
    }

    private void handleUnitProduction(HudState state) {
        String name = state.pendingUnitProduction;
        state.pendingUnitProduction = null;
        UnitType type = UnitType.byName(name);
        if (type == null) {
            state.cash += BuildingEconomy.unitCost(name);
            return;
        }
        Entity src = state.selectedBuilding;
        if (src == null) {
            state.cash += BuildingEconomy.unitCost(name);
            return;
        }
        Transform st = src.get(Transform.class);
        BuildingType bt = src.get(BuildingType.class);
        float offset = (bt != null ? bt.halfSize() : FOOTPRINT_HALF) + 2.0f;
        UnitSpawnSystem.spawnNear(ecs, type, st.pos.x + offset, st.pos.z, navGrid, pathFinder, Team.PLAYER);
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
        boolean[] overlap = { false };
        ecs.dominion().findEntitiesWith(Transform.class, BuildingType.class)
                .stream().forEach(r -> {
                    if (overlap[0]) return;
                    Vector3f p = r.comp1().pos;
                    float bh = r.comp2().halfSize();
                    if (Math.abs(p.x - cx) < (bh + h) && Math.abs(p.z - cz) < (bh + h)) {
                        overlap[0] = true;
                    }
                });
        return !overlap[0];
    }

    private void dispose() {
        if (hud != null) hud.dispose();
        if (navDebugMesh != null) navDebugMesh.dispose();
        if (renderer != null) renderer.dispose();
        if (fogTexture != null) fogTexture.dispose();
        if (minimap != null) minimap.dispose();
        if (terrainMesh != null) terrainMesh.dispose();
        if (ecs != null) ecs.shutdown();
        if (buildingCollider != null) buildingCollider.dispose();
        if (terrainCollider != null) terrainCollider.dispose();
        if (physics != null) physics.dispose();
        if (input != null) input.dispose();
        if (window != null) window.dispose();
    }
}
