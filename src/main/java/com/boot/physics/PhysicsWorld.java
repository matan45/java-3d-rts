package com.boot.physics;

import org.joml.Vector3f;
import physx.PxTopLevelFunctions;
import physx.common.PxDefaultAllocator;
import physx.common.PxDefaultCpuDispatcher;
import physx.common.PxDefaultErrorCallback;
import physx.common.PxFoundation;
import physx.common.PxTolerancesScale;
import physx.common.PxVec3;
import physx.physics.PxHitFlagEnum;
import physx.physics.PxHitFlags;
import physx.physics.PxPhysics;
import physx.physics.PxQueryFilterData;
import physx.physics.PxQueryFlagEnum;
import physx.physics.PxQueryFlags;
import physx.physics.PxRaycastBuffer10;
import physx.physics.PxRaycastHit;
import physx.physics.PxScene;
import physx.physics.PxSceneDesc;

public final class PhysicsWorld {

    private final PxDefaultAllocator allocator;
    private final PxDefaultErrorCallback errorCb;
    private final PxFoundation foundation;
    private final PxTolerancesScale tolerances;
    private final PxPhysics physics;
    private final PxDefaultCpuDispatcher dispatcher;
    private final PxScene scene;

    private final int version;

    private final PxVec3 tmpOrigin = new PxVec3(0, 0, 0);
    private final PxVec3 tmpDir = new PxVec3(0, -1, 0);
    private final PxHitFlags rayFlags;
    private final PxQueryFlags rayQueryFlags;
    private final PxQueryFilterData rayQueryFilter;
    private final PxRaycastBuffer10 rayHit = new PxRaycastBuffer10();

    public PhysicsWorld() {
        version = PxTopLevelFunctions.getPHYSICS_VERSION();
        allocator = new PxDefaultAllocator();
        errorCb = new PxDefaultErrorCallback();
        foundation = PxTopLevelFunctions.CreateFoundation(version, allocator, errorCb);
        tolerances = new PxTolerancesScale();
        physics = PxTopLevelFunctions.CreatePhysics(version, foundation, tolerances);

        dispatcher = PxTopLevelFunctions.DefaultCpuDispatcherCreate(2);
        PxSceneDesc sceneDesc = new PxSceneDesc(tolerances);
        sceneDesc.setGravity(new PxVec3(0, -9.81f, 0));
        sceneDesc.setCpuDispatcher(dispatcher);
        sceneDesc.setFilterShader(PxTopLevelFunctions.DefaultFilterShader());
        scene = physics.createScene(sceneDesc);
        sceneDesc.destroy();

        rayFlags = new PxHitFlags((short) (
                PxHitFlagEnum.ePOSITION.value | PxHitFlagEnum.eNORMAL.value | PxHitFlagEnum.eDEFAULT.value));
        rayQueryFlags = new PxQueryFlags((short) (
                PxQueryFlagEnum.eSTATIC.value | PxQueryFlagEnum.eDYNAMIC.value));
        rayQueryFilter = new PxQueryFilterData(rayQueryFlags);
    }


    public PxPhysics physics() { return physics; }
    public PxScene scene() { return scene; }
    public int version() { return version; }

    public void step(float dt) {
        if (dt <= 0) return;
        scene.simulate(dt);
        scene.fetchResults(true);
    }

    public Vector3f raycast(float ox, float oy, float oz,
                            float dx, float dy, float dz,
                            float maxDist, Vector3f out) {
        tmpOrigin.setX(ox); tmpOrigin.setY(oy); tmpOrigin.setZ(oz);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0f) return null;
        float inv = 1f / len;
        tmpDir.setX(dx * inv); tmpDir.setY(dy * inv); tmpDir.setZ(dz * inv);

        boolean hit = scene.raycast(tmpOrigin, tmpDir, maxDist, rayHit, rayFlags, rayQueryFilter);
        if (!hit) return null;
        if (!rayHit.getHasBlock()) {
            int touches = rayHit.getNbTouches();
            if (touches > 0) {
                PxRaycastHit t = rayHit.getTouch(0);
                PxVec3 p = t.getPosition();
                out.set(p.getX(), p.getY(), p.getZ());
                return out;
            }
            return null;
        }
        PxRaycastHit block = rayHit.getBlock();
        PxVec3 p = block.getPosition();
        out.set(p.getX(), p.getY(), p.getZ());
        return out;
    }

    public void dispose() {
        rayHit.destroy();
        rayQueryFilter.destroy();
        rayQueryFlags.destroy();
        rayFlags.destroy();
        tmpDir.destroy();
        tmpOrigin.destroy();
        scene.release();
        dispatcher.destroy();
        physics.release();
        tolerances.destroy();
        foundation.release();
        errorCb.destroy();
        allocator.destroy();
    }
}
