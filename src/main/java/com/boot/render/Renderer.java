package com.boot.render;

import com.boot.core.Window;
import com.boot.physics.PhysicsWorld;
import com.boot.ui.HudState;
import com.boot.units.Unit;
import com.boot.units.UnitManager;
import com.boot.world.PlacedBuilding;
import com.boot.world.RtsCamera;
import com.boot.world.SupplyPile;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11.*;

public final class Renderer {

    private final Shader terrainShader;
    private final Shader buildingShader;
    private final Map<String, Mesh> buildingMeshes = new HashMap<>();
    private final Mesh cubeMesh;

    private final Matrix4f model = new Matrix4f();
    private final Vector3f lightDir = new Vector3f(-0.45f, -0.85f, -0.30f).normalize().negate();
    private final Vector3f ambient = new Vector3f(0.30f, 0.32f, 0.38f);

    private static final float[] BUILDING_TINT  = { 0.78f, 0.74f, 0.65f, 1.00f };
    private static final float[] SUPPLY_TINT    = { 0.20f, 0.85f, 0.30f, 1.00f };
    private static final float[] GHOST_VALID    = { 0.30f, 1.00f, 0.40f, 0.45f };
    private static final float[] GHOST_INVALID  = { 1.00f, 0.30f, 0.30f, 0.45f };

    private static final float PILE_HALF = 2f;

    private final Vector3f rayOrigin = new Vector3f();
    private final Vector3f rayDir = new Vector3f();
    private final Vector3f pickResult = new Vector3f();

    public Renderer() {
        terrainShader = new Shader("/shaders/terrain.vert", "/shaders/terrain.frag");
        buildingShader = new Shader("/shaders/building.vert", "/shaders/building.frag");

        Mesh placeholder = AssimpLoader.loadResource("/models/placeholder_building.obj");
        for (String name : HudState.STRUCTURES) {
            buildingMeshes.put(name, placeholder);
        }

        cubeMesh = buildUnitCube();
    }

    private static Mesh buildUnitCube() {
        float[] verts = {
                -1, -1,  1,   1, -1,  1,   1,  1,  1,  -1,  1,  1,
                 1, -1, -1,  -1, -1, -1,  -1,  1, -1,   1,  1, -1,
                 1, -1,  1,   1, -1, -1,   1,  1, -1,   1,  1,  1,
                -1, -1, -1,  -1, -1,  1,  -1,  1,  1,  -1,  1, -1,
                -1,  1,  1,   1,  1,  1,   1,  1, -1,  -1,  1, -1,
                -1, -1, -1,   1, -1, -1,   1, -1,  1,  -1, -1,  1,
        };
        int[] idx = new int[36];
        for (int f = 0; f < 6; f++) {
            int base = f * 4;
            int o = f * 6;
            idx[o    ] = base;
            idx[o + 1] = base + 1;
            idx[o + 2] = base + 2;
            idx[o + 3] = base;
            idx[o + 4] = base + 2;
            idx[o + 5] = base + 3;
        }
        FloatBuffer vb = BufferUtils.createFloatBuffer(verts.length).put(verts);
        vb.flip();
        IntBuffer ib = BufferUtils.createIntBuffer(idx.length).put(idx);
        ib.flip();
        return new Mesh(vb, ib);
    }

    public void render(Window window, RtsCamera camera, TerrainMesh terrain,
                       List<PlacedBuilding> placed, List<SupplyPile> piles,
                       List<Unit> units,
                       PlacedBuilding ghost, boolean ghostValid,
                       NavDebugMesh navDebug, List<Vector3f> debugPath) {
        glViewport(0, 0, window.framebufferWidth(), window.framebufferHeight());
        glClearColor(0.50f, 0.65f, 0.82f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        terrainShader.bind();
        terrainShader.setMat4("uProj", camera.projection());
        terrainShader.setMat4("uView", camera.view());
        terrainShader.setMat4("uModel", model.identity());
        terrainShader.setVec3("uLightDir", lightDir);
        terrainShader.setVec3("uAmbient", ambient);
        terrainShader.setFloat("uMaxHeight", terrain.maxHeight());
        terrain.render();
        terrainShader.unbind();

        buildingShader.bind();
        buildingShader.setMat4("uProj", camera.projection());
        buildingShader.setMat4("uView", camera.view());
        buildingShader.setVec3("uLightDir", lightDir);
        buildingShader.setVec3("uAmbient", ambient);

        buildingShader.setVec4("uTint",
                BUILDING_TINT[0], BUILDING_TINT[1], BUILDING_TINT[2], BUILDING_TINT[3]);
        for (PlacedBuilding p : placed) {
            Mesh mesh = buildingMeshes.get(p.name());
            if (mesh == null) continue;
            model.identity().translate(p.cx(), p.cy(), p.cz());
            buildingShader.setMat4("uModel", model);
            mesh.render();
        }

        if (piles != null && !piles.isEmpty()) {
            buildingShader.setVec4("uTint",
                    SUPPLY_TINT[0], SUPPLY_TINT[1], SUPPLY_TINT[2], SUPPLY_TINT[3]);
            for (SupplyPile p : piles) {
                model.identity()
                        .translate(p.cx(), p.cy() + PILE_HALF, p.cz())
                        .scale(PILE_HALF);
                buildingShader.setMat4("uModel", model);
                cubeMesh.render();
            }
        }

        if (units != null && !units.isEmpty()) {
            for (Unit u : units) {
                buildingShader.setVec4("uTint", u.type.r, u.type.g, u.type.b, 1f);
                float halfH = u.type.height * 0.5f;
                model.identity()
                        .translate(u.pos.x, u.pos.y + halfH, u.pos.z)
                        .rotateY(u.heading)
                        .scale(u.type.radius, halfH, u.type.radius);
                buildingShader.setMat4("uModel", model);
                cubeMesh.render();
            }
            for (Unit u : units) {
                if (!u.selected) continue;
                float[] ringColor = selectionRingColor(u.state);
                buildingShader.setVec4("uTint", ringColor[0], ringColor[1], ringColor[2], 1f);
                float r = u.type.radius * 1.6f;
                model.identity()
                        .translate(u.pos.x, u.pos.y + 0.08f, u.pos.z)
                        .scale(r, 0.06f, r);
                buildingShader.setMat4("uModel", model);
                cubeMesh.render();
            }
        }

        if (ghost != null) {
            Mesh mesh = buildingMeshes.get(ghost.name());
            if (mesh != null) {
                float[] tint = ghostValid ? GHOST_VALID : GHOST_INVALID;
                buildingShader.setVec4("uTint", tint[0], tint[1], tint[2], tint[3]);
                model.identity().translate(ghost.cx(), ghost.cy(), ghost.cz());
                buildingShader.setMat4("uModel", model);

                glEnable(GL_BLEND);
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                glDepthMask(false);
                mesh.render();
                glDepthMask(true);
                glDisable(GL_BLEND);
            }
        }

        if (navDebug != null) {
            buildingShader.setVec4("uTint", 0.20f, 0.95f, 0.30f, 0.55f);
            model.identity();
            buildingShader.setMat4("uModel", model);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glDepthMask(false);
            navDebug.render();
            glDepthMask(true);
            glDisable(GL_BLEND);
        }

        if (debugPath != null && !debugPath.isEmpty()) {
            buildingShader.setVec4("uTint", 1.00f, 0.90f, 0.10f, 1f);
            for (Vector3f wp : debugPath) {
                model.identity()
                        .translate(wp.x, wp.y + 0.6f, wp.z)
                        .scale(0.35f, 0.6f, 0.35f);
                buildingShader.setMat4("uModel", model);
                cubeMesh.render();
            }
        }

        buildingShader.unbind();
    }

    public Vector3f pickTerrain(double cursorX, double cursorY,
                                Window window, RtsCamera camera, PhysicsWorld physics,
                                boolean enabled) {
        if (!buildCursorRay(cursorX, cursorY, window, camera, enabled)) return null;

        return physics.raycast(
                rayOrigin.x, rayOrigin.y, rayOrigin.z,
                rayDir.x, rayDir.y, rayDir.z,
                3000f, pickResult);
    }

    public Unit pickUnit(double cursorX, double cursorY,
                         Window window, RtsCamera camera,
                         UnitManager unitMgr, boolean enabled) {
        if (!buildCursorRay(cursorX, cursorY, window, camera, enabled)) return null;
        return unitMgr.pickUnit(rayOrigin, rayDir, 3000f);
    }

    private final Matrix4f tmpVP = new Matrix4f();
    private final Vector4f tmpClip = new Vector4f();

    public boolean projectToScreen(float wx, float wy, float wz,
                                   RtsCamera camera, Window window,
                                   float[] outXY) {
        tmpVP.set(camera.projection()).mul(camera.view());
        tmpClip.set(wx, wy, wz, 1f);
        tmpVP.transform(tmpClip);
        if (tmpClip.w <= 0.0001f) return false;
        float ndcX = tmpClip.x / tmpClip.w;
        float ndcY = tmpClip.y / tmpClip.w;
        outXY[0] = (ndcX * 0.5f + 0.5f) * window.width();
        outXY[1] = (1f - (ndcY * 0.5f + 0.5f)) * window.height();
        return ndcX >= -1f && ndcX <= 1f && ndcY >= -1f && ndcY <= 1f;
    }

    public int pickBuilding(double cursorX, double cursorY,
                            Window window, RtsCamera camera,
                            List<PlacedBuilding> placed, float selectionHeight,
                            boolean enabled) {
        if (!buildCursorRay(cursorX, cursorY, window, camera, enabled)) return -1;

        int bestIdx = -1;
        float bestT = Float.MAX_VALUE;
        for (int i = 0; i < placed.size(); i++) {
            PlacedBuilding p = placed.get(i);
            float h = p.halfSize();
            float t = intersectAABB(
                    rayOrigin.x, rayOrigin.y, rayOrigin.z,
                    rayDir.x, rayDir.y, rayDir.z,
                    p.cx() - h, p.cy(), p.cz() - h,
                    p.cx() + h, p.cy() + selectionHeight, p.cz() + h);
            if (t >= 0 && t < bestT) {
                bestT = t;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private boolean buildCursorRay(double cursorX, double cursorY,
                                   Window window, RtsCamera camera, boolean enabled) {
        if (!enabled) return false;

        int fbw = window.framebufferWidth();
        int fbh = window.framebufferHeight();
        double scaleX = (double) fbw / Math.max(1, window.width());
        double scaleY = (double) fbh / Math.max(1, window.height());
        float px = (float) (cursorX * scaleX);
        float py = (float) (fbh - cursorY * scaleY);

        if (px < 0 || px > fbw || py < 0 || py > fbh) return false;

        Matrix4f viewProj = new Matrix4f(camera.projection()).mul(camera.view());
        int[] viewport = {0, 0, fbw, fbh};
        viewProj.unprojectRay(px, py, viewport, rayOrigin, rayDir);
        return true;
    }

    private static final float[] RING_IDLE     = { 0.25f, 0.95f, 1.00f };
    private static final float[] RING_MOVING   = { 0.30f, 1.00f, 0.40f };
    private static final float[] RING_HARVEST  = { 1.00f, 0.85f, 0.20f };
    private static final float[] RING_DEPOSIT  = { 1.00f, 1.00f, 0.50f };

    private static float[] selectionRingColor(Unit.State s) {
        return switch (s) {
            case MOVING, MOVING_TO_PILE, MOVING_TO_BASE -> RING_MOVING;
            case HARVESTING -> RING_HARVEST;
            case DEPOSITING -> RING_DEPOSIT;
            default -> RING_IDLE;
        };
    }

    private static float intersectAABB(float ox, float oy, float oz,
                                       float dx, float dy, float dz,
                                       float minX, float minY, float minZ,
                                       float maxX, float maxY, float maxZ) {
        float invDx = 1f / dx, invDy = 1f / dy, invDz = 1f / dz;
        float tx1 = (minX - ox) * invDx, tx2 = (maxX - ox) * invDx;
        float ty1 = (minY - oy) * invDy, ty2 = (maxY - oy) * invDy;
        float tz1 = (minZ - oz) * invDz, tz2 = (maxZ - oz) * invDz;
        float tmin = Math.max(Math.max(Math.min(tx1, tx2), Math.min(ty1, ty2)), Math.min(tz1, tz2));
        float tmax = Math.min(Math.min(Math.max(tx1, tx2), Math.max(ty1, ty2)), Math.max(tz1, tz2));
        if (tmax < 0 || tmax < tmin) return -1f;
        return tmin > 0 ? tmin : tmax;
    }

    public void dispose() {
        Set<Mesh> unique = new HashSet<>(buildingMeshes.values());
        for (Mesh m : unique) m.dispose();
        cubeMesh.dispose();
        buildingShader.dispose();
        terrainShader.dispose();
    }
}
