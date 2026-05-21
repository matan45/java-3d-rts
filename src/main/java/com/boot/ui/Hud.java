package com.boot.ui;

import com.boot.core.Window;
import com.boot.economy.BuildingEconomy;
import com.boot.world.RtsCamera;
import com.boot.world.SupplyPile;
import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
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

    private static final int PANEL_FLAGS =
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

    private static final int COL_BG          = 0xF01A1F26;
    private static final int COL_BG_DARK     = 0xF010141A;
    private static final int COL_ACCENT      = 0xFF3DA9FC;
    private static final int COL_CASH        = 0xFFFFD466;
    private static final int COL_POWER_OK    = 0xFF66E07A;
    private static final int COL_POWER_LOW   = 0xFFFF5566;
    private static final int COL_TAB_OFF     = 0xFF2A323D;
    private static final int COL_TAB_ON      = 0xFF3DA9FC;
    private static final int COL_BORDER      = 0xFF3A4250;
    private static final int COL_TEXT_DIM    = 0xFF8FA1B3;

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
        drawSidebar(winW, winH, camera);
        if (state.hasSelection()) drawCommandBar(winW, winH);
        drawDebugOverlay(winW, winH, fps, camera, hover);

        ImGui.render();
        implGl3.renderDrawData(ImGui.getDrawData());
    }

    private void drawTopBar(float winW) {
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(winW, UiLayout.TOP_BAR_HEIGHT, ImGuiCond.Always);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COL_BG_DARK);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 14f, 6f);

        if (ImGui.begin("##topbar", PANEL_FLAGS)) {
            ImGui.pushStyleColor(ImGuiCol.Text, COL_CASH);
            ImGui.text("$");
            ImGui.popStyleColor();
            ImGui.sameLine();
            ImGui.text(String.format("%,d", state.cash));

            if (state.cashPerSecond != 0) {
                ImGui.sameLine();
                ImGui.pushStyleColor(ImGuiCol.Text, COL_TEXT_DIM);
                ImGui.text(String.format("(+$%d/s)", state.cashPerSecond));
                ImGui.popStyleColor();
            }

            ImGui.sameLine();
            ImGui.dummy(22, 0);
            ImGui.sameLine();

            int powColor = state.lowPower() ? COL_POWER_LOW : COL_POWER_OK;
            ImGui.pushStyleColor(ImGuiCol.Text, powColor);
            ImGui.text("Power");
            ImGui.popStyleColor();
            ImGui.sameLine();
            ImGui.text(String.format("%d / %d", state.powerConsumed, state.powerProduced));

            ImGui.sameLine();
            ImGui.dummy(22, 0);
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, COL_TEXT_DIM);
            ImGui.text("USA  -  General  -  Skirmish");
            ImGui.popStyleColor();

            String timeText = state.formattedGameTime();
            ImVec2 sz = ImGui.calcTextSize(timeText);
            ImGui.sameLine(winW - sz.x - 18f);
            ImGui.text(timeText);
        }
        ImGui.end();
        ImGui.popStyleVar();
        ImGui.popStyleColor();
    }

    private void drawSidebar(float winW, float winH, RtsCamera camera) {
        float x = winW - UiLayout.SIDEBAR_WIDTH;
        float y = UiLayout.TOP_BAR_HEIGHT;
        float w = UiLayout.SIDEBAR_WIDTH;
        float h = winH - UiLayout.TOP_BAR_HEIGHT;

        ImGui.setNextWindowPos(x, y, ImGuiCond.Always);
        ImGui.setNextWindowSize(w, h, ImGuiCond.Always);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COL_BG);
        ImGui.pushStyleColor(ImGuiCol.Border, COL_BORDER);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 8f, 8f);
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 4f, 4f);

        if (ImGui.begin("##sidebar", PANEL_FLAGS | ImGuiWindowFlags.NoScrollbar)) {
            drawTabs();
            ImGui.separator();
            drawBuildGrid();
            ImGui.dummy(0, 6);
            drawGeneralPowers();
            ImGui.dummy(0, 6);
            drawMapCashReadout();
            drawMinimapPanel(camera);
        }
        ImGui.end();
        ImGui.popStyleVar(2);
        ImGui.popStyleColor(2);
    }

    private void drawTabs() {
        HudState.Tab[] tabs = HudState.Tab.values();
        String[] labels = { "STRUCT", "UNITS", "UPGRADE" };
        float avail = UiLayout.SIDEBAR_WIDTH - 16f - (tabs.length - 1) * 4f;
        float bw = avail / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = state.activeTab == tabs[i];
            ImGui.pushStyleColor(ImGuiCol.Button, active ? COL_TAB_ON : COL_TAB_OFF);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, active ? COL_TAB_ON : 0xFF3A4250);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, COL_ACCENT);
            if (ImGui.button(labels[i], bw, 26)) {
                state.activeTab = tabs[i];
            }
            ImGui.popStyleColor(3);
            if (i < tabs.length - 1) ImGui.sameLine();
        }
    }

    private void drawBuildGrid() {
        HudState.Tab tab = state.activeTab;
        String[] items = state.currentTabItems();

        if (items.length == 0 && tab != HudState.Tab.STRUCTURES) {
            ImGui.pushStyleColor(ImGuiCol.Text, COL_TEXT_DIM);
            ImGui.textWrapped(state.selectionName.isEmpty()
                    ? "(Select a building to train units or buy upgrades)"
                    : "(" + state.selectionName + " has nothing to produce)");
            ImGui.popStyleColor();
            return;
        }

        int cols = 2;
        float pad = 4f;
        float bw = (UiLayout.SIDEBAR_WIDTH - 16f - pad * (cols - 1)) / cols;
        float bh = 50f;

        for (int i = 0; i < items.length; i++) {
            String name = items[i];
            int cost = costForTab(tab, name);
            boolean affordable = cost == 0 || state.cash >= cost;

            int btnCol = affordable ? 0xFF2A323D : 0xFF1A1F25;
            int btnHoverCol = affordable ? 0xFF3A4555 : 0xFF22272E;
            ImGui.pushStyleColor(ImGuiCol.Button, btnCol);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, btnHoverCol);
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, COL_ACCENT);
            boolean clicked = ImGui.button("##b" + i, bw, bh);
            ImGui.popStyleColor(3);

            if (clicked && affordable) {
                switch (tab) {
                    case STRUCTURES -> state.pendingPlacementType = name;
                    case UNITS, UPGRADES -> state.cash -= cost;
                }
            }

            ImVec2 min = ImGui.getItemRectMin();
            ImVec2 max = ImGui.getItemRectMax();
            ImDrawList dl = ImGui.getWindowDrawList();
            ImVec2 ts = ImGui.calcTextSize(name);
            float tx = min.x + (max.x - min.x - ts.x) * 0.5f;
            float ty = min.y + (max.y - min.y - ts.y) * 0.5f - (cost > 0 ? 6f : 0f);
            int textCol = affordable ? 0xFFE8ECF1 : 0xFF6A727B;
            dl.addText(tx, ty, textCol, name);

            if (cost > 0) {
                String costText = "$" + cost;
                ImVec2 cs = ImGui.calcTextSize(costText);
                float cxText = min.x + (max.x - min.x - cs.x) * 0.5f;
                float cyText = max.y - cs.y - 4f;
                int costCol = affordable ? COL_CASH : COL_POWER_LOW;
                dl.addText(cxText, cyText, costCol, costText);
            }

            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text(name);
                if (cost > 0) {
                    ImGui.textDisabled("Cost: $" + cost);
                }
                int income = BuildingEconomy.income(name);
                if (income > 0) {
                    ImGui.textDisabled("Income: +$" + income + "/s");
                }
                ImGui.endTooltip();
            }

            if ((i + 1) % cols != 0) ImGui.sameLine(0f, pad);
        }
    }

    private static int costForTab(HudState.Tab tab, String name) {
        return switch (tab) {
            case STRUCTURES -> BuildingEconomy.cost(name);
            case UNITS -> BuildingEconomy.unitCost(name);
            case UPGRADES -> BuildingEconomy.upgradeCost(name);
        };
    }

    private void drawGeneralPowers() {
        ImGui.pushStyleColor(ImGuiCol.Text, COL_TEXT_DIM);
        ImGui.text("General Powers");
        ImGui.popStyleColor();

        String[] powers = { "A10", "Para", "Spy" };
        float bw = 56f;
        for (int i = 0; i < powers.length; i++) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0xFF402A1A);
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0xFF604030);
            ImGui.button(powers[i], bw, 36);
            ImGui.popStyleColor(2);
            if (i < powers.length - 1) ImGui.sameLine();
        }
    }

    private void drawMapCashReadout() {
        ImGui.pushStyleColor(ImGuiCol.Text, COL_TEXT_DIM);
        ImGui.text(String.format("Map: $%,d", state.mapCashAvailable));
        ImGui.popStyleColor();
    }

    private void drawMinimapPanel(RtsCamera camera) {
        if (minimap == null) {
            ImGui.textDisabled("(no minimap)");
            return;
        }
        float size = UiLayout.SIDEBAR_WIDTH - 16f;
        ImVec2 cursor = ImGui.getCursorScreenPos();

        ImDrawList dl = ImGui.getWindowDrawList();
        dl.addRectFilled(cursor.x - 2, cursor.y - 2, cursor.x + size + 2, cursor.y + size + 2, COL_BG_DARK);

        ImGui.image(minimap.textureId(), size, size);

        float ws = minimap.worldSize();

        for (SupplyPile p : state.supplyPilesView) {
            float dx = cursor.x + (p.cx() / ws) * size;
            float dz = cursor.y + (p.cz() / ws) * size;
            dl.addCircleFilled(dx, dz, 2.5f, 0xFF22CC44);
        }

        float cx = cursor.x + (camera.target().x / ws) * size;
        float cy = cursor.y + (camera.target().z / ws) * size;
        float viewExtent = Math.min(size * 0.5f, (camera.distance() / ws) * size * 0.9f);

        dl.addRect(cx - viewExtent, cy - viewExtent, cx + viewExtent, cy + viewExtent,
                COL_ACCENT, 0f, 0, 1.5f);
        dl.addCircleFilled(cx, cy, 3f, COL_ACCENT);
        dl.addRect(cursor.x, cursor.y, cursor.x + size, cursor.y + size, COL_BORDER, 0f, 0, 1.0f);
    }

    private void drawCommandBar(float winW, float winH) {
        float w = winW - UiLayout.SIDEBAR_WIDTH;
        float h = UiLayout.COMMAND_BAR_HEIGHT;
        float x = 0;
        float y = winH - h;

        ImGui.setNextWindowPos(x, y, ImGuiCond.Always);
        ImGui.setNextWindowSize(w, h, ImGuiCond.Always);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, COL_BG_DARK);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 10f, 6f);

        if (ImGui.begin("##commandbar", PANEL_FLAGS)) {
            ImGui.text(state.selectionName);
            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Text, COL_TEXT_DIM);
            ImGui.text("  " + state.selectionType);
            ImGui.popStyleColor();

            if (state.selectionMaxHp > 0) {
                ImGui.sameLine();
                float hpFrac = state.selectionHp / (float) state.selectionMaxHp;
                int hpColor = hpFrac > 0.5f ? COL_POWER_OK
                            : hpFrac > 0.25f ? COL_CASH
                            : COL_POWER_LOW;
                ImGui.pushStyleColor(ImGuiCol.Text, hpColor);
                ImGui.text(String.format("   HP %d/%d", state.selectionHp, state.selectionMaxHp));
                ImGui.popStyleColor();
            }

            ImGui.sameLine();
            ImGui.dummy(8, 0);
            ImGui.sameLine();
            drawVeterancyChevrons(state.selectionVeterancy);

            String[] cmds = "Structure".equals(state.selectionType)
                    ? new String[] { "Sell", "Repair", "Rally Point", "Power" }
                    : new String[] { "Move", "Attack", "Stop", "Guard", "Force Fire", "Garrison" };
            for (int i = 0; i < cmds.length; i++) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0xFF2A323D);
                ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0xFF3A4555);
                ImGui.button(cmds[i], 72, 36);
                ImGui.popStyleColor(2);
                if (i < cmds.length - 1) ImGui.sameLine();
            }
        }
        ImGui.end();
        ImGui.popStyleVar();
        ImGui.popStyleColor();
    }

    private void drawVeterancyChevrons(int vet) {
        ImDrawList dl = ImGui.getWindowDrawList();
        ImVec2 cursor = ImGui.getCursorScreenPos();
        float w = 8f, h = 7f, gap = 3f, yOff = 4f;
        for (int i = 0; i < 3; i++) {
            float x0 = cursor.x + i * (w + gap);
            float y0 = cursor.y + yOff;
            float ax = x0,        ay = y0 + h;
            float bx = x0 + w / 2, by = y0;
            float cx = x0 + w,    cy = y0 + h;
            if (i < vet) {
                dl.addTriangleFilled(ax, ay, bx, by, cx, cy, COL_CASH);
            } else {
                dl.addTriangle(ax, ay, bx, by, cx, cy, 0xFF555560, 1.2f);
            }
        }
        ImGui.dummy(3 * (w + gap), h + yOff);
    }

    private void drawDebugOverlay(float winW, float winH, double fps, RtsCamera camera, Vector3f hover) {
        float ox = winW - UiLayout.SIDEBAR_WIDTH - 10f;
        float oy = UiLayout.TOP_BAR_HEIGHT + 10f;
        ImGui.setNextWindowPos(ox, oy, ImGuiCond.Always, 1f, 0f);
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
