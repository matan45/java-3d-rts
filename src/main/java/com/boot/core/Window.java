package com.boot.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window {

    private final long handle;
    private int width;
    private int height;
    private int framebufferWidth;
    private int framebufferHeight;

    public Window(String title, int width, int height) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            glfwTerminate();
            throw new RuntimeException("Failed to create GLFW window");
        }

        this.width = width;
        this.height = height;
        this.framebufferWidth = width;
        this.framebufferHeight = height;

        glfwSetFramebufferSizeCallback(handle, (w, fbw, fbh) -> {
            framebufferWidth = fbw;
            framebufferHeight = fbh;
        });
        glfwSetWindowSizeCallback(handle, (w, nw, nh) -> {
            this.width = nw;
            this.height = nh;
        });

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        glfwShowWindow(handle);

        GL.createCapabilities();
    }

    public long handle() { return handle; }
    public int width() { return width; }
    public int height() { return height; }
    public int framebufferWidth() { return framebufferWidth; }
    public int framebufferHeight() { return framebufferHeight; }
    public float aspect() {
        return framebufferHeight == 0 ? 1f : (float) framebufferWidth / framebufferHeight;
    }

    public boolean shouldClose() { return glfwWindowShouldClose(handle); }

    public void requestClose() { glfwSetWindowShouldClose(handle, true); }

    public void swapAndPoll() {
        glfwSwapBuffers(handle);
        glfwPollEvents();
    }

    public boolean isFocused() {
        return glfwGetWindowAttrib(handle, GLFW_FOCUSED) == GLFW_TRUE;
    }

    public void dispose() {
        glfwDestroyWindow(handle);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
