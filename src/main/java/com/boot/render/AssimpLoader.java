package com.boot.render;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.assimp.Assimp.*;

public final class AssimpLoader {

    private static final int FLAGS =
            aiProcess_Triangulate |
            aiProcess_JoinIdenticalVertices |
            aiProcess_PreTransformVertices |
            aiProcess_ImproveCacheLocality;

    private AssimpLoader() {}

    public static Mesh loadResource(String resourcePath) {
        byte[] bytes;
        try (InputStream in = AssimpLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new RuntimeException("Model resource not found: " + resourcePath);
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + resourcePath, e);
        }

        int dot = resourcePath.lastIndexOf('.');
        String hint = dot >= 0 ? resourcePath.substring(dot + 1) : "";

        ByteBuffer data = MemoryUtil.memAlloc(bytes.length);
        try {
            data.put(bytes).flip();
            AIScene scene = aiImportFileFromMemory(data, FLAGS, hint);
            if (scene == null || scene.mNumMeshes() == 0) {
                String err = aiGetErrorString();
                if (scene != null) aiReleaseImport(scene);
                throw new RuntimeException("Assimp failed for " + resourcePath + ": " + err);
            }
            try {
                return extract(scene);
            } finally {
                aiReleaseImport(scene);
            }
        } finally {
            MemoryUtil.memFree(data);
        }
    }

    private static Mesh extract(AIScene scene) {
        int meshCount = scene.mNumMeshes();
        PointerBuffer meshPointers = scene.mMeshes();

        int totalVerts = 0;
        int totalTris = 0;
        AIMesh[] meshes = new AIMesh[meshCount];
        for (int i = 0; i < meshCount; i++) {
            meshes[i] = AIMesh.create(meshPointers.get(i));
            totalVerts += meshes[i].mNumVertices();
            totalTris += meshes[i].mNumFaces();
        }

        FloatBuffer positions = MemoryUtil.memAllocFloat(totalVerts * 3);
        IntBuffer indices = MemoryUtil.memAllocInt(totalTris * 3);
        try {
            int vertexOffset = 0;
            for (AIMesh mesh : meshes) {
                int nv = mesh.mNumVertices();
                AIVector3D.Buffer verts = mesh.mVertices();
                for (int v = 0; v < nv; v++) {
                    AIVector3D p = verts.get(v);
                    positions.put(p.x()).put(p.y()).put(p.z());
                }
                int nf = mesh.mNumFaces();
                AIFace.Buffer faces = mesh.mFaces();
                for (int f = 0; f < nf; f++) {
                    IntBuffer idx = faces.get(f).mIndices();
                    indices.put(idx.get(0) + vertexOffset);
                    indices.put(idx.get(1) + vertexOffset);
                    indices.put(idx.get(2) + vertexOffset);
                }
                vertexOffset += nv;
            }
            positions.flip();
            indices.flip();
            return new Mesh(positions, indices);
        } finally {
            MemoryUtil.memFree(positions);
            MemoryUtil.memFree(indices);
        }
    }
}
