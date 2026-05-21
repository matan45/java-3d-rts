package com.boot.core;

import imgui.ImGui;
import imgui.ImGuiIO;

import static org.lwjgl.glfw.GLFW.*;

public final class Input {

    private final long window;

    private final boolean[] keys = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] mouseDown = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] mousePressedEdge = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private double cursorX, cursorY;
    private double lastCursorX, lastCursorY;
    private double cursorDx, cursorDy;
    private double scrollAccum;

    private boolean gameWantsMouse = true;
    private boolean gameWantsKeyboard = true;

    public Input(Window w) {
        this.window = w.handle();

        glfwSetKeyCallback(window, (win, key, sc, action, mods) -> {
            if (key < 0 || key >= keys.length) return;
            if (action == GLFW_PRESS) keys[key] = true;
            else if (action == GLFW_RELEASE) keys[key] = false;
        });

        glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button < 0 || button >= mouseDown.length) return;
            if (action == GLFW_PRESS) {
                if (!mouseDown[button]) mousePressedEdge[button] = true;
                mouseDown[button] = true;
            } else if (action == GLFW_RELEASE) {
                mouseDown[button] = false;
            }
        });

        glfwSetCursorPosCallback(window, (win, x, y) -> {
            cursorX = x;
            cursorY = y;
        });

        glfwSetScrollCallback(window, (win, xoff, yoff) -> scrollAccum += yoff);

        try (var stack = org.lwjgl.system.MemoryStack.stackPush()) {
            var px = stack.mallocDouble(1);
            var py = stack.mallocDouble(1);
            glfwGetCursorPos(window, px, py);
            cursorX = lastCursorX = px.get(0);
            cursorY = lastCursorY = py.get(0);
        }
    }

    public void beginFrame() {
        cursorDx = cursorX - lastCursorX;
        cursorDy = cursorY - lastCursorY;
        lastCursorX = cursorX;
        lastCursorY = cursorY;

        if (ImGui.getCurrentContext() != null) {
            ImGuiIO io = ImGui.getIO();
            gameWantsMouse = !io.getWantCaptureMouse();
            gameWantsKeyboard = !io.getWantCaptureKeyboard();
        } else {
            gameWantsMouse = true;
            gameWantsKeyboard = true;
        }
    }

    public void endFrame() {
        scrollAccum = 0;
        for (int i = 0; i < mousePressedEdge.length; i++) mousePressedEdge[i] = false;
    }

    public boolean isKeyDown(int key) {
        if (key < 0 || key >= keys.length) return false;
        return keys[key] && gameWantsKeyboard;
    }

    public boolean isKeyPressed(int key) {
        if (key < 0 || key >= keys.length) return false;
        return keys[key];
    }

    public boolean isMouseDown(int button) {
        if (button < 0 || button >= mouseDown.length) return false;
        return mouseDown[button] && gameWantsMouse;
    }

    public boolean isMousePressed(int button) {
        if (button < 0 || button >= mousePressedEdge.length) return false;
        return mousePressedEdge[button] && gameWantsMouse;
    }

    public double cursorX() { return cursorX; }
    public double cursorY() { return cursorY; }
    public double cursorDx() { return cursorDx; }
    public double cursorDy() { return cursorDy; }

    public double consumeScroll() {
        if (!gameWantsMouse) { scrollAccum = 0; return 0; }
        double v = scrollAccum;
        scrollAccum = 0;
        return v;
    }

    public boolean gameWantsMouse() { return gameWantsMouse; }
    public boolean gameWantsKeyboard() { return gameWantsKeyboard; }

    public void dispose() {
    }
}
