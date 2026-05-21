package com.boot.render;

import com.boot.core.Window;
import com.boot.world.Heightmap;
import com.boot.world.RtsCamera;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public final class Renderer {

    private final Shader terrainShader;
    private final Matrix4f model = new Matrix4f();
    private final Vector3f lightDir = new Vector3f(-0.45f, -0.85f, -0.30f).normalize().negate();
    private final Vector3f ambient = new Vector3f(0.30f, 0.32f, 0.38f);

    private final Vector3f rayOrigin = new Vector3f();
    private final Vector3f rayDir = new Vector3f();
    private final Vector3f pickResult = new Vector3f();

    public Renderer() {
        terrainShader = new Shader("/shaders/terrain.vert", "/shaders/terrain.frag");
    }

    public void render(Window window, RtsCamera camera, TerrainMesh terrain) {
        glViewport(0, 0, window.framebufferWidth(), window.framebufferHeight());
        glClearColor(0.50f, 0.65f, 0.82f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        camera.updateMatrices(window.aspect());

        terrainShader.bind();
        terrainShader.setMat4("uProj", camera.projection());
        terrainShader.setMat4("uView", camera.view());
        terrainShader.setMat4("uModel", model.identity());
        terrainShader.setVec3("uLightDir", lightDir);
        terrainShader.setVec3("uAmbient", ambient);
        terrain.render();
        terrainShader.unbind();
    }

    public Vector3f pickTerrain(double cursorX, double cursorY,
                                Window window, RtsCamera camera, Heightmap heightmap,
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

        return marchTerrain(heightmap);
    }

    private Vector3f marchTerrain(Heightmap hm) {
        float maxDist = 2000f;
        float step = 0.5f;
        float refineSteps = 8;

        float t = 0f;
        float prevT = 0f;
        float prevDelta = sampleDelta(hm, prevT);

        while (t < maxDist) {
            t += step;
            float delta = sampleDelta(hm, t);

            if (delta <= 0f && prevDelta > 0f) {
                float lo = prevT, hi = t;
                for (int i = 0; i < refineSteps; i++) {
                    float mid = (lo + hi) * 0.5f;
                    if (sampleDelta(hm, mid) <= 0f) hi = mid; else lo = mid;
                }
                float tHit = (lo + hi) * 0.5f;
                pickResult.set(rayDir).mul(tHit).add(rayOrigin);
                pickResult.y = hm.heightAt(pickResult.x, pickResult.z);
                return pickResult;
            }

            prevT = t;
            prevDelta = delta;

            if (t > 50f) step = Math.min(step * 1.05f, 4f);
        }
        return null;
    }

    private float sampleDelta(Heightmap hm, float t) {
        float x = rayOrigin.x + rayDir.x * t;
        float y = rayOrigin.y + rayDir.y * t;
        float z = rayOrigin.z + rayDir.z * t;
        return y - hm.heightAt(x, z);
    }

    public void dispose() {
        terrainShader.dispose();
    }
}
