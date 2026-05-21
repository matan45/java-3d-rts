package com.boot.ui;

import com.boot.world.RtsCamera;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.joml.Vector3f;

public final class Hud {

    private final ImGuiImplGlfw implGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 implGl3 = new ImGuiImplGl3();

    private double frameMsEma = 16.0;
    private static final int HUD_FLAGS =
            ImGuiWindowFlags.NoDecoration |
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoSavedSettings |
            ImGuiWindowFlags.AlwaysAutoResize |
            ImGuiWindowFlags.NoFocusOnAppearing |
            ImGuiWindowFlags.NoNav;

    public void init(long windowHandle) {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        implGlfw.init(windowHandle, true);
        implGl3.init("#version 460");
    }

    public void frame(float dt, RtsCamera camera, Vector3f hover) {
        double frameMs = dt * 1000.0;
        frameMsEma += 0.1 * (frameMs - frameMsEma);
        double fps = frameMsEma > 0 ? 1000.0 / frameMsEma : 0;

        implGl3.newFrame();
        implGlfw.newFrame();
        ImGui.newFrame();

        ImGui.setNextWindowPos(10, 10, ImGuiCond.Always);
        ImGui.setNextWindowBgAlpha(0.35f);
        if (ImGui.begin("HUD", HUD_FLAGS)) {
            ImGui.text(String.format("FPS  %.0f   (%.2f ms)", fps, frameMsEma));
            ImGui.separator();
            Vector3f t = camera.target();
            ImGui.text(String.format("Cam  x=%.1f  y=%.1f  z=%.1f", t.x, t.y, t.z));
            ImGui.text(String.format("Dist %.1f   Yaw %.0f°   Pitch %.0f°",
                    camera.distance(),
                    Math.toDegrees(camera.yaw()),
                    Math.toDegrees(camera.pitch())));
            ImGui.separator();
            if (hover != null) {
                ImGui.text(String.format("Hover  x=%.1f  z=%.1f  h=%.1f",
                        hover.x, hover.z, hover.y));
            } else {
                ImGui.text("Hover  --");
            }
            ImGui.separator();
            ImGui.textDisabled("WASD pan | edge pan | wheel zoom | MMB rotate | ESC exit");
        }
        ImGui.end();

        ImGui.render();
        implGl3.renderDrawData(ImGui.getDrawData());
    }

    public void dispose() {
        implGl3.shutdown();
        implGlfw.shutdown();
        ImGui.destroyContext();
    }
}
