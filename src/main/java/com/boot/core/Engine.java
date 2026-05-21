package com.boot.core;

import com.boot.physics.PhysicsWorld;
import com.boot.physics.TerrainCollider;
import com.boot.render.Renderer;
import com.boot.render.TerrainMesh;
import com.boot.ui.Hud;
import com.boot.ui.Minimap;
import com.boot.world.Heightmap;
import com.boot.world.RtsCamera;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public final class Engine {

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

        heightmap = new Heightmap(256, 1.0f, 30f, 0xC0FFEEL);
        terrainMesh = new TerrainMesh(heightmap);

        physics = new PhysicsWorld();
        terrainCollider = new TerrainCollider(physics, heightmap);
        physics.step(0.001f);

        camera = new RtsCamera();
        camera.target().set(heightmap.worldSize() * 0.5f, 0f, heightmap.worldSize() * 0.5f);

        renderer = new Renderer();
        hud = new Hud();
        hud.init(window.handle());
        minimap = new Minimap(heightmap);
        hud.attachMinimap(minimap);

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

            renderer.render(window, camera, terrainMesh);

            Vector3f hover = renderer.pickTerrain(
                    input.cursorX(), input.cursorY(),
                    window, camera, physics,
                    input.gameWantsMouse());

            hud.frame(dt, window, camera, hover);

            input.endFrame();
            window.swapAndPoll();
        }
    }

    private void dispose() {
        if (hud != null) hud.dispose();
        if (renderer != null) renderer.dispose();
        if (terrainMesh != null) terrainMesh.dispose();
        if (terrainCollider != null) terrainCollider.dispose();
        if (physics != null) physics.dispose();
        if (input != null) input.dispose();
        if (window != null) window.dispose();
    }
}
