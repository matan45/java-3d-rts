package com.boot.ui;

import com.boot.core.Window;
import com.boot.world.RtsCamera;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.joml.Vector3f;

public final class Hud {

    private final ImGuiImplGlfw implGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 implGl3 = new ImGuiImplGl3();

    private final HudState state = new HudState();
    private Minimap minimap;

    private double frameMsEma = 16.0;

    private static final float TOP_BAR_HEIGHT = 32f;
    private static final float BOTTOM_BAR_HEIGHT = 190f;
    private static final float MINIMAP_SIZE = 170f;
    private static final float SELECTION_PANEL_WIDTH = 280f;

    private static final int PANEL_BASE_FLAGS =
            ImGuiWindowFlags.NoDecoration |
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoSavedSettings |
            ImGuiWindowFlags.NoBringToFrontOnFocus |
            ImGuiWindowFlags.NoNav;

    private static final int OVERLAY_FLAGS =
            ImGuiWindowFlags.NoDecoration |
            ImGuiWindowFlags.NoMove |
            ImGuiWindowFlags.NoSavedSettings |
            ImGuiWindowFlags.AlwaysAutoResize |
            ImGuiWindowFlags.NoFocusOnAppearing |
            ImGuiWindowFlags.NoNav |
            ImGuiWindowFlags.NoInputs;

    public void init(long windowHandle) {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        implGlfw.init(windowHandle, true);
        implGl3.init("#version 460");
    }

    public void attachMinimap(Minimap minimap) {
        this.minimap = minimap;
    }

    public HudState state() { return state; }

    public void frame(float dt, Window window, RtsCamera camera, Vector3f hover) {
        state.tick(dt);

        double frameMs = dt * 1000.0;
        frameMsEma += 0.1 * (frameMs - frameMsEma);
        double fps = frameMsEma > 0 ? 1000.0 / frameMsEma : 0;

        implGl3.newFrame();
        implGlfw.newFrame();
        ImGui.newFrame();

        float winW = window.width();
        float winH = window.height();

        drawTopBar(winW);
        drawBottomBar(winW, winH, camera);
        drawDebugOverlay(winW, fps, camera, hover);

        ImGui.render();
        implGl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawTopBar(float winW) {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(winW, TOP_BAR_HEIGHT, ImGuiCond.Always);
        ImGui.setNextWindowBgAlpha(0.85f);

        if (ImGui.begin("##topbar", PANEL_BASE_FLAGS)) {
            resourceCell(0xFFE9C46A, "Gold",   state.gold);
            ImGui.sameLine();
            resourceCell(0xFF94A66B, "Wood",   state.wood);
            ImGui.sameLine();
            resourceCell(0xFFE76F51, "Food",   state.food);
            ImGui.sameLine();
            resourceCell(0xFFADADAD, "Stone",  state.stone);

            ImGui.sameLine();
            ImGui.dummy(20, 0);
            ImGui.sameLine();
            int popColor = state.population >= state.populationCap ? 0xFFFF5555 : 0xFFD4D4D4;
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, popColor);
            ImGui.text(String.format("Pop  %d / %d", state.population, state.populationCap));
            ImGui.popStyleColor();

            String timeText = "Time " + state.formattedGameTime();
            ImVec2 sz = ImGui.calcTextSize(timeText);
            ImGui.sameLine(winW - sz.x - 16f);
            ImGui.text(timeText);
        }
        ImGui.end();
    }

    private void resourceCell(int color, String label, int value) {
        ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, color);
        ImGui.text(label);
        ImGui.popStyleColor();
        ImGui.sameLine();
        ImGui.text(String.valueOf(value));
    }

    private void drawBottomBar(float winW, float winH, RtsCamera camera) {
        float y = winH - BOTTOM_BAR_HEIGHT;
        ImGui.setNextWindowPos(0, y, ImGuiCond.Always);
        ImGui.setNextWindowSize(winW, BOTTOM_BAR_HEIGHT, ImGuiCond.Always);
        ImGui.setNextWindowBgAlpha(0.92f);

        if (ImGui.begin("##bottombar", PANEL_BASE_FLAGS)) {
            drawMinimapPanel(camera);
            ImGui.sameLine();
            drawCommandGrid(winW);
            ImGui.sameLine();
            drawSelectionPanel();
        }
        ImGui.end();
    }

    private void drawMinimapPanel(RtsCamera camera) {
        ImGui.beginChild("##minimap", MINIMAP_SIZE + 8, BOTTOM_BAR_HEIGHT - 16, false);
        if (minimap != null) {
            ImVec2 cursor = ImGui.getCursorScreenPos();
            ImGui.image(minimap.textureId(), MINIMAP_SIZE, MINIMAP_SIZE);

            ImDrawList dl = ImGui.getWindowDrawList();
            float ws = minimap.worldSize();
            float cx = cursor.x + (camera.target().x / ws) * MINIMAP_SIZE;
            float cy = cursor.y + (camera.target().z / ws) * MINIMAP_SIZE;

            float viewExtent = Math.min(MINIMAP_SIZE * 0.5f, (camera.distance() / ws) * MINIMAP_SIZE * 0.9f);
            dl.addRect(cx - viewExtent, cy - viewExtent, cx + viewExtent, cy + viewExtent,
                    0xFFFFFFFF, 0f, 0, 1.5f);

            dl.addCircleFilled(cx, cy, 3f, 0xFFFFFFFF);

            dl.addRect(cursor.x, cursor.y, cursor.x + MINIMAP_SIZE, cursor.y + MINIMAP_SIZE,
                    0xFF202020, 0f, 0, 1.5f);
        } else {
            ImGui.textDisabled("(no minimap)");
        }
        ImGui.endChild();
    }

    private void drawCommandGrid(float winW) {
        float available = winW - MINIMAP_SIZE - SELECTION_PANEL_WIDTH - 60;
        ImGui.beginChild("##commands", available, BOTTOM_BAR_HEIGHT - 16, false);

        ImGui.textDisabled("Commands");
        ImGui.separator();

        String[][] commands = {
                {"Move",   "Attack", "Stop",   "Hold"},
                {"Patrol", "Build",  "Repair", "Gather"},
                {"Train",  "Cancel", "",       ""},
        };
        float bw = 64f;
        float bh = 36f;
        for (String[] row : commands) {
            for (int i = 0; i < row.length; i++) {
                String label = row[i];
                if (label.isEmpty()) {
                    ImGui.dummy(bw, bh);
                } else {
                    ImGui.button(label, bw, bh);
                }
                if (i < row.length - 1) ImGui.sameLine();
            }
        }
        ImGui.endChild();
    }

    private void drawSelectionPanel() {
        ImGui.beginChild("##selection", SELECTION_PANEL_WIDTH, BOTTOM_BAR_HEIGHT - 16, false);
        ImGui.textDisabled("Selection");
        ImGui.separator();

        if (state.hasSelection()) {
            ImGui.text(state.selectionName);
            ImGui.text(String.format("HP  %d / %d", state.selectionHp, state.selectionMaxHp));
            float frac = state.selectionMaxHp > 0
                    ? (float) state.selectionHp / state.selectionMaxHp : 0f;
            ImGui.progressBar(frac, 0, 14, "");
        } else {
            ImGui.dummy(0, 10);
            ImGui.textDisabled("Nothing selected");
            ImGui.dummy(0, 6);
            ImGui.textDisabled("Left-click a unit");
            ImGui.textDisabled("or drag a box to select");
        }
        ImGui.endChild();
    }

    private void drawDebugOverlay(float winW, double fps, RtsCamera camera, Vector3f hover) {
        ImGui.setNextWindowPos(winW - 10, TOP_BAR_HEIGHT + 10, ImGuiCond.Always, 1f, 0f);
        ImGui.setNextWindowBgAlpha(0.35f);
        if (ImGui.begin("##debug", OVERLAY_FLAGS)) {
            ImGui.text(String.format("FPS  %.0f   (%.2f ms)", fps, frameMsEma));
            Vector3f t = camera.target();
            ImGui.text(String.format("Cam  %.0f, %.0f, %.0f", t.x, t.y, t.z));
            ImGui.text(String.format("Dist %.0f   Yaw %.0f", camera.distance(), Math.toDegrees(camera.yaw())));
            if (hover != null) {
                ImGui.text(String.format("Hover %.0f, %.0f, %.0f", hover.x, hover.z, hover.y));
            } else {
                ImGui.text("Hover --");
            }
        }
        ImGui.end();
    }

    public void dispose() {
        if (minimap != null) minimap.dispose();
        implGl3.shutdown();
        implGlfw.shutdown();
        ImGui.destroyContext();
    }
}
