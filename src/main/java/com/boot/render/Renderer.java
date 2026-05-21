package com.boot.render;

import com.boot.core.Window;
import com.boot.physics.PhysicsWorld;
import com.boot.ui.HudState;
import com.boot.world.PlacedBuilding;
import com.boot.world.RtsCamera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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

    private final Matrix4f model = new Matrix4f();
    private final Vector3f lightDir = new Vector3f(-0.45f, -0.85f, -0.30f).normalize().negate();
    private final Vector3f ambient = new Vector3f(0.30f, 0.32f, 0.38f);

    private static final float[] BUILDING_TINT  = { 0.78f, 0.74f, 0.65f, 1.00f };
    private static final float[] GHOST_VALID    = { 0.30f, 1.00f, 0.40f, 0.45f };
    private static final float[] GHOST_INVALID  = { 1.00f, 0.30f, 0.30f, 0.45f };

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
    }

    public void render(Window window, RtsCamera camera, TerrainMesh terrain,
                       List<PlacedBuilding> placed,
                       PlacedBuilding ghost, boolean ghostValid) {
        glViewport(0, 0, window.framebufferWidth(), window.framebufferHeight());
        glClearColor(0.50f, 0.65f, 0.82f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        terrainShader.bind();
        terrainShader.setMat4("uProj", camera.projection());
        terrainShader.setMat4("uView", camera.view());
        terrainShader.setMat4("uModel", model.identity());
        terrainShader.setVec3("uLightDir", lightDir);
        terrainShader.setVec3("uAmbient", ambient);
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

        buildingShader.unbind();
    }

    public Vector3f pickTerrain(double cursorX, double cursorY,
                                Window window, RtsCamera camera, PhysicsWorld physics,
                                boolean enabled) {
        if (!enabled) return null;

        int fbw = window.framebufferWidth();
        int fbh = window.framebufferHeight();
        double scaleX = (double) fbw / Math.max(1, window.width());
        double scaleY = (double) fbh / Math.max(1, window.height());
        float px = (float) (cursorX * scaleX);
        float py = (float) (fbh - cursorY * scaleY);

        if (px < 0 || px > fbw || py < 0 || py > fbh) return null;

        Matrix4f viewProj = new Matrix4f(camera.projection()).mul(camera.view());
        int[] viewport = {0, 0, fbw, fbh};
        viewProj.unprojectRay(px, py, viewport, rayOrigin, rayDir);

        return physics.raycast(
                rayOrigin.x, rayOrigin.y, rayOrigin.z,
                rayDir.x, rayDir.y, rayDir.z,
                3000f, pickResult);
    }

    public void dispose() {
        Set<Mesh> unique = new HashSet<>(buildingMeshes.values());
        for (Mesh m : unique) m.dispose();
        buildingShader.dispose();
        terrainShader.dispose();
    }
}
