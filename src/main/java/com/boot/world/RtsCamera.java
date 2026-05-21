package com.boot.world;

import com.boot.core.Input;
import com.boot.core.Window;
import com.boot.ui.UiLayout;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public final class RtsCamera {

    private static final Vector3f UP = new Vector3f(0, 1, 0);

    private final Vector3f target = new Vector3f();
    private float yaw = 0f;
    private float pitch = (float) Math.toRadians(-55f);
    private float distance = 80f;

    private final float minDistance = 10f;
    private final float maxDistance = 350f;
    private final float panSpeed = 40f;
    private final float fastMultiplier = 3f;
    private final float rotateSensitivity = 0.005f;
    private final float zoomFactor = 1.15f;
    private final int edgePanPx = 8;
    private final float edgePanSpeedMul = 1.0f;

    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();
    private final Vector3f eye = new Vector3f();

    public Vector3f target() { return target; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public float distance() { return distance; }

    public Matrix4f view() { return view; }
    public Matrix4f projection() { return proj; }

    public void update(Input input, Heightmap heightmap, Window window, float dt) {
        boolean fast = input.isKeyDown(GLFW_KEY_LEFT_SHIFT) || input.isKeyDown(GLFW_KEY_RIGHT_SHIFT);
        float speed = panSpeed * (fast ? fastMultiplier : 1f) * (distance / 80f);

        float forwardX = (float) Math.sin(yaw);
        float forwardZ = (float) Math.cos(yaw);
        float rightX = -(float) Math.cos(yaw);
        float rightZ = (float) Math.sin(yaw);

        float mvX = 0, mvZ = 0;
        if (input.isKeyDown(GLFW_KEY_W) || input.isKeyDown(GLFW_KEY_UP))    { mvX += forwardX; mvZ += forwardZ; }
        if (input.isKeyDown(GLFW_KEY_S) || input.isKeyDown(GLFW_KEY_DOWN))  { mvX -= forwardX; mvZ -= forwardZ; }
        if (input.isKeyDown(GLFW_KEY_D) || input.isKeyDown(GLFW_KEY_RIGHT)) { mvX += rightX;   mvZ += rightZ; }
        if (input.isKeyDown(GLFW_KEY_A) || input.isKeyDown(GLFW_KEY_LEFT))  { mvX -= rightX;   mvZ -= rightZ; }

        if (input.gameWantsMouse() && window.isFocused()) {
            double cx = input.cursorX();
            double cy = input.cursorY();
            int w = window.width();
            int h = window.height();
            float left = 0;
            float right = w - UiLayout.SIDEBAR_WIDTH;
            float top = UiLayout.TOP_BAR_HEIGHT;
            float bottom = h;
            if (cx >= left && cx < right && cy >= top && cy < bottom) {
                float epSpeed = edgePanSpeedMul;
                if (cx < left + edgePanPx)        { mvX -= rightX * epSpeed;   mvZ -= rightZ * epSpeed; }
                else if (cx > right - edgePanPx) { mvX += rightX * epSpeed;   mvZ += rightZ * epSpeed; }
                if (cy < top + edgePanPx)        { mvX += forwardX * epSpeed; mvZ += forwardZ * epSpeed; }
                else if (cy > bottom - edgePanPx) { mvX -= forwardX * epSpeed; mvZ -= forwardZ * epSpeed; }
            }
        }

        float mag = (float) Math.sqrt(mvX * mvX + mvZ * mvZ);
        if (mag > 0f) {
            float inv = speed * dt / mag;
            target.x += mvX * inv;
            target.z += mvZ * inv;
        }

        if (input.isMouseDown(GLFW_MOUSE_BUTTON_MIDDLE)) {
            yaw -= (float) input.cursorDx() * rotateSensitivity;
            pitch -= (float) input.cursorDy() * rotateSensitivity;
            float maxPitch = (float) Math.toRadians(-15f);
            float minPitch = (float) Math.toRadians(-85f);
            if (pitch > maxPitch) pitch = maxPitch;
            if (pitch < minPitch) pitch = minPitch;
        }

        double scroll = input.consumeScroll();
        if (scroll != 0) {
            distance *= (float) Math.pow(zoomFactor, -scroll);
            if (distance < minDistance) distance = minDistance;
            if (distance > maxDistance) distance = maxDistance;
        }

        float worldSize = heightmap.worldSize();
        if (target.x < 0) target.x = 0;
        if (target.z < 0) target.z = 0;
        if (target.x > worldSize) target.x = worldSize;
        if (target.z > worldSize) target.z = worldSize;
        target.y = heightmap.heightAt(target.x, target.z);
    }

    public void updateMatrices(float aspect) {
        float cp = (float) Math.cos(pitch);
        float sp = (float) Math.sin(pitch);
        eye.set(
                target.x - distance * cp * (float) Math.sin(yaw),
                target.y - distance * sp,
                target.z - distance * cp * (float) Math.cos(yaw)
        );
        view.identity().lookAt(eye, target, UP);
        proj.identity().setPerspective((float) Math.toRadians(60f), aspect, 0.1f, 1500f);
    }
}
