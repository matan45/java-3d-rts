package com.boot.physics;

import com.boot.world.Heightmap;
import physx.PxTopLevelFunctions;
import physx.common.PxIDENTITYEnum;
import physx.common.PxStridedData;
import physx.common.PxTransform;
import physx.geometry.PxHeightField;
import physx.geometry.PxHeightFieldDesc;
import physx.geometry.PxHeightFieldFormatEnum;
import physx.geometry.PxHeightFieldGeometry;
import physx.geometry.PxHeightFieldSample;
import physx.geometry.PxMeshGeometryFlags;
import physx.physics.PxFilterData;
import physx.physics.PxMaterial;
import physx.physics.PxRigidStatic;
import physx.physics.PxShape;
import physx.physics.PxShapeFlagEnum;
import physx.physics.PxShapeFlags;
import physx.support.PxArray_PxHeightFieldSample;

public final class TerrainCollider {

    private final PhysicsWorld world;
    private final PxHeightField heightField;
    private final PxMaterial material;
    private final PxShape shape;
    private final PxRigidStatic actor;
    private final float heightScale;

    public TerrainCollider(PhysicsWorld world, Heightmap hm) {
        this.world = world;

        int n = hm.size();
        float maxAbs = Math.max(Math.abs(hm.maxHeight()), Math.abs(hm.minHeight()));
        this.heightScale = Math.max(0.0005f, maxAbs / 30000f);

        PxArray_PxHeightFieldSample samples = new PxArray_PxHeightFieldSample(n * n);
        PxHeightFieldSample tmp = new PxHeightFieldSample();
        try {
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    float worldH = hm.heightAtGrid(r, c);
                    short h = (short) Math.max(Short.MIN_VALUE,
                            Math.min(Short.MAX_VALUE, Math.round(worldH / heightScale)));
                    tmp.setHeight(h);
                    tmp.setMaterialIndex0((byte) 0);
                    tmp.setMaterialIndex1((byte) 0);
                    samples.set(r * n + c, tmp);
                }
            }

            PxHeightFieldDesc desc = new PxHeightFieldDesc();
            desc.setNbRows(n);
            desc.setNbColumns(n);
            desc.setFormat(PxHeightFieldFormatEnum.eS16_TM);
            PxStridedData sd = desc.getSamples();
            sd.setStride(PxHeightFieldSample.SIZEOF);
            sd.setData(samples.begin());
            desc.setSamples(sd);

            heightField = PxTopLevelFunctions.CreateHeightField(desc);
            desc.destroy();
        } finally {
            tmp.destroy();
            samples.destroy();
        }

        material = world.physics().createMaterial(0.6f, 0.6f, 0.1f);

        PxMeshGeometryFlags geomFlags = new PxMeshGeometryFlags((byte) 0);
        PxHeightFieldGeometry geom = new PxHeightFieldGeometry(
                heightField, geomFlags, heightScale, hm.quadSize(), hm.quadSize());

        PxShapeFlags shapeFlags = new PxShapeFlags((byte) (
                PxShapeFlagEnum.eSCENE_QUERY_SHAPE.value | PxShapeFlagEnum.eSIMULATION_SHAPE.value));
        shape = world.physics().createShape(geom, material, true, shapeFlags);

        PxFilterData filter = new PxFilterData(1, 1, 0, 0);
        shape.setSimulationFilterData(filter);
        filter.destroy();

        PxTransform pose = new PxTransform(PxIDENTITYEnum.PxIdentity);
        actor = world.physics().createRigidStatic(pose);
        pose.destroy();
        actor.attachShape(shape);
        world.scene().addActor(actor);

        geom.destroy();
        geomFlags.destroy();
        shapeFlags.destroy();
    }

    public void dispose() {
        world.scene().removeActor(actor);
        actor.release();
        shape.release();
        material.release();
        heightField.release();
    }
}
