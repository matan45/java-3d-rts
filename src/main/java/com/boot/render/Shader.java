package com.boot.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

public final class Shader {

    private final int program;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public Shader(String vertResource, String fragResource) {
        int vs = compile(GL_VERTEX_SHADER, loadResource(vertResource), vertResource);
        int fs = compile(GL_FRAGMENT_SHADER, loadResource(fragResource), fragResource);
        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RuntimeException("Program link failed: " + log);
        }
        glDetachShader(program, vs);
        glDetachShader(program, fs);
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String src, String name) {
        int sh = glCreateShader(type);
        glShaderSource(sh, src);
        glCompileShader(sh);
        if (glGetShaderi(sh, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(sh);
            glDeleteShader(sh);
            throw new RuntimeException("Shader compile failed (" + name + "): " + log);
        }
        return sh;
    }

    private static String loadResource(String path) {
        try (InputStream in = Shader.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Resource not found: " + path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    public void bind() { glUseProgram(program); }

    public void unbind() { glUseProgram(0); }

    private int loc(String name) {
        return uniforms.computeIfAbsent(name, n -> glGetUniformLocation(program, n));
    }

    public void setMat4(String name, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            glUniformMatrix4fv(loc(name), false, m.get(stack.mallocFloat(16)));
        }
    }

    public void setVec3(String name, Vector3f v) {
        glUniform3f(loc(name), v.x, v.y, v.z);
    }

    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(loc(name), x, y, z);
    }

    public void setFloat(String name, float v) {
        glUniform1f(loc(name), v);
    }

    public void setVec4(String name, float x, float y, float z, float w) {
        glUniform4f(loc(name), x, y, z, w);
    }

    public void dispose() {
        glDeleteProgram(program);
    }
}
