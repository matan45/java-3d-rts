package com.boot.physics;

import org.joml.Vector3f;
import physx.PxTopLevelFunctions;
import physx.common.PxDefaultAllocator;
import physx.common.PxDefaultCpuDispatcher;
import physx.common.PxDefaultErrorCallback;
import physx.common.PxFoundation;
import physx.common.PxIDENTITYEnum;
import physx.common.PxTolerancesScale;
import physx.common.PxTransform;
import physx.common.PxVec3;
import physx.geometry.PxBoxGeometry;
import physx.geometry.PxSphereGeometry;
import physx.physics.PxFilterData;
import physx.physics.PxHitFlagEnum;
import physx.physics.PxHitFlags;
import physx.physics.PxMaterial;
import physx.physics.PxPhysics;
import physx.physics.PxQueryFilterData;
import physx.physics.PxQueryFlagEnum;
import physx.physics.PxQueryFlags;
import physx.physics.PxRaycastBuffer10;
import physx.physics.PxRaycastHit;
import physx.physics.PxRigidActor;
import physx.physics.PxRigidBodyFlagEnum;
import physx.physics.PxRigidDynamic;
import physx.physics.PxRigidStatic;
import physx.physics.PxScene;
import physx.physics.PxSceneDesc;
import physx.physics.PxShape;
import physx.physics.PxShapeFlagEnum;
import physx.physics.PxShapeFlags;

public final class PhysicsWorld {

    public static final int FILTER_TERRAIN = 1;
    public static final int FILTER_BUILDING = 2;

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

    private PxMaterial defaultMaterial;
    private PxShapeFlags shapeFlags;
    private PxTransform tmpKinematicPose;

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

    private PxMaterial defaultMaterial() {
        if (defaultMaterial == null) {
            defaultMaterial = physics.createMaterial(0.6f, 0.6f, 0.1f);
        }
        return defaultMaterial;
    }

    private PxShapeFlags shapeFlags() {
        if (shapeFlags == null) {
            shapeFlags = new PxShapeFlags((byte) (
                    PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value
                            | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        }
        return shapeFlags;
    }

    public PxRigidDynamic createKinematicSphere(float x, float y, float z, float radius) {
        PxTransform pose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxVec3 p = pose.getP();
        p.setX(x); p.setY(y); p.setZ(z);
        PxRigidDynamic actor = physics.createRigidDynamic(pose);
        pose.destroy();

        actor.setRigidBodyFlag(PxRigidBodyFlagEnum.eKINEMATIC, true);

        PxSphereGeometry geom = new PxSphereGeometry(radius);
        PxShape shape = physics.createShape(geom, defaultMaterial(), true, shapeFlags());
        PxFilterData filter = new PxFilterData(1, 1, 0, 0);
        shape.setSimulationFilterData(filter);
        filter.destroy();
        actor.attachShape(shape);
        shape.release();
        geom.destroy();

        scene.addActor(actor);
        return actor;
    }

    public PxRigidStatic createStaticBox(float cx, float cy, float cz,
                                         float hx, float hy, float hz) {
        return createStaticBox(cx, cy, cz, hx, hy, hz, 0);
    }

    public PxRigidStatic createStaticBox(float cx, float cy, float cz,
                                         float hx, float hy, float hz,
                                         int queryGroup) {
        PxTransform pose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        PxVec3 p = pose.getP();
        p.setX(cx); p.setY(cy); p.setZ(cz);
        PxRigidStatic actor = physics.createRigidStatic(pose);
        pose.destroy();

        PxBoxGeometry geom = new PxBoxGeometry(hx, hy, hz);
        PxShape shape = physics.createShape(geom, defaultMaterial(), true, shapeFlags());
        PxFilterData simFilter = new PxFilterData(1, 1, 0, 0);
        shape.setSimulationFilterData(simFilter);
        simFilter.destroy();
        if (queryGroup != 0) {
            PxFilterData queryFilter = new PxFilterData(queryGroup, 0, 0, 0);
            shape.setQueryFilterData(queryFilter);
            queryFilter.destroy();
        }
        actor.attachShape(shape);
        shape.release();
        geom.destroy();

        scene.addActor(actor);
        return actor;
    }

    public void setKinematicPose(PxRigidDynamic actor, float x, float y, float z) {
        if (tmpKinematicPose == null) {
            tmpKinematicPose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        }
        PxVec3 p = tmpKinematicPose.getP();
        p.setX(x); p.setY(y); p.setZ(z);
        actor.setKinematicTarget(tmpKinematicPose);
    }

    public void releaseActor(PxRigidActor actor) {
        scene.removeActor(actor);
        actor.release();
    }

    public Vector3f raycast(float ox, float oy, float oz,
                            float dx, float dy, float dz,
                            float maxDist, Vector3f out) {
        return raycastClosest(ox, oy, oz, dx, dy, dz, maxDist, out, 0);
    }

    public Vector3f raycastTerrain(float ox, float oy, float oz,
                                   float dx, float dy, float dz,
                                   float maxDist, Vector3f out) {
        return raycastClosest(ox, oy, oz, dx, dy, dz, maxDist, out, FILTER_TERRAIN);
    }

    private Vector3f raycastClosest(float ox, float oy, float oz,
                                    float dx, float dy, float dz,
                                    float maxDist, Vector3f out, int requiredWord0) {
        tmpOrigin.setX(ox); tmpOrigin.setY(oy); tmpOrigin.setZ(oz);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0f) return null;
        float inv = 1f / len;
        tmpDir.setX(dx * inv); tmpDir.setY(dy * inv); tmpDir.setZ(dz * inv);

        boolean hit = scene.raycast(tmpOrigin, tmpDir, maxDist, rayHit, rayFlags, rayQueryFilter);
        if (!hit) return null;

        PxRaycastHit best = null;
        float bestDist = Float.MAX_VALUE;

        if (rayHit.getHasBlock()) {
            PxRaycastHit b = rayHit.getBlock();
            if (passes(b, requiredWord0)) {
                best = b;
                bestDist = b.getDistance();
            }
        }
        int touches = rayHit.getNbTouches();
        for (int i = 0; i < touches; i++) {
            PxRaycastHit t = rayHit.getTouch(i);
            if (!passes(t, requiredWord0)) continue;
            float d = t.getDistance();
            if (d < bestDist) {
                bestDist = d;
                best = t;
            }
        }

        if (best == null) return null;
        PxVec3 p = best.getPosition();
        out.set(p.getX(), p.getY(), p.getZ());
        return out;
    }

    private static boolean passes(PxRaycastHit h, int requiredWord0) {
        if (requiredWord0 == 0) return true;
        PxShape s = h.getShape();
        if (s == null) return false;
        PxFilterData fd = s.getQueryFilterData();
        return fd != null && fd.getWord0() == requiredWord0;
    }

    public void dispose() {
        if (tmpKinematicPose != null) tmpKinematicPose.destroy();
        if (shapeFlags != null) shapeFlags.destroy();
        if (defaultMaterial != null) defaultMaterial.release();
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
